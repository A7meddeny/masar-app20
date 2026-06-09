package com.masar.portal.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.masar.portal.network.MasarApi
import com.masar.portal.ui.components.*
import com.masar.portal.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ServiceType(val key: String, val label: String, val icon: ImageVector, val color: Color, val askAmount: Boolean = false, val askOdo: Boolean = false, val allowPhoto: Boolean = true)

private val SERVICES = listOf(
    ServiceType("advance",   "طلب سلفة",          Icons.Filled.Payments,        Amber, askAmount = true),
    ServiceType("leave",     "طلب إجازة",         Icons.Filled.EventBusy,       Color(0xFF8AA6E8)),
    ServiceType("accident",  "إبلاغ عن حادث",     Icons.Filled.Warning,         Red, askOdo = true),
    ServiceType("complaint", "تقديم شكوى",        Icons.Filled.Report,          Color(0xFFE8745A)),
    ServiceType("fuel",      "صيانة دورية",       Icons.Filled.Build,           Green),
    ServiceType("report",    "طلب تقرير",         Icons.Filled.Description,     TxtSoft, allowPhoto = false),
)

@Composable
fun ServicesScreen(
    baseUrl: String,
    nid: String,
    token: String,
    onSubmitted: () -> Unit,
) {
    var selected by remember { mutableStateOf<ServiceType?>(null) }

    if (selected == null) {
        ServicesGrid(onSelect = { selected = it })
    } else {
        RequestForm(
            baseUrl = baseUrl,
            nid = nid,
            token = token,
            type = selected!!,
            onCancel = { selected = null },
            onSubmitted = {
                selected = null
                onSubmitted()
            },
        )
    }
}

@Composable
private fun ServicesGrid(onSelect: (ServiceType) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("الخدمات", style = MaterialTheme.typography.headlineSmall, color = TxtPrimary)
        Text(
            "اختر الخدمة التي تحتاج طلبها من الإدارة",
            style = MaterialTheme.typography.bodySmall,
            color = TxtDim,
        )
        Spacer(Modifier.height(6.dp))
        SERVICES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { srv ->
                    ServiceTile(
                        srv = srv,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(srv) },
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f)) {}
            }
        }
    }
}

