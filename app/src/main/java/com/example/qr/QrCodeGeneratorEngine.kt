package com.example.qr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Base64
import com.example.data.model.DotStyle
import com.example.data.model.ErrorCorrectionLevel
import com.example.data.model.EyeShape
import com.example.data.model.QrConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel as ZXingEC
import java.io.ByteArrayOutputStream

object QrCodeGeneratorEngine {

    fun generateBitmap(config: QrConfig, sizePx: Int = 1024): Bitmap? {
        try {
            val content = config.getEffectiveContent()
            if (content.isBlank()) return null

            val hints = mutableMapOf<EncodeHintType, Any>()
            val ecLevel = if (config.logoBase64 != null) {
                ZXingEC.H // Force High when logo is present
            } else {
                when (config.errorCorrection) {
                    ErrorCorrectionLevel.L -> ZXingEC.L
                    ErrorCorrectionLevel.M -> ZXingEC.M
                    ErrorCorrectionLevel.Q -> ZXingEC.Q
                    ErrorCorrectionLevel.H -> ZXingEC.H
                }
            }
            hints[EncodeHintType.ERROR_CORRECTION] = ecLevel
            hints[EncodeHintType.MARGIN] = 2 // 2 modules quiet zone
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val writer = QRCodeWriter()
            // Encode at unscaled size to get the exact matrix of modules
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height

            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Parse colors
            val bgColor = try {
                android.graphics.Color.parseColor(config.backgroundColorHex)
            } catch (e: Exception) {
                android.graphics.Color.WHITE
            }
            val fgColor = try {
                android.graphics.Color.parseColor(config.foregroundColorHex)
            } catch (e: Exception) {
                android.graphics.Color.BLACK
            }

            canvas.drawColor(bgColor)

            val moduleSize = sizePx.toFloat() / matrixWidth
            val margin = 2

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fgColor
                style = Paint.Style.FILL
            }
            val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fgColor
                style = Paint.Style.FILL
            }

            // Identify Finder Pattern regions
            // Top-Left: (margin .. margin+6, margin .. margin+6)
            // Top-Right: (width-margin-7 .. width-margin-1, margin .. margin+6)
            // Bottom-Left: (margin .. margin+6, height-margin-7 .. height-margin-1)
            val tlRect = RectF(margin * moduleSize, margin * moduleSize, (margin + 7) * moduleSize, (margin + 7) * moduleSize)
            val trRect = RectF((matrixWidth - margin - 7) * moduleSize, margin * moduleSize, (matrixWidth - margin) * moduleSize, (margin + 7) * moduleSize)
            val blRect = RectF(margin * moduleSize, (matrixHeight - margin - 7) * moduleSize, (margin + 7) * moduleSize, (matrixHeight - margin) * moduleSize)

            // Draw Eyes
            drawEye(canvas, tlRect, config.eyeShape, eyePaint)
            drawEye(canvas, trRect, config.eyeShape, eyePaint)
            drawEye(canvas, blRect, config.eyeShape, eyePaint)

            // Draw Data Dots
            for (y in 0 until matrixHeight) {
                for (x in 0 until matrixWidth) {
                    if (isFinderPattern(x, y, matrixWidth, matrixHeight, margin)) continue
                    if (bitMatrix.get(x, y)) {
                        val left = x * moduleSize
                        val top = y * moduleSize
                        val right = left + moduleSize
                        val bottom = top + moduleSize
                        val rect = RectF(left, top, right, bottom)
                        drawDot(canvas, rect, config.dotStyle, dotPaint, moduleSize)
                    }
                }
            }

