package com.masar.portal.ui.screens

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.masar.portal.model.IFormItem
import com.masar.portal.network.MasarApi
import com.masar.portal.ui.components.*
import com.masar.portal.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.ByteArrayOutputStream

private val TYPE_LABELS = mapOf(
    "vacation" to ("📅 طلب إجازة" to Color(0xFF8AA6E8)),
    "leave"    to ("📅 طلب إجازة" to Color(0xFF8AA6E8)),
    "warning"  to ("⚠️ إنذار رسمي" to Amber),
    "advance"  to ("💰 طلب سلفة"  to Green),
)

@Composable
fun IFormsScreen(
    baseUrl: String,
    nid: String,
    token: String,
) {
    var forms by remember { mutableStateOf<List<IFormItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedForm by remember { mutableStateOf<IFormItem?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        val resp = MasarApi(baseUrl).fetchIForms(nid, token)
        forms = if (resp.ok) resp.forms else emptyList()
        loading = false
    }

    if (selectedForm != null) {
        IFormDetailScreen(
            baseUrl = baseUrl,
            nid = nid,
            token = token,
            form = selectedForm!!,
            onBack = { selectedForm = null },
            onSubmitted = {
                selectedForm = null
                refreshKey++
            },
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("النماذج التفاعلية", style = MaterialTheme.typography.headlineSmall,
            color = TxtPrimary, fontWeight = FontWeight.Bold)
        Text("اضغط على نموذج لمراجعته وتوقيعه.",
            style = MaterialTheme.typography.bodySmall, color = TxtDim)

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandRed)
            }
            return@Column
        }

        if (forms.isEmpty()) {
            EmptyFormsState()
            return@Column
        }

        // النماذج المعلّقة (لم يوقّعها بعد) - status = sent
        val pending = forms.filter { it.status == "sent" || it.status == "rejected" || it.status == "pending" }
        if (pending.isNotEmpty()) {
            MasarCard {
                SectionTitle("⏰ تنتظر التوقيع (${pending.size})")
                Spacer(Modifier.height(10.dp))
                pending.forEach { f ->
                    FormCard(f, onClick = { selectedForm = f })
                    if (f != pending.last()) Spacer(Modifier.height(8.dp))
                }
            }
        }

        // النماذج الموقّعة
        val signed = forms.filter { it.status !in listOf("sent", "rejected", "pending") }
        if (signed.isNotEmpty()) {
            MasarCard {
                SectionTitle("📋 السجل (${signed.size})")
                Spacer(Modifier.height(10.dp))
                signed.forEach { f ->
                    FormCard(f, onClick = { selectedForm = f })
                    if (f != signed.last()) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FormCard(form: IFormItem, onClick: () -> Unit) {
    val (label, color) = TYPE_LABELS[form.type] ?: ("📄 ${form.type}" to TxtSoft)
    val (badge, badgeColor) = when (form.status) {
        "sent", "pending" -> ("بانتظار توقيعك" to Amber)
        "driver_signed", "filled" -> ("وُقّع - بانتظار اعتماد" to Color(0xFF8AA6E8))
        "supervisor_signed", "approved" -> ("✅ معتمد" to Green)
        "rejected" -> ("❌ مرفوض" to Red)
        else -> (form.status to TxtSoft)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Ink2,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineDim),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleSmall,
                    color = color, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(
                    color = badgeColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor, fontWeight = FontWeight.Bold)
                }
            }
            if (!form.created_at.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("أُرسل: ${form.created_at}", style = MaterialTheme.typography.labelSmall, color = TxtDim)
            }
        }
    }
}

@Composable
private fun EmptyFormsState() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Description, contentDescription = null,
                tint = TxtDim, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("لا توجد نماذج بعد", style = MaterialTheme.typography.bodyMedium, color = TxtDim)
            Spacer(Modifier.height(4.dp))
            Text("عند إرسال نموذج من المشرف ستظهر هنا.",
                style = MaterialTheme.typography.labelSmall, color = TxtDim)
        }
    }
}

