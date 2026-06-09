package com.masar.portal.network

import com.masar.portal.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * عميل HTTP بسيط بدون مكتبات خارجية ثقيلة.
 * يتعامل مع API الموجود في موقع مسار.
 */
class MasarApi(private val baseUrl: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        coerceInputValues = true
    }

    /** فحص أن الرابط يستجيب */
    suspend fun ping(): Boolean = pingWithError().first

    /** فحص + إرجاع سبب الفشل */
    suspend fun pingWithError(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        // جرّب endpoint API الصحيح
        try {
            val conn = (URL("$baseUrl/api/portal.php?action=brand").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..399) return@withContext (true to "")
            return@withContext (false to "كود $code من الخادم")
        } catch (e: Exception) {
            // جرّب portal.php الصفحة العادية كـ fallback
            try {
                val conn = (URL("$baseUrl/portal.php").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                }
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..399) return@withContext (true to "")
                return@withContext (false to "كود $code")
            } catch (e2: Exception) {
                return@withContext (false to (e.message ?: e.javaClass.simpleName))
            }
        }
    }

    /** جلب إعدادات العلامة (الشعار، الاسم) */
    suspend fun fetchBrand(): BrandInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val res = httpGet("$baseUrl/api/portal.php?action=brand")
            json.decodeFromString<BrandResponse>(res).brand
        }.getOrNull()
    }

    /** تسجيل دخول المندوب */
    suspend fun login(nid: String, password: String): LoginResponse = withContext(Dispatchers.IO) {
        try {
            val body = """{"national_id":${esc(nid)},"password":${esc(password)}}"""
            val res = httpPost("$baseUrl/api/portal.php?action=app_login", body)
            json.decodeFromString<LoginResponse>(res)
        } catch (e: Exception) {
            LoginResponse(ok = false, error = "تعذّر الاتصال: ${e.message ?: ""}")
        }
    }

    /** جلب كل بيانات المندوب */
    suspend fun fetchMe(nid: String, token: String): MeResponse = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/portal.php?action=app_me&nid=${enc(nid)}&token=${enc(token)}"
            val res = httpGet(url)
            json.decodeFromString<MeResponse>(res)
        } catch (e: Exception) {
            MeResponse(ok = false, error = "تعذّر الاتصال: ${e.message ?: ""}")
        }
    }

    /** إرسال طلب جديد (شكوى/سلفة/إجازة/حادث/صيانة) — مع التوكن */
    suspend fun submitRequest(
        nid: String,
        token: String,
        type: String,
        details: String,
        amount: Double? = null,
        odometer: Int? = null,
    ): SimpleResponse = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder("{")
            sb.append("\"national_id\":${esc(nid)},")
            sb.append("\"token\":${esc(token)},")
            sb.append("\"type\":${esc(type)},")
            sb.append("\"details\":${esc(details)}")
            if (amount != null)   sb.append(",\"amount\":$amount")
            if (odometer != null) sb.append(",\"odometer\":$odometer")
            sb.append("}")
            val res = httpPost("$baseUrl/api/portal.php?action=app_submit", sb.toString())
            json.decodeFromString<SimpleResponse>(res)
        } catch (e: Exception) {
            SimpleResponse(ok = false, error = "تعذّر الإرسال: ${e.message ?: ""}")
        }
    }

    /** إرسال طلب مع صورة (multipart) */
    suspend fun submitRequestWithPhoto(
        nid: String,
        token: String,
        type: String,
        details: String,
        amount: Double? = null,
        odometer: Int? = null,
        photoBytes: ByteArray? = null,
        photoFileName: String = "photo.jpg",
    ): SimpleResponse = withContext(Dispatchers.IO) {
        if (photoBytes == null) {
            // لا توجد صورة → استخدم JSON العادي
            return@withContext submitRequest(nid, token, type, details, amount, odometer)
        }
        try {
            val boundary = "----MasarBoundary" + System.currentTimeMillis()
            val conn = (URL("$baseUrl/api/portal.php?action=app_submit").openConnection() as HttpURLConnection).apply {
                connectTimeout = 30000
                readTimeout = 30000
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
            }

            conn.outputStream.use { out ->
                val w = out.bufferedWriter(Charsets.UTF_8)
                fun field(name: String, value: String) {
                    w.write("--$boundary\r\n")
                    w.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    w.write(value)
                    w.write("\r\n")
                }
                field("national_id", nid)
                field("token", token)
                field("type", type)
                field("details", details)
                if (amount != null) field("amount", amount.toString())
                if (odometer != null) field("odometer", odometer.toString())
                // الصورة
                w.write("--$boundary\r\n")
                w.write("Content-Disposition: form-data; name=\"photo\"; filename=\"$photoFileName\"\r\n")
                w.write("Content-Type: image/jpeg\r\n\r\n")
                w.flush()
                out.write(photoBytes)
                out.flush()
                w.write("\r\n--$boundary--\r\n")
                w.flush()
            }

            val res = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: throw e
            }
            conn.disconnect()
            json.decodeFromString<SimpleResponse>(res)
        } catch (e: Exception) {
            SimpleResponse(ok = false, error = "تعذّر الإرسال: ${e.message ?: ""}")
        }
    }

    /** تعليم رسالة من المشرف كمقروءة */
    suspend fun markSupervisorMessageSeen(nid: String, token: String, id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = """{"national_id":${esc(nid)},"token":${esc(token)},"id":$id}"""
            val res = httpPost("$baseUrl/api/portal.php?action=app_mark_seen", body)
            json.decodeFromString<SimpleResponse>(res).ok
        } catch (e: Exception) { false }
    }

    /* ============================================================
       النماذج التفاعلية الإلكترونية
       ============================================================ */

    /** جلب قائمة النماذج التفاعلية المرسلة للمندوب */
    suspend fun fetchIForms(nid: String, token: String): IFormsListResponse = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/portal.php?action=app_iforms_list&nid=${enc(nid)}&token=${enc(token)}"
            val res = httpGet(url)
            json.decodeFromString<IFormsListResponse>(res)
        } catch (e: Exception) {
            IFormsListResponse(ok = false, error = "تعذّر الاتصال: ${e.message ?: ""}")
        }
    }

    /** جلب تفاصيل نموذج تفاعلي محدد */
    suspend fun fetchIFormDetail(nid: String, token: String, formId: Int): IFormDetailResponse = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/portal.php?action=app_iform_detail&nid=${enc(nid)}&token=${enc(token)}&id=$formId"
            val res = httpGet(url)
            json.decodeFromString<IFormDetailResponse>(res)
        } catch (e: Exception) {
            IFormDetailResponse(ok = false, error = "تعذّر الاتصال: ${e.message ?: ""}")
        }
    }

    /** توقيع نموذج تفاعلي وإرساله للمشرف */
    suspend fun signIForm(
        nid: String, token: String, formId: Int,
        signatureBase64: String,
        action: String = "sign",  // sign | reject
        formData: Map<String, String> = emptyMap()
    ): SimpleResponse = withContext(Dispatchers.IO) {
        try {
            // ابنِ data JSON من الحقول
            val dataJson = if (formData.isNotEmpty()) {
                val pairs = formData.entries.joinToString(",") { (k, v) ->
                    "${esc(k)}:${esc(v)}"
                }
                "{$pairs}"
            } else "{}"
            val body = buildString {
                append("{")
                append("\"national_id\":${esc(nid)},")
                append("\"token\":${esc(token)},")
                append("\"id\":$formId,")
                append("\"action\":${esc(action)},")
                append("\"signature\":${esc(signatureBase64)},")
                append("\"data\":$dataJson")
                append("}")
            }
            val res = httpPost("$baseUrl/api/portal.php?action=app_iform_submit", body)
            json.decodeFromString<SimpleResponse>(res)
        } catch (e: Exception) {
            SimpleResponse(ok = false, error = "تعذّر الإرسال: ${e.message ?: ""}")
        }
    }

    /** فحص حالة الحساب (active/on_leave/inactive) */
    suspend fun checkAccountStatus(nid: String, token: String): AccountStatusResponse = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/portal.php?action=app_status&nid=${enc(nid)}&token=${enc(token)}"
            val res = httpGet(url)
            json.decodeFromString<AccountStatusResponse>(res)
        } catch (e: Exception) {
            AccountStatusResponse(ok = false, error = e.message)
        }
    }

    // ============ Helpers ============

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }

    private fun httpPost(url: String, body: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
        val text = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            // اقرأ الخطأ من ErrorStream لاستخراج JSON
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: throw e
        }
        conn.disconnect()
        return text
    }

    private fun esc(v: String): String = json.encodeToString(String.serializer(), v)
    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")
}
