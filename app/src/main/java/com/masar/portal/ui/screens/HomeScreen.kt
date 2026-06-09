package com.masar.portal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import com.masar.portal.model.RequestItem
import com.masar.portal.network.MasarApi
import com.masar.portal.ui.components.*
import com.masar.portal.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    baseUrl: String,
    driverName: String,
    nid: String,
    token: String,
    data: MeResponse?,
    loading: Boolean,
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    // قائمة محلية للرسائل عشان نقدر نخفيها بعد القراءة
    var dismissedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ===== رأس الصفحة: ترحيب + صورة المندوب =====
        HeaderCard(baseUrl, driverName, data)

        if (loading && data == null) {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandRed)
            }
            return@Column
        }

        if (data?.ok == true) {
            // 🚫 لافتة حالة الحساب (إذا موقوف أو إجازة)
            val status = data.account_status ?: data.driver?.status
            if (status == "inactive") {
                StatusBanner(
                    icon = "🚫",
                    title = "الحساب موقوف",
                    message = "تواصل مع المشرف لمعرفة السبب وإعادة التفعيل.",
                    bgColor = Red.copy(alpha = 0.15f),
                    fgColor = Red,
                )
                return@Column
            } else if (status == "on_leave") {
                StatusBanner(
                    icon = "🏖️",
                    title = "أنت في إجازة",
                    message = "حسابك في وضع الإجازة. تواصل مع المشرف عند الرجوع.",
                    bgColor = Amber.copy(alpha = 0.15f),
                    fgColor = Amber,
                )
            }

            // ===== رسائل المشرف غير المقروءة =====
            val supMsgs = parseSupervisorMessages(data.supervisor_messages)
            val unread = supMsgs.filter {
                it.seen_by_driver_at == null && !dismissedIds.contains(it.id)
            }
            if (unread.isNotEmpty()) {
                SupervisorMessagesCard(
                    messages = unread,
                    onDismiss = { msg ->
                        dismissedIds = dismissedIds + msg.id
                        scope.launch {
                            runCatching {
                                MasarApi(baseUrl).markSupervisorMessageSeen(nid, token, msg.id)
                            }
                        }
                    }
                )
            }

            // 📅 شارة الشهر الحالي
            MonthBadge(data.current_month)

            PerformanceOverviewCard(data)
            SalaryCard(data)
            VerifyCard(data)                  // 📷 معدّل التحقق
            LastDayCard(data)
            QuickStatsRow(data)
        }
    }
}

// شارة الشهر الحالي
@Composable
private fun MonthBadge(currentMonth: String?) {
    if (currentMonth.isNullOrBlank()) return
    val monthsAr = listOf("يناير","فبراير","مارس","أبريل","مايو","يونيو",
                          "يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر")
    val parts = currentMonth.split("-")
    if (parts.size < 2) return
    val year = parts[0]
    val monthIdx = (parts[1].toIntOrNull() ?: 1) - 1
    val monthName = monthsAr.getOrNull(monthIdx) ?: parts[1]

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Amber.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Amber.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📅", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Text(
                "بيانات شهر $monthName $year",
                style = MaterialTheme.typography.titleSmall,
                color = Amber,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// لافتة حالة الحساب (موقوف / إجازة)
@Composable
private fun StatusBanner(
    icon: String,
    title: String,
    message: String,
    bgColor: Color,
    fgColor: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, fgColor.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = fgColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = TxtPrimary)
            }
        }
    }
}

// محلّل آمن لـ supervisor_messages من JsonElement
private fun parseSupervisorMessages(je: kotlinx.serialization.json.JsonElement?): List<RequestItem> {
    if (je == null) return emptyList()
    return try {
        val js = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true; explicitNulls = false
            isLenient = true; coerceInputValues = true
        }
        when {
            je is kotlinx.serialization.json.JsonArray -> {
                je.mapNotNull { item ->
                    runCatching {
                        js.decodeFromJsonElement(RequestItem.serializer(), item)
                    }.getOrNull()
                }
            }
            else -> emptyList()
        }
    } catch (e: Exception) { emptyList() }
}

