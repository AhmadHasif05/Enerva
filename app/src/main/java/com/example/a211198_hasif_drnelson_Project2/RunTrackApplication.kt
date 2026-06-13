package com.example.a211198_hasif_drnelson_Project2

import android.app.Application
import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import com.example.a211198_hasif_drnelson_Project2.data.repository.AuthRepository
import com.example.a211198_hasif_drnelson_Project2.data.repository.GalleryRepository
import com.example.a211198_hasif_drnelson_Project2.data.repository.MessageRepository
import com.example.a211198_hasif_drnelson_Project2.data.repository.UserRepository
import com.google.firebase.FirebaseApp

// Application subclass — eagerly initializes Firebase + the Room database so
// ViewModelFactory and any other early consumers can fetch them.
class RunTrackApplication : Application() {
    val authRepository: AuthRepository by lazy { AuthRepository() }
    val userRepository: UserRepository by lazy { UserRepository(AppDatabase.get(this), cacheDir) }
    val messageRepository: MessageRepository by lazy { MessageRepository(AppDatabase.get(this)) }
    val galleryRepository: GalleryRepository by lazy { GalleryRepository(AppDatabase.get(this), cacheDir) }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AppDatabase.get(this)
    }
}
