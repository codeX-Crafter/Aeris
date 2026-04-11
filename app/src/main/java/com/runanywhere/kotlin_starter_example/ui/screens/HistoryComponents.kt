package com.runanywhere.kotlin_starter_example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.kotlin_starter_example.data.HistoryType
import com.runanywhere.kotlin_starter_example.data.SyncedHistoryItem
import com.runanywhere.kotlin_starter_example.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryDrawerContent(
    historyViewModel: HistoryViewModel,
    type: HistoryType,
    onItemSelected: (SyncedHistoryItem) -> Unit
) {
    val items by historyViewModel.historyItems.collectAsState()
    val searchQuery by historyViewModel.searchQuery.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var itemToRename by remember { mutableStateOf<SyncedHistoryItem?>(null) }
    var newTitleInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            if (type == HistoryType.CONVERSATION) "Chat History" else "Caption History",
            fontSize = 24.sp, 
            fontWeight = FontWeight.Bold, 
            color = Color(0xFF1A2340)
        )
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { historyViewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search history...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color(0xFF6FB1FC),
                unfocusedBorderColor = Color(0xFFEEF0F5)
            )
        )
        
        Spacer(Modifier.height(16.dp))
        
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6FB1FC))
            }
        } else {
            val filtered = items.filter { 
                it.type == type && (it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.any { line -> line.text.contains(searchQuery, ignoreCase = true) })
            }
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { item ->
                    HistoryItemRow(
                        item = item,
                        onClick = { onItemSelected(item) },
                        onDelete = { historyViewModel.deleteItem(item.id) },
                        onRename = { 
                            itemToRename = item
                            newTitleInput = item.title 
                        },
                        onShare = { historyViewModel.shareHistoryItem(context, item) }
                    )
                }
            }
        }
    }

    if (itemToRename != null) {
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Rename Session") },
            text = {
                OutlinedTextField(
                    value = newTitleInput,
                    onValueChange = { newTitleInput = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    itemToRename?.let { historyViewModel.renameItem(it.id, newTitleInput) }
                    itemToRename = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HistoryItemRow(
    item: SyncedHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateString = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp))

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF1A2340))
                Text(dateString, fontSize = 11.sp, color = Color.Gray)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; onRename() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { onShare(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}
