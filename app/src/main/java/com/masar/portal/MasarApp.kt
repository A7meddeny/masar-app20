package com.masar.portal

import android.app.Application
import com.masar.portal.data.SessionStore

class MasarApp : Application() {
    lateinit var session: SessionStore
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionStore(this)
    }
}