@Composable
private fun IFormDetailScreen(
    baseUrl: String,
    nid: String,
    token: String,
    form: IFormItem,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // قائمة الحقول المعبأة (key → value)
    val fields = remember { mutableStateMapOf<String, String>() }

    // تحميل البيانات السابقة إن وُجدت
    LaunchedEffect(form.id) {
        if (!form.data_json.isNullOrBlank() && form.data_json != "{}") {
            try {
                val obj = Json.parseToJsonElement(form.data_json) as? JsonObject
                obj?.forEach { (k, v) ->
                    fields[k] = (v as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull ?: ""
                }
            } catch (_: Exception) {}
        }
    }

    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasWidth by remember { mutableStateOf(0) }
    var canvasHeight by remember { mutableStateOf(0) }
    var submitting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    val (label, color) = TYPE_LABELS[form.type] ?: ("📄 نموذج" to TxtSoft)
    val isPending = form.status in listOf("sent", "rejected", "pending")
    val hasSignature = strokes.isNotEmpty() || currentStroke.isNotEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = TxtPrimary)
            }
            Text(label, style = MaterialTheme.typography.titleLarge,
                color = color, fontWeight = FontWeight.Bold)
        }

        // عرض حقول النموذج (مختلفة حسب النوع)
        when (form.type) {
            "vacation", "leave" -> VacationFields(fields, isPending)
            "warning"           -> WarningFields(fields, isPending)
            "advance"           -> AdvanceFields(fields, isPending)
            else                -> {
                MasarCard {
                    Text("نوع غير معروف: ${form.type}", color = TxtDim)
                }
            }
        }

        // ملاحظة الرفض إن وُجدت
        if (!form.rejection_reason.isNullOrBlank()) {
            Surface(color = Red.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("سبب الرفض السابق:",
                        style = MaterialTheme.typography.labelSmall, color = Red,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(form.rejection_reason, style = MaterialTheme.typography.bodySmall, color = TxtPrimary)
                }
            }
        }

        // التوقيع (للنماذج المعلّقة فقط)
        if (isPending) {
            MasarCard {
                SectionTitle("✍️ توقيعك")
                Spacer(Modifier.height(6.dp))
                Text("ارسم توقيعك بإصبعك أدناه",
                    style = MaterialTheme.typography.labelSmall, color = TxtDim)
                Spacer(Modifier.height(10.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, LineDim, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> currentStroke = listOf(offset) },
                                onDrag = { change, _ -> currentStroke = currentStroke + change.position },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) {
                                        strokes.add(currentStroke.toList())
                                        currentStroke = emptyList()
                                    }
                                },
                                onDragCancel = {
                                    if (currentStroke.isNotEmpty()) {
                                        strokes.add(currentStroke.toList())
                                        currentStroke = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        canvasWidth = size.width.toInt()
                        canvasHeight = size.height.toInt()
                        (strokes + listOf(currentStroke)).forEach { stroke ->
                            for (i in 1 until stroke.size) {
                                drawLine(Color.Black, stroke[i - 1], stroke[i],
                                    strokeWidth = 5f, cap = StrokeCap.Round)
                            }
                        }
                    }
                    if (!hasSignature) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("اضغط هنا وارسم توقيعك", color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            strokes.clear()
                            currentStroke = emptyList()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("مسح") }

                    Button(
                        onClick = {
                            // فحص الحقول المطلوبة
                            val missing = validateFields(form.type, fields)
                            if (missing != null) {
                                result = "⚠️ $missing"
                                return@Button
                            }
                            if (!hasSignature) {
                                result = "⚠️ يجب رسم التوقيع أولاً"
                                return@Button
                            }
                            scope.launch {
                                submitting = true
                                val base64 = strokesToBase64(strokes.toList(), canvasWidth, canvasHeight)
                                val resp = MasarApi(baseUrl).signIForm(
                                    nid = nid, token = token, formId = form.id,
                                    signatureBase64 = base64, action = "sign",
                                    formData = fields.toMap()
                                )
                                submitting = false
                                if (resp.ok) {
                                    result = "✅ تم توقيع النموذج وإرساله للمشرف"
                                    kotlinx.coroutines.delay(1500)
                                    onSubmitted()
                                } else {
                                    result = "❌ ${resp.error ?: "فشل الإرسال"}"
                                }
                            }
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        enabled = !submitting,
                    ) {
                        Text(if (submitting) "جارٍ الإرسال..." else "✓ توقيع وإرسال")
                    }
                }

                if (result != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(result!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result!!.startsWith("✅")) Green else Red)
                }
            }
        }
    }
}

// ============================================================
// حقول نموذج الإجازة
// ============================================================
@Composable
private fun VacationFields(fields: MutableMap<String, String>, editable: Boolean) {
    MasarCard {
        SectionTitle("📅 بيانات الإجازة")
        Spacer(Modifier.height(10.dp))
        OptionsField("نوع الإجازة", "leaveType", fields, editable, listOf(
            "annual" to "إجازة سنوية",
            "sick" to "إجازة مرضية",
            "emergency" to "إجازة طارئة",
            "other" to "أخرى"
        ))
        Spacer(Modifier.height(10.dp))
        TextField2(fields, "leave_days", "عدد أيام الإجازة", editable, KeyboardType.Number)
        TextField2(fields, "start_date", "تاريخ بداية الإجازة (YYYY-MM-DD)", editable)
        TextField2(fields, "end_date", "تاريخ نهاية الإجازة (YYYY-MM-DD)", editable)
        TextField2(fields, "last_day", "آخر يوم عمل (YYYY-MM-DD)", editable)
        TextField2(fields, "return_date", "تاريخ العودة (YYYY-MM-DD)", editable)
        TextField2(fields, "destination", "الوجهة", editable)
        TextField2(fields, "reason", "سبب الخروج", editable, minLines = 2)
        TextField2(fields, "address_phone", "العنوان ورقم الهاتف أثناء الإجازة", editable, minLines = 2)
    }
}

