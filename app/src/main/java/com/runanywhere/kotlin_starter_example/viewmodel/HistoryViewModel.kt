package com.runanywhere.kotlin_starter_example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.runanywhere.kotlin_starter_example.data.HistoryContentLine
import com.runanywhere.kotlin_starter_example.data.HistoryType
import com.runanywhere.kotlin_starter_example.data.SyncedHistoryItem
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class HistoryViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    
    private val _historyItems = MutableStateFlow<List<SyncedHistoryItem>>(emptyList())
    val historyItems: StateFlow<List<SyncedHistoryItem>> = _historyItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun loadHistory(type: HistoryType? = null) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var query = db.collection("users")
                    .document(uid)
                    .collection("history")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                
                if (type != null) {
                    query = query.whereEqualTo("type", type.name)
                }

                query.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("HistoryViewModel", "Listen failed", e)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            try {
                                val item = doc.toObject(SyncedHistoryItem::class.java)
                                item?.copy(id = doc.id)
                            } catch (ex: Exception) {
                                Log.e("HistoryViewModel", "Mapping error", ex)
                                null
                            }
                        }
                        _historyItems.value = items
                    }
                }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error setting up history listener", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun saveSession(type: HistoryType, content: List<HistoryContentLine>) {
        if (content.isEmpty()) return
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                // 1. Generate Title via LLM
                val contextText = content.take(10).joinToString("\n") { it.text }
                val prompt = "Generate a 2-4 word title for this conversation snippet. Respond with ONLY the title.\nSnippet: $contextText"
                
                var title = try {
                    withContext(Dispatchers.IO) {
                        RunAnywhere.chat(prompt).trim().removeSurrounding("\"").removeSurrounding("'")
                    }
                } catch (e: Exception) {
                    if (type == HistoryType.CONVERSATION) "Conversation" else "Live Captions"
                }
                
                if (title.isBlank() || title.length > 50) {
                    title = if (type == HistoryType.CONVERSATION) "Conversation" else "Live Captions"
                }

                // 2. Save to Firestore
                val item = SyncedHistoryItem(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    title = title,
                    timestamp = System.currentTimeMillis(),
                    content = content
                )
                
                db.collection("users")
                    .document(uid)
                    .collection("history")
                    .document(item.id)
                    .set(item)
                    .await()
                
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error saving session", e)
            }
        }
    }

    fun deleteItem(itemId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(uid)
                    .collection("history")
                    .document(itemId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error deleting item", e)
            }
        }
    }

    fun renameItem(itemId: String, newTitle: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(uid)
                    .collection("history")
                    .document(itemId)
                    .update("title", newTitle)
                    .await()
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error renaming item", e)
            }
        }
    }

    fun shareHistoryItem(context: Context, item: SyncedHistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = buildString {
                    appendLine("Aeris - ${item.title}")
                    appendLine("Date: ${java.util.Date(item.timestamp)}")
                    appendLine("--------------------------")
                    item.content.forEach { line ->
                        val sender = if (line.fromOther) "Person" else "Me"
                        appendLine("$sender: ${line.text}")
                    }
                }
                
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, item.title)
                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Session"))
                }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error sharing item", e)
            }
        }
    }
}
