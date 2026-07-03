package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DotStyle
import com.example.data.model.ErrorCorrectionLevel
import com.example.data.model.EyeShape
import com.example.data.model.QrConfig
import com.example.qr.QrCodeGeneratorEngine
import com.example.ui.DesignPreset
import java.io.InputStream

@Composable
fun StyleControlsSection(
    config: QrConfig,
    presets: List<DesignPreset>,
    onConfigChanged: (QrConfig) -> Unit,
    onApplyPreset: (DesignPreset) -> Unit,
    onOpenAiLogoStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bmp: Bitmap? = BitmapFactory.decodeStream(inputStream)
                bmp?.let {
                    val base64 = QrCodeGeneratorEngine.bitmapToBase64(it)
                    onConfigChanged(config.copy(
                        logoBase64 = base64,
                        errorCorrection = ErrorCorrectionLevel.H
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Quick Presets
        Text(
            text = "2. Professional Design Presets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { preset ->
                val fgCol = try { Color(android.graphics.Color.parseColor(preset.fgHex)) } catch (e: Exception) { Color.Black }
                val bgCol = try { Color(android.graphics.Color.parseColor(preset.bgHex)) } catch (e: Exception) { Color.White }
                val isSelected = config.foregroundColorHex == preset.fgHex && config.backgroundColorHex == preset.bgHex
                
                Surface(
                    onClick = { onApplyPreset(preset) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.testTag("preset_${preset.name}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(fgCol)
                                .border(1.dp, Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Colors Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Color Palette",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = config.foregroundColorHex,
                        onValueChange = { onConfigChanged(config.copy(foregroundColorHex = it)) },
                        label = { Text("Foreground (Dots)") },
                        placeholder = { Text("#0F172A") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = {
                            val col = try { Color(android.graphics.Color.parseColor(config.foregroundColorHex)) } catch (e: Exception) { Color.Black }
                            Box(modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(1.dp, Color.Gray, CircleShape))
                        }
                    )
                    OutlinedTextField(
                        value = config.backgroundColorHex,
                        onValueChange = { onConfigChanged(config.copy(backgroundColorHex = it)) },
                        label = { Text("Background") },
                        placeholder = { Text("#FFFFFF") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = {
                            val col = try { Color(android.graphics.Color.parseColor(config.backgroundColorHex)) } catch (e: Exception) { Color.White }
                            Box(modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(1.dp, Color.Gray, CircleShape))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Quick Color Chips
                Text("Quick Foreground Swatches:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                val swatches = listOf("#0F172A", "#1E3A8A", "#064E3B", "#701A75", "#B45309", "#DC2626", "#000000")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    swatches.forEach { hex ->
                        val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(if (config.foregroundColorHex == hex) 2.dp else 1.dp, if (config.foregroundColorHex == hex) MaterialTheme.colorScheme.primary else Color.LightGray, CircleShape)
                                .clickable { onConfigChanged(config.copy(foregroundColorHex = hex)) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Eye Shape & Dot Style Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Finder Patterns (Corner Eyes Shape)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EyeShape.values()) { shape ->
                        FilterChip(
                            selected = config.eyeShape == shape,
                            onClick = { onConfigChanged(config.copy(eyeShape = shape)) },
                            label = { Text(shape.label) },
                            leadingIcon = if (config.eyeShape == shape) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Data Modules (Dot Style)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DotStyle.values()) { style ->
                        FilterChip(
                            selected = config.dotStyle == style,
                            onClick = { onConfigChanged(config.copy(dotStyle = style)) },
                            label = { Text(style.label) },
                            leadingIcon = if (config.dotStyle == style) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logo Upload & Error Correction Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Center Logo / Badge",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (config.logoBase64 != null) {
                        IconButton(
                            onClick = { onConfigChanged(config.copy(logoBase64 = null)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Logo", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Image")
                    }
                    Button(
                        onClick = onOpenAiLogoStudio,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Logo Studio")
                    }
                }

                if (config.logoBase64 != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⚠️ Logo attached: Error Correction automatically set to High (30%) for reliable scanning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Error Correction Level: ${config.errorCorrection.percent}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ErrorCorrectionLevel.values().forEach { ec ->
                        val selected = config.errorCorrection == ec
                        Surface(
                            onClick = {
                                if (config.logoBase64 == null) {
                                    onConfigChanged(config.copy(errorCorrection = ec))
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "${ec.level} (${ec.percent.take(3)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
