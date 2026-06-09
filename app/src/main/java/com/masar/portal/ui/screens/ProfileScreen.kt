package com.masar.portal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.masar.portal.model.MeResponse
import com.masar.portal.ui.components.InfoRow
import com.masar.portal.ui.components.MasarCard
import com.masar.portal.ui.components.SectionTitle
import com.masar.portal.ui.theme.*

@Composable
fun ProfileScreen(
    baseUrl: String,
    data: MeResponse?,
    driverName: String,
    onLogout: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val info = data?.driver

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ===== صورة + اسم =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Ink2),
                contentAlignment = Alignment.Center,
            ) {
                val photo = info?.driver_photo
                val fullPhoto = when {
                    photo == null -> null
                    photo.startsWith("http") -> photo
                    photo.startsWith("/") -> "$baseUrl$photo"
                    else -> "$baseUrl/$photo"
                }
                if (fullPhoto != null) {
                    AsyncImage(
                        model = fullPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Filled.Person, null, modifier = Modifier.size(60.dp), tint = TxtDim)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(driverName, style = MaterialTheme.typography.headlineSmall, color = TxtPrimary, fontWeight = FontWeight.Bold)
            if (!info?.courier_id.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("معرّف السائق: ${info?.courier_id}", style = MaterialTheme.typography.labelSmall, color = TxtDim)
            }
        }

        // ===== بيانات شخصية =====
        MasarCard {
            SectionTitle("البيانات الشخصية")
            Spacer(Modifier.height(8.dp))
            InfoRow("رقم الإقامة", info?.national_id)
            InfoRow("رقم الجوال",   info?.phone)
            InfoRow("لوحة السيارة",
                if ((info?.plate_letters ?: "").isNotBlank() || (info?.plate_numbers ?: "").isNotBlank())
                    "${info?.plate_letters ?: ""} ${info?.plate_numbers ?: ""}".trim()
                else null
            )
        }

        // ===== التواريخ المهمة =====
        MasarCard {
            SectionTitle("تواريخ مهمة")
            Spacer(Modifier.height(8.dp))
            InfoRow("الإقامة القادمة",     info?.iqama_next_expiry)
            InfoRow("تفويض السيارة",       info?.car_auth_expiry)
            InfoRow("بطاقة السائق",        info?.driver_card_expiry)
            InfoRow("كرت تشغيل السيارة",   info?.car_op_expiry)
        }

        // ===== الحساب البنكي =====
        if (!info?.bank_account.isNullOrBlank() || !info?.iban.isNullOrBlank()) {
            MasarCard {
                SectionTitle("الحساب البنكي")
                Spacer(Modifier.height(8.dp))
                InfoRow("رقم الحساب", info?.bank_account)
                InfoRow("الآيبان",     info?.iban)
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red),
            border = androidx.compose.foundation.BorderStroke(1.dp, Red.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.Filled.ExitToApp, null)
            Spacer(Modifier.width(8.dp))
            Text("تسجيل خروج", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("تأكيد الخروج") },
            text = { Text("سيتم تسجيل خروجك. هل أنت متأكد؟") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("نعم، اخرج", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("إلغاء") }
            },
            containerColor = Ink2,
        )
    }
}
