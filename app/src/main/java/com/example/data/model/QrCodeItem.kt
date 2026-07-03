package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "qr_items")
@Serializable
data class QrCodeItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firestoreId: String = "",
    val userId: String = "", // Firebase user ID if signed in
    val title: String,
    val category: String = "General",
    val formType: String = QrFormType.URL.name,
    val content: String,
    val foregroundColorHex: String = "#0F172A",
    val backgroundColorHex: String = "#FFFFFF",
    val eyeShape: String = EyeShape.SQUIRCLE.name,
    val dotStyle: String = DotStyle.ROUNDED.name,
    val errorCorrection: String = ErrorCorrectionLevel.M.name,
    val logoBase64: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    fun toQrConfig(): QrConfig {
        val fType = try { QrFormType.valueOf(formType) } catch (e: Exception) { QrFormType.URL }
        val eShape = try { EyeShape.valueOf(eyeShape) } catch (e: Exception) { EyeShape.SQUIRCLE }
        val dStyle = try { DotStyle.valueOf(dotStyle) } catch (e: Exception) { DotStyle.ROUNDED }
        val ecLevel = try { ErrorCorrectionLevel.valueOf(errorCorrection) } catch (e: Exception) { ErrorCorrectionLevel.M }
        
        return QrConfig(
            formType = fType,
            rawContent = content,
            title = title,
            foregroundColorHex = foregroundColorHex,
            backgroundColorHex = backgroundColorHex,
            eyeShape = eShape,
            dotStyle = dStyle,
            errorCorrection = ecLevel,
            logoBase64 = logoBase64
        )
    }
}
