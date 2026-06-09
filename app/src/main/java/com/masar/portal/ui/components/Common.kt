package com.masar.portal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.masar.portal.ui.theme.*

@Composable
fun MasarCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Ink2,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineDim),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** بطاقة ملوّنة بتدرّج */
@Composable
fun GradientCard(
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

/** بطاقة إحصائية صغيرة */
@Composable
fun StatCard(
    label: String,
    value: String,
    accent: Color = BrandRed,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Ink2,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineDim),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TxtDim)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TxtDim)
            }
        }
    }
}

/** شارة دائرية صغيرة (للفئة A/B/C/D) */
@Composable
fun CategoryBadge(level: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (level.uppercase()) {
        "A" -> Green to Color.White
        "B" -> Amber to Color.White
        "C" -> Color(0xFFE8745A) to Color.White
        "D" -> Red to Color.White
        else -> TxtDim to Color.White
    }
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(level.ifBlank { "-" }, color = fg, fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
    }
}

/** شريط تقدم أفقي */
@Composable
fun ProgressRow(
    label: String,
    current: Int,
    target: Int,
    accent: Color = BrandRed,
) {
    val ratio = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(targetValue = ratio, label = "progress")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TxtSoft, modifier = Modifier.weight(1f))
            Text("$current / $target", style = MaterialTheme.typography.titleSmall, color = TxtPrimary)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Ink4)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )
        }
    }
}

/** صف بيانات بسيط (مفتاح-قيمة) */
@Composable
fun InfoRow(label: String, value: String?, valueColor: Color = TxtPrimary) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TxtDim)
        Text(value ?: "—", style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}

/** عنوان قسم */
@Composable
fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(3.dp)
                .height(18.dp)
                .background(BrandRed, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = TxtPrimary, fontWeight = FontWeight.Bold)
    }
}
