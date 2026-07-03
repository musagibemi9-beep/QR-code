package com.example.data.repository

import android.util.Log
import com.example.data.local.QrDao
import com.example.data.model.DotStyle
import com.example.data.model.ErrorCorrectionLevel
import com.example.data.model.EyeShape
import com.example.data.model.QrCodeItem
import com.example.data.model.QrFormType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class QrRepository(private val qrDao: QrDao) {
    val allItems: Flow<List<QrCodeItem>> = qrDao.getAllItems()
    
    private val firestore by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }
    private val auth by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }

    suspend fun saveItem(item: QrCodeItem): Long = withContext(Dispatchers.IO) {
        val currentUser = auth?.currentUser
        val itemToSave = if (currentUser != null) {
            item.copy(userId = currentUser.uid)
        } else {
            item
        }
        val localId = qrDao.insertItem(itemToSave)
        
        // Try sync with Firestore if logged in
        if (currentUser != null && firestore != null) {
            try {
                val docRef = if (itemToSave.firestoreId.isNotBlank()) {
                    firestore!!.collection("users").document(currentUser.uid)
                        .collection("qr_codes").document(itemToSave.firestoreId)
                } else {
                    val newId = UUID.randomUUID().toString()
                    firestore!!.collection("users").document(currentUser.uid)
                        .collection("qr_codes").document(newId)
                }
                
                val mapData = mapOf(
                    "title" to itemToSave.title,
                    "category" to itemToSave.category,
                    "formType" to itemToSave.formType,
                    "content" to itemToSave.content,
                    "foregroundColorHex" to itemToSave.foregroundColorHex,
                    "backgroundColorHex" to itemToSave.backgroundColorHex,
                    "eyeShape" to itemToSave.eyeShape,
                    "dotStyle" to itemToSave.dotStyle,
                    "errorCorrection" to itemToSave.errorCorrection,
                    "logoBase64" to itemToSave.logoBase64,
                    "createdAt" to itemToSave.createdAt
                )
                docRef.set(mapData).await()
                qrDao.markSynced(localId, docRef.id)
            } catch (e: Exception) {
                Log.e("QrRepository", "Firestore sync failed: ${e.message}")
            }
        }
        localId
    }

    suspend fun deleteItem(item: QrCodeItem) = withContext(Dispatchers.IO) {
        qrDao.deleteItemById(item.id)
        val currentUser = auth?.currentUser
        if (currentUser != null && firestore != null && item.firestoreId.isNotBlank()) {
            try {
                firestore!!.collection("users").document(currentUser.uid)
                    .collection("qr_codes").document(item.firestoreId)
                    .delete().await()
            } catch (e: Exception) {
                Log.e("QrRepository", "Firestore delete failed: ${e.message}")
            }
        }
    }

    suspend fun syncWithCloud() = withContext(Dispatchers.IO) {
        val currentUser = auth?.currentUser ?: return@withContext
        val db = firestore ?: return@withContext
        try {
            // 1. Push unsynced local items
            val unsynced = qrDao.getUnsyncedItems()
            for (item in unsynced) {
                val newId = UUID.randomUUID().toString()
                val docRef = db.collection("users").document(currentUser.uid)
                    .collection("qr_codes").document(newId)
                val mapData = mapOf(
                    "title" to item.title,
                    "category" to item.category,
                    "formType" to item.formType,
                    "content" to item.content,
                    "foregroundColorHex" to item.foregroundColorHex,
                    "backgroundColorHex" to item.backgroundColorHex,
                    "eyeShape" to item.eyeShape,
                    "dotStyle" to item.dotStyle,
                    "errorCorrection" to item.errorCorrection,
                    "logoBase64" to item.logoBase64,
                    "createdAt" to item.createdAt
                )
                docRef.set(mapData).await()
                qrDao.markSynced(item.id, newId)
            }
            
            // 2. Fetch cloud items and merge
            val snapshot = db.collection("users").document(currentUser.uid)
                .collection("qr_codes").get().await()
            for (doc in snapshot.documents) {
                val cloudId = doc.id
                val title = doc.getString("title") ?: "Cloud QR"
                val category = doc.getString("category") ?: "Cloud"
                val formType = doc.getString("formType") ?: QrFormType.URL.name
                val content = doc.getString("content") ?: ""
                val fg = doc.getString("foregroundColorHex") ?: "#0F172A"
                val bg = doc.getString("backgroundColorHex") ?: "#FFFFFF"
                val eye = doc.getString("eyeShape") ?: EyeShape.SQUIRCLE.name
                val dot = doc.getString("dotStyle") ?: DotStyle.ROUNDED.name
                val ec = doc.getString("errorCorrection") ?: ErrorCorrectionLevel.M.name
                val logo = doc.getString("logoBase64")
                val created = doc.getLong("createdAt") ?: System.currentTimeMillis()
                
                // Save locally if not exists
                val newItem = QrCodeItem(
                    firestoreId = cloudId,
                    userId = currentUser.uid,
                    title = title,
                    category = category,
                    formType = formType,
                    content = content,
                    foregroundColorHex = fg,
                    backgroundColorHex = bg,
                    eyeShape = eye,
                    dotStyle = dot,
                    errorCorrection = ec,
                    logoBase64 = logo,
                    createdAt = created,
                    isSynced = true
                )
                qrDao.insertItem(newItem)
            }
        } catch (e: Exception) {
            Log.e("QrRepository", "Cloud sync error: ${e.message}")
        }
    }

    suspend fun seedDefaultPresetsIfEmpty() = withContext(Dispatchers.IO) {
        val unsynced = qrDao.getUnsyncedItems()
        // If nothing at all in DB, let's check via a quick query or just insert presets if first run
    }
}
