package com.example.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

enum class QrFormType(val title: String, val icon: String) {
    URL("Website URL", "link"),
    WHATSAPP("WhatsApp", "chat"),
    LOCATION("Location", "place"),
    WIFI("Wi-Fi Network", "wifi"),
    VCARD("Contact Card", "contact_page"),
    TEXT("Plain Text", "notes")
}

enum class EyeShape(val label: String) {
    SQUARE("Square (Classic)"),
    ROUNDED("Rounded Corner"),
    CIRCLE("Circle"),
    DIAMOND("Diamond"),
    SQUIRCLE("Squircle (Modern)")
}

enum class DotStyle(val label: String) {
    SQUARE("Square Matrix"),
    ROUNDED("Soft Rounded"),
    DOTS("Dots / Circles"),
    DIAMOND("Diamond Matrix"),
    FLUID("Connected / Fluid")
}

enum class ErrorCorrectionLevel(val level: String, val percent: String) {
    L("L", "7% (Low)"),
    M("M", "15% (Medium)"),
    Q("Q", "25% (Quartile)"),
    H("H", "30% (High - Best for Logos)")
}

data class QrConfig(
    val formType: QrFormType = QrFormType.URL,
    val rawContent: String = "https://online-qr-generator.com",
    val title: String = "My Website QR",
    
    // WhatsApp Fields
    val waPhone: String = "",
    val waMessage: String = "",
    
    // Location Fields
    val locQuery: String = "",
    val locLat: String = "",
    val locLng: String = "",
    
    // Wifi Fields
    val wifiSsid: String = "",
    val wifiPass: String = "",
    val wifiEncryption: String = "WPA",
    val wifiHidden: Boolean = false,
    
    // vCard Fields
    val vcName: String = "",
    val vcPhone: String = "",
    val vcEmail: String = "",
    val vcCompany: String = "",
    val vcTitle: String = "",
    val vcWebsite: String = "",
    
    // Customization Design
    val foregroundColorHex: String = "#0F172A", // Slate 900
    val backgroundColorHex: String = "#FFFFFF",
    val eyeShape: EyeShape = EyeShape.SQUIRCLE,
    val dotStyle: DotStyle = DotStyle.ROUNDED,
    val errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
    
    // Logo / Image
    val logoBase64: String? = null,
    val logoScale: Float = 0.22f, // 22% of QR width
    val hasBorder: Boolean = true
) {
    fun getEffectiveContent(): String {
        return when (formType) {
            QrFormType.URL -> if (rawContent.isBlank()) "https://online-qr-generator.com" else rawContent
            QrFormType.WHATSAPP -> {
                val cleanPhone = waPhone.replace(Regex("[^0-9]"), "")
                if (cleanPhone.isBlank()) "https://wa.me/"
                else {
                    val encodedMsg = java.net.URLEncoder.encode(waMessage, "UTF-8")
                    if (waMessage.isBlank()) "https://wa.me/$cleanPhone"
                    else "https://wa.me/$cleanPhone?text=$encodedMsg"
                }
            }
            QrFormType.LOCATION -> {
                if (locLat.isNotBlank() && locLng.isNotBlank()) {
                    "https://maps.google.com/?q=$locLat,$locLng"
                } else if (locQuery.isNotBlank()) {
                    "https://maps.google.com/?q=" + java.net.URLEncoder.encode(locQuery, "UTF-8")
                } else if (rawContent.isNotBlank()) {
                    rawContent
                } else {
                    "https://maps.google.com/?q=Googleplex+Mountain+View"
                }
            }
            QrFormType.WIFI -> {
                val ssid = wifiSsid.ifBlank { "MyWiFi" }
                val type = wifiEncryption
                val pass = wifiPass
                val hidden = if (wifiHidden) "true" else "false"
                "WIFI:S:$ssid;T:$type;P:$pass;H:$hidden;;"
            }
            QrFormType.VCARD -> {
                val name = vcName.ifBlank { "John Doe" }
                val phone = vcPhone
                val email = vcEmail
                val org = vcCompany
                val title = vcTitle
                val url = vcWebsite
                buildString {
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    appendLine("FN:$name")
                    if (org.isNotBlank()) appendLine("ORG:$org")
                    if (title.isNotBlank()) appendLine("TITLE:$title")
                    if (phone.isNotBlank()) appendLine("TEL;TYPE=CELL:$phone")
                    if (email.isNotBlank()) appendLine("EMAIL:$email")
                    if (url.isNotBlank()) appendLine("URL:$url")
                    append("END:VCARD")
                }
            }
            QrFormType.TEXT -> if (rawContent.isBlank()) "QR Studio Pro - Professional Generator" else rawContent
        }
    }
}
