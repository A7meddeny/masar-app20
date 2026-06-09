package com.masar.portal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.masar.portal.R
import com.masar.portal.model.LoginDriver
import com.masar.portal.network.MasarApi
import com.masar.portal.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    baseUrl: String,
    onLogin: (token: String, driver: LoginDriver) -> Unit,
    onChangeSite: () -> Unit,
) {
    var nid by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(60.dp))
            Image(
                painter = painterResource(R.drawable.logo_splash),
                contentDescription = null,
                modifier = Modifier.size(90.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text("تسجيل الدخول",
                style = MaterialTheme.typography.headlineMedium,
                color = TxtPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "أدخل بياناتك المسجّلة لدى الإدارة",
                style = MaterialTheme.typography.bodySmall,
                color = TxtDim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = nid,
                onValueChange = { nid = it.filter { c -> c.isDigit() }; error = null },
                label = { Text("رقم الإقامة") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = pwd,
                onValueChange = { pwd = it; error = null },
                label = { Text("كلمة المرور") },
                singleLine = true,
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPwd = !showPwd }) {
                        Icon(
                            imageVector = if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = Red, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    if (nid.isBlank() || pwd.isBlank()) {
                        error = "أدخل رقم الإقامة وكلمة المرور"
                        return@Button
                    }
                    loading = true; error = null
                    scope.launch {
                        val res = MasarApi(baseUrl).login(nid.trim(), pwd)
                        loading = false
                        if (res.ok && res.token != null && res.driver != null) {
                            onLogin(res.token, res.driver)
                        } else {
                            error = res.error ?: "فشل تسجيل الدخول"
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("دخول", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onChangeSite) {
                Text("تغيير الموقع", color = TxtDim, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
