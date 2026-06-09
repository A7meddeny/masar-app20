package com.masar.portal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import com.masar.portal.model.MeResponse
import com.masar.portal.model.RequestItem
import com.masar.portal.ui.components.MasarCard
import com.masar.portal.ui.components.SectionTitle
import com.masar.portal.ui.theme.*

private val TYPE_LABELS = mapOf(
    "advance"   to "السلف",
    "leave"     to "الإجازات",
    "accident"  to "الحوادث",
    "complaint" to "الشكاوى",
    "fuel"      to "صيانة دورية",
    "report"    to "التقارير",
)

@Composable
fun HistoryScreen(data: MeResponse?, baseUrl: String = "") {
    // محلل آمن لـ history JsonElement → Map<String, List<RequestItem>>
    val history = remember(data?.history) {
        parseHistory(data?.history)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("سجل طلباتي", style = MaterialTheme.typography.headlineSmall, color = TxtPrimary)
        Text(
            "متابعة كل الطلبات السابقة وحالتها لدى الإدارة",
            style = MaterialTheme.typography.bodySmall,
            color = TxtDim,
        )

        if (history.isEmpty() || history.all { it.value.isEmpty() }) {
            EmptyState()
            return@Column
        }

        TYPE_LABELS.forEach { (key, label) ->
            val items = history[key].orEmpty()
            if (items.isNotEmpty()) {
                MasarCard {
                    SectionTitle("$label (${items.size})")
                    Spacer(Modifier.height(10.dp))
                    items.forEach { req ->
                        RequestRow(req, baseUrl)
                        if (req != items.last()) {
                            Divider(color = LineDim, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestRow(req: RequestItem, baseUrl: String = "") {
    val ctx = LocalContext.current
    val (badge, badgeBg) = when (req.status) {
        "approved" -> "معتمد"   to Green
        "rejected" -> "مرفوض"   to Red
        else       -> "بانتظار" to Amber
    }
    // مسار صورة الإيصال (يبقى ثابت في uploads/)
    val receiptUrl: String? = req.receipt_photo?.let { rp ->
        when {
            rp.startsWith("http") -> rp
            baseUrl.isBlank() -> null
            else -> {
                val cleanBase = baseUrl.trimEnd('/')
                if (rp.startsWith("uploads/")) "$cleanBase/$rp" else "$cleanBase/uploads/$rp"
            }
        }
    }

    Column {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeBg.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeBg),
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    color = badgeBg,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            // رقم الطلب
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandRed.copy(alpha = 0.14f),
            ) {
                Text(
                    "#${req.id}",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    color = BrandRed,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    req.details ?: "(بدون تفاصيل)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TxtPrimary,
                    maxLines = 3,
                )
                if (req.amount != null && req.amount > 0) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("المبلغ: ${"%,.2f".format(req.amount)} ﷼",
                            style = MaterialTheme.typography.labelSmall, color = Amber, fontWeight = FontWeight.Bold)
                        // طريقة الدفع للسلف
                        if (!req.payment_method.isNullOrBlank()) {
                            Text(
                                if (req.payment_method == "cash") "💵 نقد" else "🏦 بنكي",
                                style = MaterialTheme.typography.labelSmall,
                                color = TxtSoft,
                            )
                        }
                    }
                }
                // اسم المشرف
                if (!req.supervisor_name.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "المشرف: ${req.supervisor_name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TxtSoft,
                    )
                }
                if (!req.review_note.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Ink3,
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text("رد الإدارة:", style = MaterialTheme.typography.labelSmall, color = TxtDim)
                            Text(req.review_note, style = MaterialTheme.typography.bodySmall, color = TxtSoft)
                        }
                    }
                }
                // 📎 صورة الإيصال (إن وُجدت)
                if (receiptUrl != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Ink3,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Green.copy(alpha = 0.4f)),
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(
                                "📎 صورة إيصال التحويل البنكي",
                                style = MaterialTheme.typography.labelSmall,
                                color = Green,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            AsyncImage(
                                model = receiptUrl,
                                contentDescription = "إيصال البنك",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 280.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(receiptUrl))
                                            ctx.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                            )
                            Text(
                                "اضغط على الصورة للعرض كاملاً",
                                style = MaterialTheme.typography.labelSmall,
                                color = TxtDim,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    (req.created_at ?: "").take(16),
                    style = MaterialTheme.typography.labelSmall,
                    color = TxtDim,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📭", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text("لا توجد طلبات سابقة بعد",
            style = MaterialTheme.typography.titleMedium, color = TxtPrimary)
        Spacer(Modifier.height(6.dp))
        Text("اضغط على «الخدمات» لتقديم أول طلب",
            style = MaterialTheme.typography.bodySmall, color = TxtDim)
    }
}

// محلّل آمن لـ history JsonElement → Map<String, List<RequestItem>>
private fun parseHistory(je: kotlinx.serialization.json.JsonElement?): Map<String, List<RequestItem>> {
    if (je == null) return emptyMap()
    return try {
        val js = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true; explicitNulls = false
            isLenient = true; coerceInputValues = true
        }
        when (je) {
            is kotlinx.serialization.json.JsonObject -> {
                je.mapValues { (_, value) ->
                    if (value is kotlinx.serialization.json.JsonArray) {
                        value.mapNotNull { item ->
                            runCatching {
                                js.decodeFromJsonElement(RequestItem.serializer(), item)
                            }.getOrNull()
                        }
                    } else emptyList()
                }
            }
            else -> emptyMap()
        }
    } catch (e: Exception) { emptyMap() }
}
