package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAiService
import com.example.auth.AuthManager
import com.example.auth.UserProfile
import com.example.data.local.QrDatabase
import com.example.data.model.DotStyle
import com.example.data.model.ErrorCorrectionLevel
import com.example.data.model.EyeShape
import com.example.data.model.QrCodeItem
import com.example.data.model.QrConfig
import com.example.data.model.QrFormType
import com.example.data.repository.QrRepository
import com.example.qr.QrCodeGeneratorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DesignPreset(
    val name: String,
    val fgHex: String,
    val bgHex: String,
    val eyeShape: EyeShape,
    val dotStyle: DotStyle,
    val ecLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M
)

class QrViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QrRepository(QrDatabase.getDatabase(application).qrDao())

    val savedItems: StateFlow<List<QrCodeItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<UserProfile?> = AuthManager.currentUser

    private val _currentConfig = MutableStateFlow(QrConfig())
    val currentConfig: StateFlow<QrConfig> = _currentConfig.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    private val _generatedSvg = MutableStateFlow<String>("")
    val generatedSvg: StateFlow<String> = _generatedSvg.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0=Create, 1=Style, 2=AI Studio, 3=History
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // AI Status flags
    private val _isSearchingLink = MutableStateFlow(false)
    val isSearchingLink: StateFlow<Boolean> = _isSearchingLink.asStateFlow()

    private val _isSearchingLocation = MutableStateFlow(false)
    val isSearchingLocation: StateFlow<Boolean> = _isSearchingLocation.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _aiMessage = MutableStateFlow<String?>(null)
    val aiMessage: StateFlow<String?> = _aiMessage.asStateFlow()

    val presets = listOf(
        DesignPreset("Classic Slate", "#0F172A", "#FFFFFF", EyeShape.SQUIRCLE, DotStyle.ROUNDED),
        DesignPreset("Corporate Blue", "#1E3A8A", "#F0F9FF", EyeShape.ROUNDED, DotStyle.DOTS),
        DesignPreset("Emerald Mint", "#064E3B", "#ECFDF5", EyeShape.DIAMOND, DotStyle.DIAMOND),
        DesignPreset("Neon Cyber", "#D946EF", "#090D16", EyeShape.CIRCLE, DotStyle.FLUID),
        DesignPreset("Golden Cream", "#B45309", "#FFFBEB", EyeShape.SQUARE, DotStyle.SQUARE),
        DesignPreset("Sunset Orange", "#EA580C", "#FFF7ED", EyeShape.SQUIRCLE, DotStyle.ROUNDED)
    )

    init {
        regenerateQr()
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    fun setTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun updateConfig(newConfig: QrConfig) {
        _currentConfig.value = newConfig
        regenerateQr()
    }

    fun applyPreset(preset: DesignPreset) {
        val updated = _currentConfig.value.copy(
            foregroundColorHex = preset.fgHex,
            backgroundColorHex = preset.bgHex,
            eyeShape = preset.eyeShape,
            dotStyle = preset.dotStyle,
            errorCorrection = preset.ecLevel
        )
        updateConfig(updated)
    }

    fun regenerateQr() {
        viewModelScope.launch(Dispatchers.Default) {
            val bmp = QrCodeGeneratorEngine.generateBitmap(_currentConfig.value, 1024)
            val svg = QrCodeGeneratorEngine.generateSvgString(_currentConfig.value)
            _generatedBitmap.value = bmp
            _generatedSvg.value = svg
        }
    }

    fun clearAiMessage() {
        _aiMessage.value = null
    }

    // --- AI Studio Features ---

    fun searchSmartLink(brandQuery: String) {
        if (brandQuery.isBlank()) return
        viewModelScope.launch {
            _isSearchingLink.value = true
            _aiMessage.value = "Searching official link for '$brandQuery' via Google Search Grounding..."
            val (url, summary) = GeminiAiService.searchSmartLink(brandQuery)
            _isSearchingLink.value = false
            _aiMessage.value = summary
            
            updateConfig(_currentConfig.value.copy(
                formType = QrFormType.URL,
                rawContent = url,
                title = brandQuery.take(25) + " QR"
            ))
        }
    }

    fun searchSmartLocation(placeQuery: String) {
        if (placeQuery.isBlank()) return
        viewModelScope.launch {
            _isSearchingLocation.value = true
            _aiMessage.value = "Grounding coordinates for '$placeQuery' via Google Maps..."
            val (lat, lng, address) = GeminiAiService.searchSmartLocation(placeQuery)
            _isSearchingLocation.value = false
            _aiMessage.value = "Found: $address ($lat, $lng)"
            
            updateConfig(_currentConfig.value.copy(
                formType = QrFormType.LOCATION,
                locQuery = address,
                locLat = lat,
                locLng = lng,
                title = placeQuery.take(25) + " Map"
            ))
        }
    }

    fun generateAiLogo(prompt: String, aspectRatio: String = "1:1", asBackground: Boolean = false) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isGeneratingImage.value = true
            _aiMessage.value = "Generating AI Logo with ratio $aspectRatio..."
            val (bitmap, msg) = GeminiAiService.generateQrLogoOrArt(prompt, aspectRatio)
            _isGeneratingImage.value = false
            _aiMessage.value = msg

            if (bitmap != null) {
                val base64 = QrCodeGeneratorEngine.bitmapToBase64(bitmap)
                if (asBackground) {
                    // Could set background or save as art
                    updateConfig(_currentConfig.value.copy(
                        logoBase64 = base64,
                        errorCorrection = ErrorCorrectionLevel.H
                    ))
                } else {
                    updateConfig(_currentConfig.value.copy(
                        logoBase64 = base64,
                        errorCorrection = ErrorCorrectionLevel.H
                    ))
                }
            }
        }
    }

    // --- Save & Cloud Sync ---

    fun saveCurrentQrToLibrary(customTitle: String? = null) {
        viewModelScope.launch {
            val config = _currentConfig.value
            val item = QrCodeItem(
                title = customTitle ?: config.title.ifBlank { "${config.formType.title} Code" },
                category = config.formType.name,
                formType = config.formType.name,
                content = config.getEffectiveContent(),
                foregroundColorHex = config.foregroundColorHex,
                backgroundColorHex = config.backgroundColorHex,
                eyeShape = config.eyeShape.name,
                dotStyle = config.dotStyle.name,
                errorCorrection = config.errorCorrection.name,
                logoBase64 = config.logoBase64,
                createdAt = System.currentTimeMillis()
            )
            repository.saveItem(item)
            _aiMessage.value = "✨ Saved to Library! " + if (currentUser.value != null) "(Synced to Firebase Cloud)" else "(Local DB)"
        }
    }

    fun loadSavedItem(item: QrCodeItem) {
        val config = item.toQrConfig()
        updateConfig(config)
        setTab(0) // Switch to create tab to view/edit
        _aiMessage.value = "Loaded '${item.title}' into editor"
    }

    fun deleteSavedItem(item: QrCodeItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _aiMessage.value = "Deleted '${item.title}'"
        }
    }

    fun syncCloudNow() {
        viewModelScope.launch {
            _aiMessage.value = "Syncing with Cloud Firestore..."
            repository.syncWithCloud()
            _aiMessage.value = "☁️ Cloud sync complete!"
        }
    }

    fun loginDemoUser(name: String, email: String) {
        AuthManager.signInDemoUser(name, email)
        viewModelScope.launch {
            repository.syncWithCloud()
        }
        _aiMessage.value = "Signed in as $name (Firebase Cloud Enabled)"
    }

    fun logout() {
        AuthManager.signOut()
        _aiMessage.value = "Signed out"
    }
}
