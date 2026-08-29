package com.aura.defense.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuraPdfReportBuilder(private val context: Context) {

        companion object {
                    private const val PAGE_WIDTH = 595
                            private const val PAGE_HEIGHT = 842
                                    private const val MARGIN = 50
                                            private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        }

            fun generate(
                        score: Int,
                                status: String,
                                        findings: List<FindingsItem>,
                                                appCount: Int,
                                                        riskyAppCount: Int,
                                                                vpnActive: Boolean,
                                                                        dnsStatus: String,
                                                                                outputPath: File
            ): Boolean = runCatching {
                        val pdf = PdfDocument()
                                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                                        val page = pdf.startPage(pageInfo)
                                                val canvas = page.canvas
                                                        var y = MARGIN.toFloat()

                                                                val titlePaint = Paint().apply { color = Color.parseColor("#00E5FF"); textSize = 24f; isFakeBoldText = true }
                                                                        val headerPaint = Paint().apply { color = Color.parseColor("#00E5FF"); textSize = 14f; isFakeBoldText = true }
                                                                                val bodyPaint = Paint().apply { color = Color.WHITE; textSize = 11f }
                                                                                        val mutedPaint = Paint().apply { color = Color.parseColor("#888888"); textSize = 10f }
                                                                                                val bgPaint = Paint().apply { color = Color.parseColor("#0D1117") }

                                                                                                        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

                                                                                                                canvas.drawText("AURA DEFENSE", MARGIN.toFloat(), y, titlePaint)
                                                                                                                        y += 20f
                                                                                                                                canvas.drawText("Security Report", MARGIN.toFloat(), y, mutedPaint)
                                                                                                                                        y += 10f
                                                                                                                                                canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", MARGIN.toFloat(), y, mutedPaint)
                                                                                                                                                        y += 35f

                                                                                                                                                                canvas.drawText("SCORE: $score/100  |  $status", MARGIN.toFloat(), y, headerPaint)
                                                                                                                                                                        y += 30f

                                                                                                                                                                                canvas.drawText("TELEMETRY", MARGIN.toFloat(), y, headerPaint)
                                                                                                                                                                                        y += 20f
                                                                                                                                                                                                canvas.drawText("Apps scanned: $appCount  |  Risky apps: $riskyAppCount", MARGIN.toFloat(), y, bodyPaint)
                                                                                                                                                                                                        y += 18f
                                                                                                                                                                                                                canvas.drawText("VPN: ${if (vpnActive) "Active" else "Inactive"}  |  DNS: $dnsStatus", MARGIN.toFloat(), y, bodyPaint)
                                                                                                                                                                                                                        y += 30f

                                                                                                                                                                                                                                canvas.drawText("FINDINGS", MARGIN.toFloat(), y, headerPaint)
                                                                                                                                                                                                                                        y += 20f
                                                                                                                                                                                                                                                findings.take(20).forEach { item ->
                                                                                                                                                                                                                                                            if (y > PAGE_HEIGHT - MARGIN) return@forEach
                                                                                                                                                                                                                                                                        canvas.drawText("[${item.severity}] ${item.title}", MARGIN.toFloat(), y, bodyPaint)
                                                                                                                                                                                                                                                                                    y += 15f
                                                                                                                                                                                                                                                                                                canvas.drawText(item.evidence, (MARGIN + 10).toFloat(), y, mutedPaint)
                                                                                                                                                                                                                                                                                                            y += 20f
                                                                                                                                                                                                                                                                                                                    }

                                                                                                                                                                                                                                                                                                                            if (findings.isEmpty()) {
                                                                                                                                                                                                                                                                                                                                            canvas.drawText("No risk signals detected.", MARGIN.toFloat(), y, bodyPaint)
                                                                                                                                                                                                                                                                                                                            }

                                                                                                                                                                                                                                                                                                                                    y = (PAGE_HEIGHT - MARGIN).toFloat()
                                                                                                                                                                                                                                                                                                                                            canvas.drawText("Aura Defense — Private Mobile Cyberdefense", MARGIN.toFloat(), y, mutedPaint)

                                                                                                                                                                                                                                                                                                                                                    pdf.finishPage(page)
                                                                                                                                                                                                                                                                                                                                                            FileOutputStream(outputPath).use { pdf.writeTo(it) }
                                                                                                                                                                                                                                                                                                                                                                    pdf.close()
                                                                                                                                                                                                                                                                                                                                                                            true
            }.onFailure { Timber.e(it, "PDF generation failed") }.getOrDefault(false)
}

data class FindingsItem(val severity: String, val title: String, val evidence: String)
