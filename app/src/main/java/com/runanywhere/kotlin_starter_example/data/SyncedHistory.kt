package com.runanywhere.kotlin_starter_example.data

data class SyncedHistoryItem(
    val id: String = "",
    val type: HistoryType = HistoryType.CONVERSATION,
    val title: String = "New Session",
    val timestamp: Long = System.currentTimeMillis(),
    val content: List<HistoryContentLine> = emptyList()
)

data class HistoryContentLine(
    val text: String = "",
    val fromOther: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f
)

enum class HistoryType {
    CONVERSATION, CAPTION, DETECTION
}
