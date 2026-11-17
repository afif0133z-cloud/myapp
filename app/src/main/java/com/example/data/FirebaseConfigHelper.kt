package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseConfigHelper {

    fun isFirebaseReady(context: Context): Boolean {
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isNotEmpty()) return true
        return runCatching { FirebaseApp.initializeApp(appContext) != null }.getOrDefault(false)
    }

    @SuppressLint("DiscouragedApi")
    fun resolveDatabaseId(context: Context): String {
        val appContext = context.applicationContext
        val resId = appContext.resources.getIdentifier(
            "firestore_database_id",
            "string",
            appContext.packageName
        )
        return if (resId != 0) appContext.getString(resId) else "(default)"
    }

    fun getFirestoreOrNull(context: Context): FirebaseFirestore? {
        if (!isFirebaseReady(context)) return null
        return runCatching {
            FirebaseFirestore.getInstance(resolveDatabaseId(context))
        }.getOrNull()
    }
}
