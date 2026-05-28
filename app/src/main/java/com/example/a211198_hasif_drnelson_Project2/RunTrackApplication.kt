package com.example.a211198_hasif_drnelson_Project2

import android.app.Application
import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase

// Application subclass — eagerly initializes the Room database so
// ViewModelFactory and any other early consumers can fetch it via
// AppDatabase.get(context).
class RunTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.get(this)
    }
}