// ============================================================
// حقول نموذج الإنذار
// ============================================================
@Composable
private fun WarningFields(fields: MutableMap<String, String>, editable: Boolean) {
    MasarCard {
        Surface(color = Amber.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()) {
            Text("⚠️ هذا إنذار صادر بحقك من الإدارة. اقرأ بعناية ووقّع للإقرار بالاستلام.",
                modifier = Modifier.padding(12.dp), color = Amber,
                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle("مستوى الإنذار")
        Spacer(Modifier.height(6.dp))
        OptionsField("", "warningLevel", fields, false, listOf(   // editable=false دائماً للإنذار
            "first" to "إنذار أول",
            "second" to "إنذار ثاني",
            "final" to "إنذار نهائي"
        ))
        Spacer(Modifier.height(14.dp))
        SectionTitle("المخالفة")
        Spacer(Modifier.height(6.dp))
        TextField2(fields, "violation", "وصف المخالفة", false, minLines = 3)
        Spacer(Modifier.height(10.dp))
        SectionTitle("الجزاء (العقوبة)")
        Spacer(Modifier.height(6.dp))
        TextField2(fields, "penalty", "العقوبة المترتبة", false, minLines = 2)
        Spacer(Modifier.height(10.dp))
        SectionTitle("ملاحظات الموظف (اختياري)")
        Spacer(Modifier.height(6.dp))
        TextField2(fields, "employee_comments", "ملاحظاتك / دفاعك", editable, minLines = 3)
    }
}

// ============================================================
// حقول نموذج طلب السلفة
// ============================================================
@Composable
private fun AdvanceFields(fields: MutableMap<String, String>, editable: Boolean) {
    MasarCard {
        SectionTitle("💰 نوع السلفة")
        Spacer(Modifier.height(6.dp))
        OptionsField("", "advanceType", fields, editable, listOf(
            "advance" to "سلفة",
            "eos_advance" to "سلفة نهاية خدمة"
        ))
        Spacer(Modifier.height(14.dp))
        SectionTitle("بيانات السلفة")
        Spacer(Modifier.height(6.dp))
        TextField2(fields, "amount", "قيمة السلفة المطلوبة (﷼)", editable, KeyboardType.Number)
        TextField2(fields, "repayment_period", "فترة السداد (شهر)", editable, KeyboardType.Number)
        TextField2(fields, "reason", "سبب الطلب (اختياري)", editable, minLines = 2)
    }
}

// ============================================================
// مكوّنات مساعدة
// ============================================================
@Composable
private fun TextField2(
    fields: MutableMap<String, String>,
    key: String,
    label: String,
    editable: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    val value = fields[key] ?: ""
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = { if (editable) fields[key] = it },
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !editable,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        maxLines = if (minLines > 1) 5 else 1,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = TxtPrimary,
            focusedTextColor = TxtPrimary,
            disabledTextColor = TxtSoft,
        ),
    )
}

@Composable
private fun OptionsField(
    title: String,
    key: String,
    fields: MutableMap<String, String>,
    editable: Boolean,
    options: List<Pair<String, String>>,
) {
    if (title.isNotBlank()) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = TxtDim)
        Spacer(Modifier.height(6.dp))
    }
    val selected = fields[key] ?: ""
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()) {
        options.forEach { (k, label) ->
            val isSelected = selected == k
            Surface(
                modifier = Modifier.weight(1f).clickable(enabled = editable) {
                    if (editable) fields[key] = k
                },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) BrandRed.copy(alpha = 0.2f) else Ink2,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) BrandRed else LineDim
                ),
            ) {
                Text(label,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) BrandRed else TxtPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

// تحقق من الحقول المطلوبة
private fun validateFields(type: String, fields: Map<String, String>): String? {
    when (type) {
        "vacation", "leave" -> {
            if (fields["leaveType"].isNullOrBlank()) return "اختر نوع الإجازة"
            if (fields["start_date"].isNullOrBlank()) return "أدخل تاريخ بداية الإجازة"
            if (fields["end_date"].isNullOrBlank()) return "أدخل تاريخ نهاية الإجازة"
            if (fields["reason"].isNullOrBlank()) return "أدخل سبب الخروج"
        }
        "advance" -> {
            if (fields["advanceType"].isNullOrBlank()) return "اختر نوع السلفة"
            if (fields["amount"].isNullOrBlank()) return "أدخل قيمة السلفة"
            if (fields["repayment_period"].isNullOrBlank()) return "أدخل فترة السداد"
        }
        // warning: لا حقول مطلوبة من المندوب (فقط التوقيع)
    }
    return null
}

// تحويل قائمة الـ strokes إلى Base64 PNG
private fun strokesToBase64(strokes: List<List<Offset>>, width: Int, height: Int): String {
    if (width <= 0 || height <= 0 || strokes.isEmpty()) return ""

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 5f
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }

    strokes.forEach { stroke ->
        for (i in 1 until stroke.size) {
            canvas.drawLine(stroke[i - 1].x, stroke[i - 1].y, stroke[i].x, stroke[i].y, paint)
        }
    }

    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    val bytes = output.toByteArray()
    bitmap.recycle()
    return "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}
