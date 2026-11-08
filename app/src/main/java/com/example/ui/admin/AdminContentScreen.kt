package com.example.ui.admin

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppConfig
import com.example.data.model.BannerItem
import com.example.data.model.PrizeItem

@Composable
fun AdminContentScreen(
    appConfig: AppConfig,
    announcements: List<AnnouncementItem>,
    banners: List<BannerItem>,
    prizes: List<PrizeItem>,
    onSaveConfig: (AppConfig, String) -> Unit,
    onSaveAnnouncement: (String?, String, String, String, Int, Boolean) -> Unit,
    onDeleteAnnouncement: (String, String) -> Unit,
    onSaveBanner: (String?, String, String, String, String, Boolean) -> Unit,
    onDeleteBanner: (String, String) -> Unit,
    onSavePrize: (String?, String, String, String, String, Boolean) -> Unit,
    onDeletePrize: (String, String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var section by remember { mutableStateOf("notices") } // notices | banners | prizes | links_terms

    // Notice form state
    var noticeTitle by remember { mutableStateOf("") }
    var noticeBody by remember { mutableStateOf("") }
    var noticeCategory by remember { mutableStateOf("announcement") }
    var noticeClassText by remember { mutableStateOf("0") }

    // Banner form state
    var bannerTitle by remember { mutableStateOf("") }
    var bannerSubtitle by remember { mutableStateOf("") }
    var bannerImgUrl by remember { mutableStateOf("") }
    var bannerActionUrl by remember { mutableStateOf("") }

    // Prize form state
    var prizeRank by remember { mutableStateOf("Rank 1") }
    var prizeTitle by remember { mutableStateOf("") }
    var prizeDesc by remember { mutableStateOf("") }
    var prizeImgUrl by remember { mutableStateOf("") }

    // Global Links, Support & Terms state synced from AppConfig
    var youtubeUrl by remember { mutableStateOf(appConfig.youtubeUrl) }
    var whatsappUrl by remember { mutableStateOf(appConfig.whatsappUrl) }
    var upiId by remember { mutableStateOf(appConfig.upiId) }
    var subPriceText by remember { mutableStateOf(appConfig.subscriptionPriceText) }
    var supportInfo by remember { mutableStateOf(appConfig.supportContactText) }
    var versionBanner by remember { mutableStateOf(appConfig.appVersionBanner) }
    var termsEn by remember { mutableStateOf(appConfig.termsAndConditionsEn) }
    var termsHi by remember { mutableStateOf(appConfig.termsAndConditionsHi) }

    LaunchedEffect(appConfig) {
        youtubeUrl = appConfig.youtubeUrl
        whatsappUrl = appConfig.whatsappUrl
        upiId = appConfig.upiId
        subPriceText = appConfig.subscriptionPriceText
        supportInfo = appConfig.supportContactText
        versionBanner = appConfig.appVersionBanner
        termsEn = appConfig.termsAndConditionsEn
        termsHi = appConfig.termsAndConditionsHi
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_content_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
                Text(
                    text = "Content, Links, Terms & Gift Management",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onBackground
                )
                Text(
                    text = "Publish announcements, HTTPS banners, educational gift items, channel links, and Terms & Conditions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.secondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = section == "notices",
                    onClick = { section = "notices" },
                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                    label = { Text("Notices (${announcements.size})") }
                )
                FilterChip(
                    selected = section == "banners",
                    onClick = { section = "banners" },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                    label = { Text("Banners (${banners.size})") }
                )
                FilterChip(
                    selected = section == "prizes",
                    onClick = { section = "prizes" },
                    leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null) },
                    label = { Text("Gifts & Prizes (${prizes.size})") }
                )
                FilterChip(
                    selected = section == "links_terms",
                    onClick = { section = "links_terms" },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    label = { Text("Links, Support & Terms") }
                )
            }
        }

        when (section) {
            "notices" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Publish Announcement / Notice", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("announcement", "notice", "notification", "update").forEach { cat ->
                                    FilterChip(
                                        selected = noticeCategory == cat,
                                        onClick = { noticeCategory = cat },
                                        label = { Text(cat.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = noticeTitle,
                                onValueChange = { noticeTitle = it },
                                label = { Text("Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = noticeBody,
                                onValueChange = { noticeBody = it },
                                label = { Text("Message Body") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = noticeClassText,
                                onValueChange = { noticeClassText = it },
                                label = { Text("Target Class (0 = All Classes, or 1-12)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val targetCls = noticeClassText.toIntOrNull()?.coerceIn(0, 12) ?: 0
                                    onSaveAnnouncement(null, noticeTitle, noticeBody, noticeCategory, targetCls, true)
                                    noticeTitle = ""
                                    noticeBody = ""
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Publish Notice")
                            }
                        }
                    }
                }

                items(announcements, key = { it.id.ifBlank { it.hashCode().toString() } }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "[${item.category.uppercase()}] ${item.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = scheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(item.message, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                Text(
                                    text = if (item.targetClass == 0L) "Audience: All Classes" else "Audience: Class ${item.targetClass}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.secondary
                                )
                            }
                            Switch(
                                checked = item.isActive,
                                onCheckedChange = { active ->
                                    onSaveAnnouncement(item.id, item.title, item.message, item.category, item.targetClass.toInt(), active)
                                }
                            )
                            IconButton(onClick = { onDeleteAnnouncement(item.id, item.title) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Notice", tint = scheme.error)
                            }
                        }
                    }
                }
            }

            "banners" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add Dashboard Banner (HTTPS Image URL)", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                            OutlinedTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = { Text("Banner Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = bannerSubtitle, onValueChange = { bannerSubtitle = it }, label = { Text("Subtitle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = bannerImgUrl, onValueChange = { bannerImgUrl = it }, label = { Text("HTTPS Image URL (https://...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = bannerActionUrl, onValueChange = { bannerActionUrl = it }, label = { Text("Optional Click Link URL (https://...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onSaveBanner(null, bannerTitle, bannerSubtitle, bannerImgUrl, bannerActionUrl, true)
                                    if (bannerImgUrl.trim().startsWith("https://")) {
                                        bannerTitle = ""
                                        bannerSubtitle = ""
                                        bannerImgUrl = ""
                                        bannerActionUrl = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Banner")
                            }
                        }
                    }
                }

                items(banners, key = { it.id.ifBlank { it.hashCode().toString() } }) { b ->
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                if (b.subtitle.isNotBlank()) Text(b.subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                Text(b.imageUrl, style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                            }
                            Switch(
                                checked = b.isActive,
                                onCheckedChange = { active ->
                                    onSaveBanner(b.id, b.title, b.subtitle, b.imageUrl, b.actionUrl, active)
                                }
                            )
                            IconButton(onClick = { onDeleteBanner(b.id, b.title) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Banner", tint = scheme.error)
                            }
                        }
                    }
                }
            }

            "prizes" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add Gift / Educational Prize Item", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                            OutlinedTextField(value = prizeRank, onValueChange = { prizeRank = it }, label = { Text("Rank Label (e.g. Rank 1)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = prizeTitle, onValueChange = { prizeTitle = it }, label = { Text("Reward Title (Age-Appropriate Educational Item)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = prizeDesc, onValueChange = { prizeDesc = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = prizeImgUrl, onValueChange = { prizeImgUrl = it }, label = { Text("HTTPS Gift Image URL (https://...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onSavePrize(null, prizeRank, prizeTitle, prizeDesc, prizeImgUrl, true)
                                    if (prizeImgUrl.trim().startsWith("https://")) {
                                        prizeTitle = ""
                                        prizeDesc = ""
                                        prizeImgUrl = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Gift / Prize Item")
                            }
                        }
                    }
                }

                items(prizes, key = { it.id.ifBlank { it.hashCode().toString() } }) { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${p.rankLabel}  •  ${p.title}", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                Text(p.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                Text(p.imageUrl, style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                            }
                            Switch(
                                checked = p.isActive,
                                onCheckedChange = { active ->
                                    onSavePrize(p.id, p.rankLabel, p.title, p.description, p.imageUrl, active)
                                }
                            )
                            IconButton(onClick = { onDeletePrize(p.id, p.title) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Prize", tint = scheme.error)
                            }
                        }
                    }
                }
            }

            else -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Channel Links, Support & Payment Instructions", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                            OutlinedTextField(value = youtubeUrl, onValueChange = { youtubeUrl = it }, label = { Text("YouTube Channel HTTPS Link") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = whatsappUrl, onValueChange = { whatsappUrl = it }, label = { Text("WhatsApp Channel / Contact HTTPS Link") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("Manual UPI ID / Payment Instructions") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = subPriceText, onValueChange = { subPriceText = it }, label = { Text("Subscription Plan Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = supportInfo, onValueChange = { supportInfo = it }, label = { Text("Support Information") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = versionBanner, onValueChange = { versionBanner = it }, label = { Text("Dashboard App Version Banner") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = scheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Terms and Conditions (English & Hindi)", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                            }
                            OutlinedTextField(
                                value = termsEn,
                                onValueChange = { termsEn = it },
                                label = { Text("Custom Terms & Conditions (English — leave blank for standard clauses)") },
                                minLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = termsHi,
                                onValueChange = { termsHi = it },
                                label = { Text("Custom Terms & Conditions (Hindi — leave blank for standard clauses)") },
                                minLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    onSaveConfig(
                                        appConfig.copy(
                                            youtubeUrl = youtubeUrl,
                                            whatsappUrl = whatsappUrl,
                                            upiId = upiId,
                                            subscriptionPriceText = subPriceText,
                                            supportContactText = supportInfo,
                                            appVersionBanner = versionBanner,
                                            termsAndConditionsEn = termsEn,
                                            termsAndConditionsHi = termsHi
                                        ),
                                        "Updated channel links, support info, and Terms & Conditions"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_global_content_config_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Links, Support Info & Terms")
                            }
                        }
                    }
                }
            }
        }
    }
}
