package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.LogEntity
import com.example.data.local.entity.ScriptEntity
import com.example.data.local.entity.TaskEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: FirebaseUser?) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class FirebaseManager {

    companion object {
        private const val TAG = "FirebaseManager"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_TASKS = "tasks"
        private const val COLLECTION_SCRIPTS = "scripts"
        private const val COLLECTION_LOGS = "logs"
    }

    private val isFirebaseInitialized: Boolean
        get() = try {
            com.google.firebase.FirebaseApp.getInstance() != null
        } catch (e: Exception) {
            false
        }

    private val auth: FirebaseAuth? by lazy {
        if (isFirebaseInitialized) FirebaseAuth.getInstance() else null
    }
    private val firestore: FirebaseFirestore? by lazy {
        if (isFirebaseInitialized) FirebaseFirestore.getInstance() else null
    }

    val currentUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (e: Exception) {
            null
        }

    val currentUserId: String
        get() = currentUser?.uid ?: "local_guest_user"

    val isUserLoggedIn: Boolean
        get() = currentUser != null

    val isGuestUser: Boolean
        get() = currentUser?.isAnonymous ?: true

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        try {
            auth?.addAuthStateListener(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add auth state listener", e)
            trySend(null)
        }
        awaitClose {
            try {
                auth?.removeAuthStateListener(listener)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        if (auth == null) return AuthResult.Error("Firebase is not configured. Please add google-services.json to use cloud features.")
        return try {
            val result = auth!!.signInWithEmailAndPassword(email.trim(), pass).await()
            AuthResult.Success(result.user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error", e)
            AuthResult.Error(e.localizedMessage ?: "Sign in failed")
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): AuthResult {
        if (auth == null) return AuthResult.Error("Firebase is not configured. Please add google-services.json to use cloud features.")
        return try {
            val result = auth!!.createUserWithEmailAndPassword(email.trim(), pass).await()
            AuthResult.Success(result.user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error", e)
            AuthResult.Error(e.localizedMessage ?: "Sign up failed")
        }
    }

    suspend fun signInAnonymously(): AuthResult {
        if (auth == null) return AuthResult.Success(null) // Gracefully fallback to local guest
        return try {
            val result = auth!!.signInAnonymously().await()
            AuthResult.Success(result.user)
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign in error", e)
            AuthResult.Error(e.localizedMessage ?: "Guest login failed")
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error", e)
        }
    }

    suspend fun syncTaskToFirestore(task: TaskEntity): Boolean {
        if (firestore == null) return false
        return try {
            val uid = currentUserId
            val taskMap = hashMapOf(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "category" to task.category,
                "priority" to task.priority,
                "dueDateMillis" to task.dueDateMillis,
                "isCompleted" to task.isCompleted,
                "linkedScriptId" to task.linkedScriptId,
                "linkedScriptName" to task.linkedScriptName,
                "userId" to uid,
                "updatedAt" to task.updatedAt
            )
            firestore!!.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_TASKS)
                .document(task.id)
                .set(taskMap, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync task ${task.id} to Firestore", e)
            false
        }
    }

    suspend fun deleteTaskFromFirestore(taskId: String): Boolean {
        if (firestore == null) return false
        return try {
            val uid = currentUserId
            firestore!!.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_TASKS)
                .document(taskId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete task from Firestore", e)
            false
        }
    }

    suspend fun syncScriptToFirestore(script: ScriptEntity): Boolean {
        if (firestore == null) return false
        return try {
            val uid = currentUserId
            val scriptMap = hashMapOf(
                "id" to script.id,
                "name" to script.name,
                "description" to script.description,
                "type" to script.type,
                "content" to script.content,
                "webhookMethod" to script.webhookMethod,
                "webhookHeaders" to script.webhookHeaders,
                "webhookPayload" to script.webhookPayload,
                "scheduleType" to script.scheduleType,
                "scheduledTimeMillis" to script.scheduledTimeMillis,
                "dailyHour" to script.dailyHour,
                "dailyMinute" to script.dailyMinute,
                "isActive" to script.isActive,
                "isQuickAction" to script.isQuickAction,
                "lastRunStatus" to script.lastRunStatus,
                "lastRunTime" to script.lastRunTime,
                "userId" to uid,
                "updatedAt" to script.updatedAt
            )
            firestore!!.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_SCRIPTS)
                .document(script.id)
                .set(scriptMap, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync script ${script.id} to Firestore", e)
            false
        }
    }

    suspend fun deleteScriptFromFirestore(scriptId: String): Boolean {
        if (firestore == null) return false
        return try {
            val uid = currentUserId
            firestore!!.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_SCRIPTS)
                .document(scriptId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete script from Firestore", e)
            false
        }
    }

    suspend fun uploadLogToFirestore(log: LogEntity): Boolean {
        if (firestore == null) return false
        return try {
            val uid = currentUserId
            val logMap = hashMapOf(
                "id" to log.id,
                "scriptId" to log.scriptId,
                "scriptName" to log.scriptName,
                "triggerType" to log.triggerType,
                "output" to log.output,
                "status" to log.status,
                "executionDurationMs" to log.executionDurationMs,
                "timestamp" to log.timestamp,
                "userId" to uid
            )
            firestore!!.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_LOGS)
                .document(log.id)
                .set(logMap)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload log to Firestore", e)
            false
        }
    }

    suspend fun updateFcmToken(token: String) {
        if (firestore == null) return
        try {
            val uid = currentUserId
            firestore!!.collection(COLLECTION_USERS)
                .document(uid)
                .set(mapOf("fcmToken" to token, "lastTokenUpdate" to System.currentTimeMillis()), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store FCM token", e)
        }
    }
}
