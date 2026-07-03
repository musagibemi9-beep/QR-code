package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QrConfig
import com.example.data.model.QrFormType

@Composable
fun InputFormsSection(
    config: QrConfig,
    onConfigChanged: (QrConfig) -> Unit,
    onOpenAiLinkSearch: () -> Unit,
    onOpenAiLocationSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Form Type Selector Tabs
        Text(
            text = "1. Select Content Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(QrFormType.values()) { type ->
                val isSelected = config.formType == type
                val iconVector = getIconForFormType(type)
                
                Surface(
                    onClick = { onConfigChanged(config.copy(formType = type)) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("form_type_${type.name}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = type.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Form Content
        AnimatedContent(
            targetState = config.formType,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "FormTypeTransition"
        ) { targetType ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title for saving to library
                    OutlinedTextField(
                        value = config.title,
                        onValueChange = { onConfigChanged(config.copy(title = it)) },
                        label = { Text("QR Preset Title") },
                        placeholder = { Text("e.g. My Website QR") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("qr_title_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    when (targetType) {
                        QrFormType.URL -> UrlForm(config, onConfigChanged, onOpenAiLinkSearch)
                        QrFormType.WHATSAPP -> WhatsAppForm(config, onConfigChanged)
                        QrFormType.LOCATION -> LocationForm(config, onConfigChanged, onOpenAiLocationSearch)
                        QrFormType.WIFI -> WifiForm(config, onConfigChanged)
                        QrFormType.VCARD -> VCardForm(config, onConfigChanged)
                        QrFormType.TEXT -> TextForm(config, onConfigChanged)
                    }
                }
            }
        }
    }
}

@Composable
fun UrlForm(
    config: QrConfig,
    onConfigChanged: (QrConfig) -> Unit,
    onOpenAiLinkSearch: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Website / Link URL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = onOpenAiLinkSearch,
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "AI Search", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Smart Link", fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = config.rawContent,
            onValueChange = { onConfigChanged(config.copy(rawContent = it)) },
            label = { Text("URL (https://...)") },
            placeholder = { Text("https://www.example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("url_input"),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
        )
    }
}

@Composable
fun WhatsAppForm(config: QrConfig, onConfigChanged: (QrConfig) -> Unit) {
    Column {
        Text(
            text = "WhatsApp Chat Direct Link",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = config.waPhone,
            onValueChange = { onConfigChanged(config.copy(waPhone = it)) },
            label = { Text("Phone Number with Country Code") },
            placeholder = { Text("+1234567890") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wa_phone_input"),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = config.waMessage,
            onValueChange = { onConfigChanged(config.copy(waMessage = it)) },
            label = { Text("Default Message (Optional)") },
            placeholder = { Text("Hi! I would like to inquire about your services.") },
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wa_message_input"),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
fun LocationForm(
    config: QrConfig,
    onConfigChanged: (QrConfig) -> Unit,
    onOpenAiLocationSearch: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Google Maps Location",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = onOpenAiLocationSearch,
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Place, contentDescription = "AI Location", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Smart Finder", fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = config.locQuery,
            onValueChange = { onConfigChanged(config.copy(locQuery = it)) },
            label = { Text("Place Name or Address") },
            placeholder = { Text("Eiffel Tower, Paris") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = config.locLat,
                onValueChange = { onConfigChanged(config.copy(locLat = it)) },
                label = { Text("Latitude") },
                placeholder = { Text("48.8584") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = config.locLng,
                onValueChange = { onConfigChanged(config.copy(locLng = it)) },
                label = { Text("Longitude") },
                placeholder = { Text("2.2945") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
fun WifiForm(config: QrConfig, onConfigChanged: (QrConfig) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = "Wi-Fi Network Auto-Connect",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = config.wifiSsid,
            onValueChange = { onConfigChanged(config.copy(wifiSsid = it)) },
            label = { Text("Network Name (SSID)") },
            placeholder = { Text("MyHomeWiFi_5G") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = config.wifiPass,
            onValueChange = { onConfigChanged(config.copy(wifiPass = it)) },
            label = { Text("Password") },
            placeholder = { Text("SecretPassword123") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                Button(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Security: ${config.wifiEncryption}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("WPA", "WEP", "nopass").forEach { enc ->
                        DropdownMenuItem(
                            text = { Text(enc) },
                            onClick = {
                                onConfigChanged(config.copy(wifiEncryption = enc))
                                expanded = false
                            }
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = config.wifiHidden,
                    onCheckedChange = { onConfigChanged(config.copy(wifiHidden = it)) }
                )
                Text("Hidden Network", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun VCardForm(config: QrConfig, onConfigChanged: (QrConfig) -> Unit) {
    Column {
        Text(
            text = "Contact Card (vCard)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = config.vcName,
            onValueChange = { onConfigChanged(config.copy(vcName = it)) },
            label = { Text("Full Name") },
            placeholder = { Text("John Doe") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = { Icon(Icons.Default.Contacts, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = config.vcPhone,
                onValueChange = { onConfigChanged(config.copy(vcPhone = it)) },
                label = { Text("Phone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = config.vcEmail,
                onValueChange = { onConfigChanged(config.copy(vcEmail = it)) },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = config.vcCompany,
                onValueChange = { onConfigChanged(config.copy(vcCompany = it)) },
                label = { Text("Company") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = config.vcTitle,
                onValueChange = { onConfigChanged(config.copy(vcTitle = it)) },
                label = { Text("Job Title") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = config.vcWebsite,
            onValueChange = { onConfigChanged(config.copy(vcWebsite = it)) },
            label = { Text("Website") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
fun TextForm(config: QrConfig, onConfigChanged: (QrConfig) -> Unit) {
    Column {
        Text(
            text = "Plain Text / Memo / Code",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = config.rawContent,
            onValueChange = { onConfigChanged(config.copy(rawContent = it)) },
            label = { Text("Enter Any Text Content") },
            placeholder = { Text("Coupon codes, serial numbers, notes...") },
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) }
        )
    }
}

fun getIconForFormType(type: QrFormType): ImageVector {
    return when (type) {
        QrFormType.URL -> Icons.Default.Link
        QrFormType.WHATSAPP -> Icons.Default.Chat
        QrFormType.LOCATION -> Icons.Default.Place
        QrFormType.WIFI -> Icons.Default.Wifi
        QrFormType.VCARD -> Icons.Default.Contacts
        QrFormType.TEXT -> Icons.Default.Notes
    }
}
