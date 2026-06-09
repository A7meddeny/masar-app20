package com.masar.portal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.masar.portal.R
import com.masar.portal.network.MasarApi
import com.masar.portal.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onConfigured: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(32.dp)
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_splash),
                contentDescription = null,
                modifier = Modifier.size(110.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "أهلاً بك في تطبيق مسار",
                style = MaterialTheme.typography.headlineMedium,
                color = TxtPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "أدخل رابط موقع شركتك للبدء",
                style = MaterialTheme.typography.bodyMedium,
                color = TxtDim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; error = null },
                label = { Text("الرابط (مثال: vr2.umniate.com)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
            )
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error!!, color = Red, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    var url = input.trim().removeSuffix("/")
                    // التحقق هل المستخدم وضع http(s) صراحة؟
                    val hadScheme = url.startsWith("http://", ignoreCase = true)
                        || url.startsWith("https://", ignoreCase = true)
                    val rawHost = url
                        .removePrefix("http://").removePrefix("https://")
                        .removePrefix("www.")
                    if (rawHost.isEmpty() || !rawHost.contains(".")) {
                        error = "الرابط غير صحيح"; return@Button
                    }
                    loading = true; error = null
                    scope.launch {
                        // جرّب https أولاً، ثم http كاحتياط (لأن بعض المواقع تكون بـ http فقط)
                        val candidates = if (hadScheme) listOf(url) else listOf("https://$rawHost", "http://$rawHost")
                        var success: String? = null
                        var lastErr = ""
                        for (cand in candidates) {
                            val (ok, err) = MasarApi(cand).pingWithError()
                            if (ok) { success = cand; break }
                            lastErr = err
                        }
                        loading = false
                        if (success != null) onConfigured(success!!)
                        else error = if (lastErr.isNotBlank()) "تعذّر الاتصال: $lastErr" else "تعذّر الاتصال بالموقع. تحقق من الرابط واتصال الإنترنت."
                    }
                },
                enabled = !loading && input.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("متابعة", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
