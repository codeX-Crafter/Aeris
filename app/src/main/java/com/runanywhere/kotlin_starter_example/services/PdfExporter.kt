package com.runanywhere.kotlin_starter_example.services

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.runanywhere.kotlin_starter_example.data.CaptionsExport
import com.runanywhere.kotlin_starter_example.data.HistoryType
import com.runanywhere.kotlin_starter_example.data.SyncedHistoryItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    fun export(
        context: Context,
        exportData: CaptionsExport
    ): Uri? {
        val document = PdfDocument()
        val painter = PdfPainter(document, PAGE_WIDTH, PAGE_HEIGHT, MARGIN)

        painter.drawHeader(
            title = "Aeris Live Captions",
            subtitle = exportData.formattedDate,
            accentColor = Color.parseColor("#1A73E8")
        )

        painter.addSpacing(24f)

        // Limit to avoid huge PDFs
        exportData.captions
            .takeLast(500)
            .forEach { caption ->
                painter.drawCaption(caption)
                painter.addSpacing(12f)
            }

        // Finalize last page properly
        painter.finish()

        return saveToStorage(context.applicationContext, document, exportData.filename)
            .also { document.close() }
    }

    fun exportHistory(
        context: Context,
        item: SyncedHistoryItem
    ): Uri? {
        val document = PdfDocument()
        val painter = PdfPainter(document, PAGE_WIDTH, PAGE_HEIGHT, MARGIN)

        val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(item.timestamp))
        val headerTitle = if (item.type == HistoryType.CONVERSATION) "Aeris Conversation" else "Aeris Live Captions"
        val accentColor = if (item.type == HistoryType.CONVERSATION) Color.parseColor("#9C6FFC") else Color.parseColor("#1A73E8")

        painter.drawHeader(
            title = headerTitle,
            subtitle = "${item.title} ($dateStr)",
            accentColor = accentColor
        )

        painter.addSpacing(24f)

        item.content.forEach { line ->
            painter.drawHistoryLine(line)
            painter.addSpacing(12f)
        }

        painter.finish()

        val filename = "Aeris_${item.type.name}_${System.currentTimeMillis()}.pdf"
        return saveToStorage(context.applicationContext, document, filename)
            .also { document.close() }
    }

    private fun saveToStorage(
        context: Context,
        document: PdfDocument,
        filename: String
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, document, filename)
        } else {
            saveToLegacyStorage(context, document, filename)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(
        context: Context,
        document: PdfDocument,
        filename: String
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        ) ?: return null

        resolver.openOutputStream(uri)?.use { stream ->
            document.writeTo(stream)
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return uri
    }

    private fun saveToLegacyStorage(
        context: Context,
        document: PdfDocument,
        filename: String
    ): Uri? {
        val dir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, filename)
        FileOutputStream(file).use { document.writeTo(it) }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }
}
