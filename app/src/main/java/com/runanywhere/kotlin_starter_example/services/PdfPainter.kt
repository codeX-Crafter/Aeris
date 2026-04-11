package com.runanywhere.kotlin_starter_example.services

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.runanywhere.kotlin_starter_example.data.CaptionLine
import com.runanywhere.kotlin_starter_example.data.HistoryContentLine
import java.text.SimpleDateFormat
import java.util.*

class PdfPainter(
    private val document: PdfDocument,
    private val pageWidth: Int,
    private val pageHeight: Int,
    private val margin: Float
) {
    private var pageNumber = 1
    private var currentY = margin
    private lateinit var canvas: Canvas
    private lateinit var page: PdfDocument.Page

    private val contentWidth = pageWidth - (margin * 2)

    init { newPage() }

    private fun newPage() {
        if (::page.isInitialized) {
            drawFooterInternal()
            document.finishPage(page)
        }

        val pageInfo = PdfDocument.PageInfo.Builder(
            pageWidth, pageHeight, pageNumber++
        ).create()

        page = document.startPage(pageInfo)
        canvas = page.canvas
        currentY = margin
    }

    fun drawHeader(title: String, subtitle: String, accentColor: Int) {
        canvas.drawRect(
            margin, currentY, margin + contentWidth, currentY + 3f,
            Paint().apply { color = accentColor }
        )
        currentY += 16f

        canvas.drawText(title, margin, currentY + 20f, Paint().apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.parseColor("#1A1A1A")
        })
        currentY += 28f

        canvas.drawText(subtitle, margin, currentY + 14f, Paint().apply {
            textSize = 12f
            color = Color.parseColor("#666666")
        })
        currentY += 24f
    }

    fun drawCaption(caption: CaptionLine) {
        val textPaint = Paint().apply {
            textSize = 13f
            color = Color.parseColor("#1A1A1A")
        }

        val timePaint = Paint().apply {
            textSize = 10f
            color = Color.parseColor("#999999")
        }

        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val timeString = timeFormat.format(Date(caption.timestamp))

        checkPageBreak(40f)

        canvas.drawText("[$timeString]", margin, currentY + 12f, timePaint)
        currentY += 16f

        val lines = wrapText(caption.text, textPaint, contentWidth)

        lines.forEach { line ->
            checkPageBreak(22f)
            canvas.drawText(line, margin, currentY + 14f, textPaint)
            currentY += 20f
        }
    }

    fun drawHistoryLine(line: HistoryContentLine) {
        val sender = if (line.fromOther) "Person" else "Me"
        val senderPaint = Paint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (line.fromOther) Color.parseColor("#1A73E8") else Color.parseColor("#9C6FFC")
        }

        val textPaint = Paint().apply {
            textSize = 13f
            color = Color.parseColor("#1A1A1A")
        }

        val timePaint = Paint().apply {
            textSize = 10f
            color = Color.parseColor("#999999")
        }

        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val timeString = timeFormat.format(Date(line.timestamp))

        checkPageBreak(50f)

        canvas.drawText("$sender • $timeString", margin, currentY + 12f, senderPaint)
        currentY += 16f

        val lines = wrapText(line.text, textPaint, contentWidth)

        lines.forEach { textLine ->
            checkPageBreak(22f)
            canvas.drawText(textLine, margin, currentY + 14f, textPaint)
            currentY += 20f
        }
    }

    fun addSpacing(amount: Float) {
        currentY += amount
        checkPageBreak(0f)
    }

    private fun checkPageBreak(requiredSpace: Float) {
        if (currentY + requiredSpace > pageHeight - margin - 30f) {
            newPage()
        }
    }

    private fun drawFooterInternal() {
        canvas.drawText(
            "Exported by Aeris - Page $pageNumber",
            margin,
            pageHeight - margin,
            Paint().apply {
                textSize = 10f
                color = Color.GRAY
            }
        )
    }

    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val test = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(test) <= maxWidth) {
                currentLine = test
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    fun finish() {
        if (::page.isInitialized) {
            drawFooterInternal()
            document.finishPage(page)
        }
    }
}