@Composable
private fun HeaderCard(baseUrl: String, driverName: String, data: MeResponse?) {
    val photo = data?.driver?.driver_photo
    // المسار قد يكون نسبيًا (uploads/xxx.jpg) — حوّله لكامل
    val fullPhoto = when {
        photo == null -> null
        photo.startsWith("http") -> photo
        photo.startsWith("/") -> "$baseUrl$photo"
        else -> "$baseUrl/$photo"
    }

    GradientCard(
        gradient = listOf(BrandRed, BrandRedDark),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (fullPhoto != null) {
                    AsyncImage(
                        model = fullPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text("مرحبًا", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
                Text(
                    driverName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                val cid = data?.driver?.courier_id
                if (!cid.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("معرّف: $cid", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// ==== بطاقة رسائل المشرف ====
@Composable
private fun SupervisorMessagesCard(
    messages: List<RequestItem>,
    onDismiss: (RequestItem) -> Unit,
) {
    val typeLabel = mapOf(
        "complaint" to "📢 شكوى من المشرف",
        "accident"  to "🚨 تنبيه حادث",
        "advance"   to "💰 إشعار سلفة",
        "leave"     to "📅 إشعار إجازة",
        "fuel"      to "🔧 صيانة دورية",
        "report"    to "📋 تقرير من المشرف",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BrandRed.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandRed.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = BrandRed,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "رسائل من المشرف (${messages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandRed,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            messages.forEach { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Ink2,
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            typeLabel[msg.type] ?: msg.type,
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandRed,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        if (!msg.details.isNullOrBlank()) {
                            Text(
                                msg.details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TxtPrimary,
                            )
                        }
                        if (msg.amount != null && msg.amount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "المبلغ: ${formatMoney(msg.amount)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TxtSoft,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!msg.created_at.isNullOrBlank()) {
                                Text(
                                    msg.created_at.take(16),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TxtDim,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onDismiss(msg) }) {
                                Text("تم القراءة", color = BrandRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceOverviewCard(data: MeResponse) {
    val perf = data.perf
    val sup = data.sup

    MasarCard {
        SectionTitle("نظرة عامة على الأداء")
        Spacer(Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryBadge(level = perf?.level_cat ?: "-")
            Column(Modifier.weight(1f)) {
                Text("الفئة الحالية", style = MaterialTheme.typography.labelSmall, color = TxtDim)
                Text(
                    perf?.level_cat ?: "غير مصنّف",
                    style = MaterialTheme.typography.titleMedium,
                    color = TxtPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (perf?.city_rank != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("الترتيب", style = MaterialTheme.typography.labelSmall, color = TxtDim)
                    Text("#${perf.city_rank}", style = MaterialTheme.typography.titleMedium, color = BrandRed, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // إجمالي الطلبات (من شيت الأداء / المشرف) كرقم بارز
        val totalOrders = data.total_orders ?: data.perf?.volume ?: 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("إجمالي الطلبات", style = MaterialTheme.typography.labelMedium, color = TxtDim)
                Text(
                    "$totalOrders",
                    style = MaterialTheme.typography.displaySmall,
                    color = BrandRed,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (data.on_time_pct != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("في الموعد", style = MaterialTheme.typography.labelMedium, color = TxtDim)
                    val color = when {
                        data.on_time_pct >= 90 -> Green
                        data.on_time_pct >= 80 -> Amber
                        else -> Red
                    }
                    Text(
                        "%.1f%%".format(data.on_time_pct),
                        style = MaterialTheme.typography.headlineSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // التقدم نحو الهدف
        val target = data.salary?.target ?: 600
        val achieved = data.salary?.effective_delivered ?: totalOrders
        ProgressRow(label = "التقدم نحو الهدف ($target طلب)", current = achieved, target = target)

        Spacer(Modifier.height(16.dp))

        // إحصاءات سريعة
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("الإلغاءات", "${sup?.cancellations ?: 0}",
                accent = if ((sup?.cancellations ?: 0) > 0) Red else TxtDim, modifier = Modifier.weight(1f))
            StatCard("أيام العمل", "${sup?.vda_days ?: 0}",
                accent = if ((sup?.vda_days ?: 0) >= 26) Green else Amber, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SalaryCard(data: MeResponse) {
    val s = data.salary ?: return

    // عرض فقط الخصومات والغرامات (حذف المكافآت/الإضافات حسب طلب المستخدم)
    MasarCard {
        SectionTitle("ملخّص الحساب الشهري")
        Spacer(Modifier.height(8.dp))
        Text(
            "يعرض الخصومات والغرامات فقط. الراتب النهائي يُحسب من قِبل المحاسبة.",
            style = MaterialTheme.typography.labelSmall,
            color = TxtDim,
        )
        Spacer(Modifier.height(14.dp))

        // 🔴 الخصومات والغرامات فقط
        if (s.deductions.isNotEmpty()) {
            Text("الخصومات والغرامات", style = MaterialTheme.typography.labelSmall, color = TxtDim)
            Spacer(Modifier.height(6.dp))
            s.deductions.forEach { d ->
                InfoRow(d.t, "- ${formatMoney(d.a)}", valueColor = Red)
            }
        } else {
            Text(
                "✅ لا توجد خصومات أو غرامات حتى الآن.",
                style = MaterialTheme.typography.bodySmall,
                color = Green,
            )
        }
    }
}

@Composable
private fun LastDayCard(data: MeResponse) {
    val ld = data.last_day ?: return
    val deliv = ld.deliv ?: 0
    // لا تظهر البطاقة إذا لا توجد بيانات لآخر يوم
    if (deliv == 0 && (ld.acc ?: 0) == 0 && (ld.rej ?: 0) == 0) return

    MasarCard {
        SectionTitle("ملخّص طلبات آخر يوم")
        Spacer(Modifier.height(6.dp))
        val dayDate = ld.day_date
        if (!dayDate.isNullOrBlank()) {
            Surface(
                color = BrandRed.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📅", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("تاريخ آخر يوم", style = MaterialTheme.typography.labelSmall, color = TxtDim)
                        Text(dayDate, style = MaterialTheme.typography.titleSmall, color = BrandRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        InfoRow("المسلّمة", "$deliv")
        InfoRow("الموكّلة", "${ld.acc ?: 0}")
        InfoRow("المرفوضة", "${ld.rej ?: 0}",
            valueColor = if ((ld.rej ?: 0) > 0) Red else TxtPrimary)
        InfoRow("المتأخرة", "${ld.late_total ?: 0}",
            valueColor = if ((ld.late_total ?: 0) > 0) Amber else TxtPrimary)
        if ((ld.big ?: 0) > 0) {
            InfoRow("الكبيرة", "${ld.big}")
        }
        // 🚫 حذف ساعات العمل حسب طلب المستخدم
    }
}

@Composable
private fun QuickStatsRow(data: MeResponse) {
    val orders = data.orders
    MasarCard {
        SectionTitle("ملخّص الطلبات (الشهري)")
        Spacer(Modifier.height(12.dp))
        InfoRow("إجمالي المسلّمة", "${orders?.deliv ?: 0}")
        InfoRow("الموكّلة",        "${orders?.acc ?: 0}")
        InfoRow("المرفوضة",        "${orders?.rej ?: 0}",
            valueColor = if ((orders?.rej ?: 0) > 0) Red else TxtPrimary)
        InfoRow("المتأخرة",         "${orders?.late_total ?: 0}",
            valueColor = if ((orders?.late_total ?: 0) > 0) Amber else TxtPrimary)
        // 🚫 حذف "أيام عمل (تقرير)" حسب طلب المستخدم
        // تاريخ واحد فقط — آخر تاريخ توصيل
        val lastDate = data.last_date ?: orders?.last_d
        if (!lastDate.isNullOrBlank()) {
            InfoRow("آخر تاريخ توصيل", lastDate)
        }
    }
}

// 📷 بطاقة معدّل التحقق (Verification Selfie)
@Composable
private fun VerifyCard(data: MeResponse) {
    val v = data.verify ?: return
    val triggered = v.triggered ?: 0
    val passed = v.passed ?: 0
    val rate = v.rate ?: 0.0
    if (triggered == 0) return  // لا توجد بيانات تحقق

    val pct = (rate * 100).toInt()
    val (bgColor, fgColor, icon, label) = when {
        rate >= 0.90 -> Quad(Green.copy(alpha = 0.12f), Green,  "✅", "ناجح")
        rate >= 0.79 -> Quad(Amber.copy(alpha = 0.16f), Amber,  "⚠️", "لم يجتز التحقق")
        else         -> Quad(Red.copy(alpha = 0.12f),   Red,    "🚨", "يحتاج مراجعة")
    }

    MasarCard {
        SectionTitle("📷 معدّل التحقق")
        Spacer(Modifier.height(6.dp))
        Text(
            "التحقق من زي المندوب والسيارة عند فتح الشفت",
            style = MaterialTheme.typography.labelSmall,
            color = TxtDim,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, fgColor.copy(alpha = 0.4f)),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "$pct%",
                            style = MaterialTheme.typography.displaySmall,
                            color = fgColor,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = fgColor)
                    }
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = rate.toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = fgColor,
                    trackColor = fgColor.copy(alpha = 0.15f),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "صوّرت $passed يوم من أصل $triggered يوم مفروض",
                    style = MaterialTheme.typography.bodySmall,
                    color = TxtPrimary,
                )
            }
        }
    }
}

// Helper class لـ 4 قيم
private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

internal fun formatMoney(v: Double): String {
    val rounded = (v * 100).toLong() / 100.0
    return "%,.2f ﷼".format(rounded)
}
