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
import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import com.example.a211198_hasif_drnelson_Project2.data.entities.ConversationEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MessageEntity
import com.example.a211198_hasif_drnelson_Project2.model.Conversation
import com.example.a211198_hasif_drnelson_Project2.model.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    private val dao = db.messageDao()
    private val userDao = db.userDao()
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
        conversations.clear()
    }

    fun getConversation(friendName: String): Conversation =
        conversations[friendName] ?: Conversation(friendName)

    fun startConversationWith(friendName: String) {
        val owner = activeEmail.ifBlank { return }
        if (conversations.containsKey(friendName)) return
        viewModelScope.launch {
            val me = userDao.findByEmail(owner) ?: return@launch
            val opening = Message(
                fromMe = false,
                text = "Hey! Thanks for the follow 👋 Let's go for a run sometime."
            )
            dao.upsertConversation(
                ConversationEntity(owner, friendName, isGroup = false, membersCsv = "")
            )
            dao.insertMessage(opening.toEntity(owner, friendName))

            // Mirror under the recipient so they see this chat too.
            val recipient = userDao.findByName(friendName)
            if (recipient != null && recipient.email != owner) {
                if (dao.findConversation(recipient.email, me.runnerName) == null) {
                    dao.upsertConversation(
                        ConversationEntity(recipient.email, me.runnerName, isGroup = false, membersCsv = "")
                    )
                }
                dao.insertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        ownerEmail = recipient.email,
                        friendName = me.runnerName,
                        fromMe = false,
                        text = "${me.runnerName} started following you 👋",
                        timestampMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun sendMessage(friendName: String, text: String) {
        val owner = activeEmail.ifBlank { return }
        if (text.isBlank()) return
        val msg = Message(fromMe = true, text = text.trim())
        viewModelScope.launch {
            val existing = dao.findConversation(owner, friendName)
            val isGroup = existing?.isGroup == true
            val membersCsv = existing?.membersCsv.orEmpty()
            if (existing == null) {
                dao.upsertConversation(
                    ConversationEntity(owner, friendName, isGroup = false, membersCsv = "")
                )
            }
            dao.insertMessage(msg.toEntity(owner, friendName))

            val me = userDao.findByEmail(owner) ?: return@launch
            if (isGroup) {
                // Group chat — replicate the message under every registered member.
                membersCsv.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { memberName ->
                        val memberUser = userDao.findByName(memberName) ?: return@forEach
                        if (memberUser.email == owner) return@forEach
                        if (dao.findConversation(memberUser.email, friendName) == null) {
                            dao.upsertConversation(
                                ConversationEntity(memberUser.email, friendName, isGroup = true, membersCsv = membersCsv)
                            )
                        }
                        dao.insertMessage(
                            MessageEntity(
                                id = UUID.randomUUID().toString(),
                                ownerEmail = memberUser.email,
                                friendName = friendName,
                                fromMe = false,
                                text = "${me.runnerName}: ${msg.text}",
                                timestampMs = msg.timestampMs
                            )
                        )
                    }
            } else {
                // 1:1 — friendName is the OTHER party's display name. Mirror to them.
                val recipient = userDao.findByName(friendName)
                if (recipient != null && recipient.email != owner) {
                    if (dao.findConversation(recipient.email, me.runnerName) == null) {
                        dao.upsertConversation(
                            ConversationEntity(recipient.email, me.runnerName, isGroup = false, membersCsv = "")
                        )
                    }
                    dao.insertMessage(
                        MessageEntity(
                            id = UUID.randomUUID().toString(),
                            ownerEmail = recipient.email,
                            friendName = me.runnerName,
                            fromMe = false,
                            text = msg.text,
                            timestampMs = msg.timestampMs
                        )
                    )
                }
            }
        }
    }

    fun removeConversation(friendName: String) {
        val owner = activeEmail.ifBlank { return }
        viewModelScope.launch {
            dao.deleteMessagesFor(owner, friendName)
            dao.deleteConversation(owner, friendName)
        }
    }

    fun createGroup(groupName: String, members: List<String>) {
        val owner = activeEmail.ifBlank { return }
        val name = groupName.trim()
        if (name.isBlank() || members.isEmpty() || conversations.containsKey(name)) return
        val opening = Message(
            fromMe = false,
            text = "Group \"$name\" created with ${members.joinToString(", ")} 🎉"
        )
        val membersCsv = members.joinToString(",")
        viewModelScope.launch {
            // Creator's row.
            dao.upsertConversation(
                ConversationEntity(owner, name, isGroup = true, membersCsv = membersCsv)
            )
            dao.insertMessage(opening.toEntity(owner, name))

            // Replicate the group + opening to every registered member so
            // they actually see the group when they log in.
            members.forEach { memberName ->
                val memberUser = userDao.findByName(memberName) ?: return@forEach
                if (memberUser.email == owner) return@forEach
                dao.upsertConversation(
                    ConversationEntity(memberUser.email, name, isGroup = true, membersCsv = membersCsv)
                )
                dao.insertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        ownerEmail = memberUser.email,
                        friendName = name,
                        fromMe = false,
                        text = opening.text,
                        timestampMs = opening.timestampMs
                    )
                )
            }
        }
    }

    // ---- private ----

    private fun startObserving(email: String) {
        convoJob?.cancel()
        messagesJob?.cancel()
        convoJob = viewModelScope.launch {
            dao.observeConversations(email).collect { convos ->
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
            dao.observeAllMessages(email).collect { messages ->
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
                MessageViewModel(app)
            }
        }
    }
}

private fun Message.toEntity(ownerEmail: String, friendName: String) = MessageEntity(
    id = id,
    ownerEmail = ownerEmail,
    friendName = friendName,
    fromMe = fromMe,
    text = text,
    timestampMs = timestampMs
)

private fun MessageEntity.toModel() = Message(
    id = id,
    fromMe = fromMe,
    text = text,
    timestampMs = timestampMs
)
