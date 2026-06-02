package com.example.a211198_hasif_drnelson_Project2.view_model

import android.app.Application
import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.a211198_hasif_drnelson_Project2.RunTrackApplication
import com.example.a211198_hasif_drnelson_Project2.data.entities.MessageEntity
import com.example.a211198_hasif_drnelson_Project2.data.repository.MessageRepository
import com.example.a211198_hasif_drnelson_Project2.model.Conversation
import com.example.a211198_hasif_drnelson_Project2.model.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// State holder for chats. All persistence + Firestore sync lives in
// MessageRepository; this ViewModel mirrors repository flows into Compose state
// and forwards user actions. Public surface unchanged → screens untouched.
class MessageViewModel(
    application: Application,
    private val repository: MessageRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("runtrack", Context.MODE_PRIVATE)

    // Mirror of the persisted conversations, keyed by friendName.
    private val conversations = mutableStateMapOf<String, Conversation>()

    // Active user email — set on login and on app restart.
    private var activeEmail: String = prefs.getString("activeEmail", null).orEmpty()
    private var convoJob: Job? = null
    private var messagesJob: Job? = null

    val conversationList: List<Conversation> by derivedStateOf {
        conversations.values.sortedByDescending { it.lastMessage?.timestampMs ?: 0L }
    }

    val groupConversations: List<Conversation> by derivedStateOf {
        conversationList.filter { it.isGroup }
    }

    init {
        if (activeEmail.isNotBlank()) startObserving(activeEmail)
    }

    fun setActiveUser(email: String) {
        if (email == activeEmail) return
        activeEmail = email
        conversations.clear()
        startObserving(email)
    }

    fun clearActiveUser() {
        activeEmail = ""
        convoJob?.cancel()
        messagesJob?.cancel()
        repository.stopSync()
        conversations.clear()
    }

    fun getConversation(friendName: String): Conversation =
        conversations[friendName] ?: Conversation(friendName)

    fun startConversationWith(friendName: String) {
        val owner = activeEmail.ifBlank { return }
        if (conversations.containsKey(friendName)) return
        viewModelScope.launch { repository.startConversationWith(owner, friendName) }
    }

    fun sendMessage(friendName: String, text: String) {
        val owner = activeEmail.ifBlank { return }
        if (text.isBlank()) return
        viewModelScope.launch { repository.sendMessage(owner, friendName, text) }
    }

    fun removeConversation(friendName: String) {
        val owner = activeEmail.ifBlank { return }
        viewModelScope.launch { repository.removeConversation(owner, friendName) }
    }

    fun createGroup(groupName: String, members: List<String>) {
        val owner = activeEmail.ifBlank { return }
        if (groupName.isBlank() || members.isEmpty() || conversations.containsKey(groupName.trim())) return
        viewModelScope.launch { repository.createGroup(owner, groupName, members) }
    }

    // ---- private ----

    private fun startObserving(email: String) {
        convoJob?.cancel()
        messagesJob?.cancel()
        repository.startSync(viewModelScope, email)
        convoJob = viewModelScope.launch {
            repository.observeConversations(email).collect { convos ->
                // Preserve any messages already loaded; just sync metadata.
                val existing = conversations.toMap()
                conversations.clear()
                convos.forEach { c ->
                    val prevMessages = existing[c.friendName]?.messages.orEmpty()
                    conversations[c.friendName] = Conversation(
                        friendName = c.friendName,
                        messages = prevMessages,
                        isGroup = c.isGroup,
                        members = if (c.membersCsv.isBlank()) emptyList() else c.membersCsv.split(",")
                    )
                }
            }
        }
        messagesJob = viewModelScope.launch {
            repository.observeAllMessages(email).collect { messages ->
                val grouped = messages.groupBy { it.friendName }
                grouped.forEach { (friendName, msgs) ->
                    val existing = conversations[friendName] ?: Conversation(friendName)
                    conversations[friendName] = existing.copy(
                        messages = msgs.map { it.toModel() }
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RunTrackApplication
                MessageViewModel(app, app.messageRepository)
            }
        }
    }
}

private fun MessageEntity.toModel() = Message(
    id = id,
    fromMe = fromMe,
    text = text,
    timestampMs = timestampMs
)