@Composable
private fun ServiceTile(srv: ServiceType, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Ink2,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineDim),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(srv.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(srv.icon, null, tint = srv.color)
            }
            Text(srv.label, style = MaterialTheme.typography.titleSmall, color = TxtPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestForm(
    baseUrl: String,
    nid: String,
    token: String,
    type: ServiceType,
    onCancel: () -> Unit,
    onSubmitted: () -> Unit,
) {
    var details by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf("") }
    // متغيرات الصيانة الدورية (للنوع fuel فقط)
    var maintKind by remember { mutableStateOf("oil") }       // oil | tire
    var odoNext by remember { mutableStateOf("") }            // قراءة العداد القادمة (للزيت)
    var tireCount by remember { mutableStateOf("1") }         // عدد الكفرات
    var tirePos by remember { mutableStateOf("front") }       // front | rear
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // Image picker
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = TxtPrimary)
            }
            Text(type.label, style = MaterialTheme.typography.headlineSmall, color = TxtPrimary)
        }

        MasarCard {
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("التفاصيل") },
                placeholder = { Text(detailsPlaceholder(type.key)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
                minLines = 4,
            )

            if (type.askAmount) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("المبلغ المطلوب (﷼)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (type.askOdo) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = odo,
                    onValueChange = { odo = it.filter { c -> c.isDigit() } },
                    label = { Text("قراءة العداد (اختياري)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 🔧 قسم الصيانة الدورية (للنوع fuel فقط)
            if (type.key == "fuel") {
                Spacer(Modifier.height(14.dp))
                Text("نوع الصيانة", style = MaterialTheme.typography.titleSmall, color = TxtPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val oilSel = maintKind == "oil"
                    val tireSel = maintKind == "tire"
                    OutlinedButton(
                        onClick = { maintKind = "oil" },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (oilSel) 2.dp else 1.dp,
                            if (oilSel) BrandRed else LineDim
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (oilSel) BrandRed else TxtPrimary,
                            containerColor = if (oilSel) BrandRed.copy(alpha = 0.1f) else Color.Transparent,
                        ),
                    ) {
                        Text("🛢️ غيار زيت", fontWeight = if (oilSel) FontWeight.Bold else FontWeight.Normal)
                    }
                    OutlinedButton(
                        onClick = { maintKind = "tire" },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (tireSel) 2.dp else 1.dp,
                            if (tireSel) BrandRed else LineDim
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (tireSel) BrandRed else TxtPrimary,
                            containerColor = if (tireSel) BrandRed.copy(alpha = 0.1f) else Color.Transparent,
                        ),
                    ) {
                        Text("🛞 غيار كفر", fontWeight = if (tireSel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (maintKind == "oil") {
                    // قراءة العداد الحالية + القادمة
                    OutlinedTextField(
                        value = odo,
                        onValueChange = { odo = it.filter { c -> c.isDigit() } },
                        label = { Text("قراءة العداد الحالية") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = odoNext,
                        onValueChange = { odoNext = it.filter { c -> c.isDigit() } },
                        label = { Text("قراءة العداد القادمة (لتغيير الزيت)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // عدد الكفرات + الموقع
                    OutlinedTextField(
                        value = tireCount,
                        onValueChange = { v ->
                            val n = v.filter { c -> c.isDigit() }.take(1)
                            tireCount = if (n.isEmpty()) "" else n
                        },
                        label = { Text("عدد الكفرات المُغيَّرة (1-4)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("الموقع", style = MaterialTheme.typography.labelMedium, color = TxtDim)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("front" to "أمامي", "rear" to "خلفي", "both" to "أمامي + خلفي").forEach { (k, l) ->
                            val sel = tirePos == k
                            OutlinedButton(
                                onClick = { tirePos = k },
                                modifier = Modifier.weight(1f),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (sel) 2.dp else 1.dp,
                                    if (sel) BrandRed else LineDim
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (sel) BrandRed else TxtPrimary,
                                    containerColor = if (sel) BrandRed.copy(alpha = 0.1f) else Color.Transparent,
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) {
                                Text(l, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // قسم رفع الصورة
        if (type.allowPhoto) {
            MasarCard {
                Text("الصورة (اختياري)", style = MaterialTheme.typography.titleSmall, color = TxtPrimary)
                Text(
                    photoHelpText(type.key),
                    style = MaterialTheme.typography.labelSmall,
                    color = TxtDim,
                )
                Spacer(Modifier.height(10.dp))
                if (photoUri != null) {
                    Box(Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { photoPicker.launch("image/*") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تغيير")
                        }
                        OutlinedButton(
                            onClick = { photoUri = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red),
                        ) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("حذف")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = BrandRed,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("اختر صورة", color = TxtPrimary)
                        }
                    }
                }
            }
        }

        if (message != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = (if (isError) Red else Green).copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isError) Red else Green),
            ) {
                Text(
                    message!!,
                    modifier = Modifier.padding(12.dp),
                    color = if (isError) Red else Green,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Button(
            onClick = {
                if (details.isBlank()) {
                    message = "اكتب تفاصيل الطلب أولاً"
                    isError = true
                    return@Button
                }
                loading = true; message = null
                scope.launch {
                    // ابنِ التفاصيل النهائية حسب نوع الطلب
                    val finalDetails: String = if (type.key == "fuel") {
                        val userNote = if (details.isNotBlank()) "\n💬 ملاحظة: $details" else ""
                        if (maintKind == "oil") {
                            val cur = if (odo.isNotBlank()) odo else "—"
                            val next = if (odoNext.isNotBlank()) odoNext else "—"
                            "🛢️ صيانة دورية — غيار زيت\n• قراءة العداد الحالية: $cur كم\n• قراءة العداد القادمة: $next كم$userNote"
                        } else {
                            val cnt = if (tireCount.isNotBlank()) tireCount else "—"
                            val pos = when (tirePos) {
                                "front" -> "أمامي"
                                "rear"  -> "خلفي"
                                "both"  -> "أمامي + خلفي"
                                else    -> "—"
                            }
                            "🛞 صيانة دورية — غيار كفر\n• عدد الكفرات المُغيَّرة: $cnt\n• الموقع: $pos$userNote"
                        }
                    } else {
                        details
                    }

                    // اقرأ الصورة كـ bytes (في خيط IO)
                    val photoBytes: ByteArray? = if (photoUri != null) {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                ctx.contentResolver.openInputStream(photoUri!!)?.use { stream ->
                                    val buf = ByteArrayOutputStream()
                                    val tmp = ByteArray(8192)
                                    var n = stream.read(tmp)
                                    while (n > 0) {
                                        buf.write(tmp, 0, n)
                                        n = stream.read(tmp)
                                    }
                                    buf.toByteArray()
                                }
                            }.getOrNull()
                        }
                    } else null

                    // قراءة العداد للإرسال (للحادث والصيانة)
                    val odoForApi: Int? = odo.toIntOrNull()

                    val res = MasarApi(baseUrl).submitRequestWithPhoto(
                        nid = nid,
                        token = token,
                        type = type.key,
                        details = finalDetails,
                        amount = amount.toDoubleOrNull(),
                        odometer = odoForApi,
                        photoBytes = photoBytes,
                    )
                    loading = false
                    if (res.ok) {
                        message = res.message ?: "تم استلام طلبك"
                        isError = false
                        kotlinx.coroutines.delay(1500)
                        onSubmitted()
                    } else {
                        message = res.error ?: "تعذّر الإرسال"
                        isError = true
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("إرسال الطلب", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun detailsPlaceholder(type: String): String = when (type) {
    "advance"   -> "اكتب سبب طلب السلفة..."
    "leave"     -> "اكتب فترة الإجازة وسببها..."
    "accident"  -> "اكتب وصف الحادث ومكانه..."
    "complaint" -> "اكتب تفاصيل الشكوى..."
    "fuel"      -> "اكتب تفاصيل الصيانة الدورية أو الإيصال..."
    "report"    -> "اكتب نوع التقرير المطلوب..."
    else        -> "اكتب التفاصيل..."
}

private fun photoHelpText(type: String): String = when (type) {
    "advance"   -> "أرفق مستند داعم إن وُجد"
    "leave"     -> "أرفق صورة الاسكليف الطبي أو إثبات الغياب"
    "accident"  -> "أرفق صورة من الحادث أو الأضرار"
    "complaint" -> "أرفق صورة توضّح الشكوى إن وُجدت"
    "fuel"      -> "أرفق صورة إيصال الصيانة الدورية"
    else        -> "أرفق صورة داعمة للطلب"
}