            // Overlay Logo if present
            val logoB64 = config.logoBase64
            if (!logoB64.isNullOrBlank()) {
                try {
                    val decodedBytes = Base64.decode(logoB64, Base64.DEFAULT)
                    val logoBmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (logoBmp != null) {
                        val logoSize = (sizePx * config.logoScale).toInt()
                        val centerX = sizePx / 2f
                        val centerY = sizePx / 2f

                        if (config.hasBorder) {
                            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = bgColor
                                style = Paint.Style.FILL
                            }
                            val badgeSize = logoSize * 1.15f
                            val badgeRect = RectF(
                                centerX - badgeSize / 2f,
                                centerY - badgeSize / 2f,
                                centerX + badgeSize / 2f,
                                centerY + badgeSize / 2f
                            )
                            canvas.drawRoundRect(badgeRect, badgeSize * 0.2f, badgeSize * 0.2f, badgePaint)
                        }

                        val scaledLogo = Bitmap.createScaledBitmap(logoBmp, logoSize, logoSize, true)
                        canvas.drawBitmap(
                            scaledLogo,
                            centerX - logoSize / 2f,
                            centerY - logoSize / 2f,
                            null
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun isFinderPattern(x: Int, y: Int, width: Int, height: Int, margin: Int): Boolean {
        // Top-Left eye
        if (x in margin..(margin + 6) && y in margin..(margin + 6)) return true
        // Top-Right eye
        if (x in (width - margin - 7)..(width - margin - 1) && y in margin..(margin + 6)) return true
        // Bottom-Left eye
        if (x in margin..(margin + 6) && y in (height - margin - 7)..(height - margin - 1)) return true
        return false
    }

    private fun drawEye(canvas: Canvas, rect: RectF, shape: EyeShape, paint: Paint) {
        val width = rect.width()
        val unit = width / 7f
        
        when (shape) {
            EyeShape.SQUARE -> {
                // Outer box
                canvas.drawRect(rect, paint)
                // Inner white gap
                val gapPaint = Paint(paint).apply { color = android.graphics.Color.WHITE }
                val gapRect = RectF(rect.left + unit, rect.top + unit, rect.right - unit, rect.bottom - unit)
                canvas.drawRect(gapRect, gapPaint)
                // Inner pupil
                val pupilRect = RectF(rect.left + 2 * unit, rect.top + 2 * unit, rect.right - 2 * unit, rect.bottom - 2 * unit)
                canvas.drawRect(pupilRect, paint)
            }
            EyeShape.ROUNDED -> {
                val rx = width * 0.25f
                canvas.drawRoundRect(rect, rx, rx, paint)
                val gapPaint = Paint(paint).apply { color = android.graphics.Color.WHITE }
                val gapRect = RectF(rect.left + unit, rect.top + unit, rect.right - unit, rect.bottom - unit)
                canvas.drawRoundRect(gapRect, rx * 0.7f, rx * 0.7f, gapPaint)
                val pupilRect = RectF(rect.left + 2 * unit, rect.top + 2 * unit, rect.right - 2 * unit, rect.bottom - 2 * unit)
                canvas.drawRoundRect(pupilRect, rx * 0.5f, rx * 0.5f, paint)
            }
            EyeShape.CIRCLE -> {
                val cx = rect.centerX()
                val cy = rect.centerY()
                canvas.drawCircle(cx, cy, width / 2f, paint)
                val gapPaint = Paint(paint).apply { color = android.graphics.Color.WHITE }
                canvas.drawCircle(cx, cy, (width / 2f) - unit, gapPaint)
                canvas.drawCircle(cx, cy, (width / 2f) - 2 * unit, paint)
            }
            EyeShape.DIAMOND -> {
                val cx = rect.centerX()
                val cy = rect.centerY()
                canvas.save()
                canvas.rotate(45f, cx, cy)
                val scale = 0.75f
                val sWidth = width * scale
                val sRect = RectF(cx - sWidth/2, cy - sWidth/2, cx + sWidth/2, cy + sWidth/2)
                canvas.drawRect(sRect, paint)
                val gapPaint = Paint(paint).apply { color = android.graphics.Color.WHITE }
                val gUnit = sWidth / 7f
                val gapRect = RectF(sRect.left + gUnit, sRect.top + gUnit, sRect.right - gUnit, sRect.bottom - gUnit)
                canvas.drawRect(gapRect, gapPaint)
                val pupilRect = RectF(sRect.left + 2*gUnit, sRect.top + 2*gUnit, sRect.right - 2*gUnit, sRect.bottom - 2*gUnit)
                canvas.drawRect(pupilRect, paint)
                canvas.restore()
            }
            EyeShape.SQUIRCLE -> {
                val rx = width * 0.38f
                canvas.drawRoundRect(rect, rx, rx, paint)
                val gapPaint = Paint(paint).apply { color = android.graphics.Color.WHITE }
                val gapRect = RectF(rect.left + unit, rect.top + unit, rect.right - unit, rect.bottom - unit)
                canvas.drawRoundRect(gapRect, rx * 0.7f, rx * 0.7f, gapPaint)
                val pupilRect = RectF(rect.left + 2 * unit, rect.top + 2 * unit, rect.right - 2 * unit, rect.bottom - 2 * unit)
                canvas.drawRoundRect(pupilRect, rx * 0.5f, rx * 0.5f, paint)
            }
        }
    }

    private fun drawDot(canvas: Canvas, rect: RectF, style: DotStyle, paint: Paint, size: Float) {
        val inset = size * 0.05f // small padding between dots for visual elegance
        val r = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        when (style) {
            DotStyle.SQUARE -> canvas.drawRect(rect, paint)
            DotStyle.ROUNDED -> canvas.drawRoundRect(r, size * 0.35f, size * 0.35f, paint)
            DotStyle.DOTS -> canvas.drawCircle(r.centerX(), r.centerY(), r.width() / 2f, paint)
            DotStyle.DIAMOND -> {
                val path = Path().apply {
                    moveTo(r.centerX(), r.top)
                    lineTo(r.right, r.centerY())
                    lineTo(r.centerX(), r.bottom)
                    lineTo(r.left, r.centerY())
                    close()
                }
                canvas.drawPath(path, paint)
            }
            DotStyle.FLUID -> {
                canvas.drawRoundRect(rect, size * 0.4f, size * 0.4f, paint)
            }
        }
    }

    fun generateSvgString(config: QrConfig): String {
        try {
            val content = config.getEffectiveContent()
            if (content.isBlank()) return "<svg></svg>"
            val hints = mutableMapOf<EncodeHintType, Any>()
            val ecLevel = if (config.logoBase64 != null) ZXingEC.H else ZXingEC.M
            hints[EncodeHintType.ERROR_CORRECTION] = ecLevel
            hints[EncodeHintType.MARGIN] = 2
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val margin = 2

            val sb = StringBuilder()
            sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $w $h" width="100%" height="100%">""")
            sb.appendLine("""  <rect width="100%" height="100%" fill="${config.backgroundColorHex}"/>""")

            val fg = config.foregroundColorHex
            // Draw Dots
            for (y in 0 until h) {
                for (x in 0 until w) {
                    if (isFinderPattern(x, y, w, h, margin)) continue
                    if (bitMatrix.get(x, y)) {
                        when (config.dotStyle) {
                            DotStyle.SQUARE -> sb.appendLine("""  <rect x="$x" y="$y" width="1" height="1" fill="$fg"/>""")
                            DotStyle.ROUNDED, DotStyle.FLUID -> sb.appendLine("""  <rect x="$x" y="$y" width="0.95" height="0.95" rx="0.35" ry="0.35" fill="$fg"/>""")
                            DotStyle.DOTS -> sb.appendLine("""  <circle cx="${x + 0.5}" cy="${y + 0.5}" r="0.45" fill="$fg"/>""")
                            DotStyle.DIAMOND -> sb.appendLine("""  <polygon points="${x+0.5},$y ${x+1},${y+0.5} ${x+0.5},${y+1} $x,${y+0.5}" fill="$fg"/>""")
                        }
                    }
                }
            }

            // Draw Eyes SVG helper
            fun appendSvgEye(left: Int, top: Int) {
                when (config.eyeShape) {
                    EyeShape.SQUARE -> {
                        sb.appendLine("""  <rect x="$left" y="$top" width="7" height="7" fill="$fg"/>""")
                        sb.appendLine("""  <rect x="${left+1}" y="${top+1}" width="5" height="5" fill="${config.backgroundColorHex}"/>""")
                        sb.appendLine("""  <rect x="${left+2}" y="${top+2}" width="3" height="3" fill="$fg"/>""")
                    }
                    EyeShape.ROUNDED, EyeShape.SQUIRCLE -> {
                        sb.appendLine("""  <rect x="$left" y="$top" width="7" height="7" rx="2" ry="2" fill="$fg"/>""")
                        sb.appendLine("""  <rect x="${left+1}" y="${top+1}" width="5" height="5" rx="1.3" ry="1.3" fill="${config.backgroundColorHex}"/>""")
                        sb.appendLine("""  <rect x="${left+2}" y="${top+2}" width="3" height="3" rx="0.8" ry="0.8" fill="$fg"/>""")
                    }
                    EyeShape.CIRCLE -> {
                        sb.appendLine("""  <circle cx="${left+3.5}" cy="${top+3.5}" r="3.5" fill="$fg"/>""")
                        sb.appendLine("""  <circle cx="${left+3.5}" cy="${top+3.5}" r="2.5" fill="${config.backgroundColorHex}"/>""")
                        sb.appendLine("""  <circle cx="${left+3.5}" cy="${top+3.5}" r="1.5" fill="$fg"/>""")
                    }
                    EyeShape.DIAMOND -> {
                        sb.appendLine("""  <rect x="$left" y="$top" width="7" height="7" rx="0.5" ry="0.5" fill="$fg"/>""")
                        sb.appendLine("""  <rect x="${left+1}" y="${top+1}" width="5" height="5" fill="${config.backgroundColorHex}"/>""")
                        sb.appendLine("""  <rect x="${left+2}" y="${top+2}" width="3" height="3" fill="$fg"/>""")
                    }
                }
            }
            appendSvgEye(margin, margin)
            appendSvgEye(w - margin - 7, margin)
            appendSvgEye(margin, h - margin - 7)

            if (!config.logoBase64.isNullOrBlank()) {
                val logoScale = config.logoScale
                val lw = w * logoScale
                val lh = h * logoScale
                val lx = (w - lw) / 2f
                val ly = (h - lh) / 2f
                if (config.hasBorder) {
                    val bw = lw * 1.15f
                    val bx = (w - bw) / 2f
                    val by = (h - bw) / 2f
                    sb.appendLine("""  <rect x="$bx" y="$by" width="$bw" height="$bw" rx="1.5" ry="1.5" fill="${config.backgroundColorHex}"/>""")
                }
                sb.appendLine("""  <image x="$lx" y="$ly" width="$lw" height="$lh" href="data:image/png;base64,${config.logoBase64}"/>""")
            }

            sb.appendLine("</svg>")
            return sb.toString()
        } catch (e: Exception) {
            return "<svg></svg>"
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
