package com.masar.portal.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LoginResponse(
    val ok: Boolean,
    val token: String? = null,
    val driver: LoginDriver? = null,
    val error: String? = null,
)

@Serializable
data class LoginDriver(
    val id: Int,
    val name: String,
    val courier_id: String,
    val national_id: String,
)

@Serializable
data class MeResponse(
    val ok: Boolean,
    val driver: DriverInfo? = null,
    val orders: OrdersSummary? = null,
    val last_day: LastDaySummary? = null,
    val perf: PerfInfo? = null,
    val sup: SupInfo? = null,
    // history و supervisor_messages كـ JsonElement للمرونة (قد تأتي Map أو List أو null)
    val history: JsonElement? = null,
    val supervisor_messages: JsonElement? = null,
    val salary: SalaryInfo? = null,
    val total_orders: Int? = null,
    val on_time_pct: Double? = null,
    val last_date: String? = null,
    val current_month: String? = null,           // الشهر المعروض (YYYY-MM)
    val upload_dates: UploadDates? = null,
    val verify: VerifyInfo? = null,              // 📷 معدّل التحقق Selfie
    val account_status: String? = null,          // active / on_leave / inactive
    val iforms_count: Int? = null,               // عدد النماذج التفاعلية المعلّقة
    val error: String? = null,
)

@Serializable
data class UploadDates(
    val orders_last: String? = null,
    val supervisor_last: String? = null,
    val performance_last: String? = null,
    val verification_last: String? = null,       // آخر رفع لملف التحقق
)

@Serializable
data class VerifyInfo(
    val triggered: Int? = null,                  // أيام التصوير المفروضة
    val passed: Int? = null,                     // أيام التصوير المُنجزة
    val rate: Double? = null,                    // النسبة (0.0 - 1.0)
)

@Serializable
data class LastDaySummary(
    val deliv: Int? = null,
    val acc: Int? = null,
    val rej: Int? = null,
    val late_total: Int? = null,
    val big: Int? = null,
    val hours: Double? = null,
    val day_date: String? = null,
)

@Serializable
data class DriverInfo(
    val id: Int? = null,
    val full_name: String? = null,
    val courier_id: String? = null,
    val national_id: String? = null,
    val phone: String? = null,
    val plate_letters: String? = null,
    val plate_numbers: String? = null,
    val bank_account: String? = null,
    val iban: String? = null,
    val status: String? = null,                  // active / on_leave / inactive
    val driver_photo: String? = null,
    val car_photo: String? = null,
    val iqama_photo: String? = null,
    val driver_card_expiry: String? = null,
    val car_op_expiry: String? = null,
    val work_card_expiry: String? = null,        // اسم بديل لـ car_op_expiry
    val iqama_expiry: String? = null,
    val iqama_next_expiry: String? = null,
    val car_auth_expiry: String? = null,
    val car_reg_expiry: String? = null,
)

@Serializable
data class OrdersSummary(
    val deliv: Int? = null,
    val acc: Int? = null,
    val rej: Int? = null,
    val late_total: Int? = null,
    val days_count: Int? = null,
    val first_d: String? = null,
    val last_d: String? = null,
)

@Serializable
data class PerfInfo(
    val level_cat: String? = null,
    val city_rank: Int? = null,
    val on_time_rate: Double? = null,
    val completion_rate: Double? = null,
    val volume: Int? = null,
)

@Serializable
data class SupInfo(
    val vda_days: Int? = null,
    val cancellations: Int? = null,
    val total_distance: Double? = null,
    val payable_distance: Double? = null,
    val on_time_tasks: Int? = null,
)

@Serializable
data class RequestItem(
    val id: Int,
    val type: String,
    val details: String? = null,
    val amount: Double? = null,
    val status: String,
    val review_note: String? = null,
    val created_at: String? = null,
    val reviewed_at: String? = null,
    val is_supervisor_action: Int? = null,
    val seen_by_driver_at: String? = null,
    val supervisor_name: String? = null,
    val payment_method: String? = null,
    val receipt_photo: String? = null,
)

@Serializable
data class SalaryInfo(
    val base: Double,
    val base_bonus: Double? = null,
    val base_total: Double? = null,
    val net: Double,
    val deductions: List<SalaryLine> = emptyList(),
    val bonuses: List<SalaryLine> = emptyList(),
    val effective_delivered: Int,
    val target: Int,
    val level: String? = null,
)

@Serializable
data class SalaryLine(
    val t: String,
    val a: Double,
)

@Serializable
data class BrandResponse(
    val ok: Boolean,
    val brand: BrandInfo? = null,
)

@Serializable
data class BrandInfo(
    val app_name: String = "مسار - المناديب",
    val short: String = "مسار",
    val logo: String = "assets/logo.png",
    val primary: String = "#f04d45",
    val track_enabled: Int = 0,
)

@Serializable
data class SimpleResponse(
    val ok: Boolean,
    val error: String? = null,
    val message: String? = null,
)

/* ============================================================
   النماذج التفاعلية الإلكترونية (Interactive Forms)
   ============================================================ */

@Serializable
data class IFormsListResponse(
    val ok: Boolean,
    val forms: List<IFormItem> = emptyList(),
    val error: String? = null,
)

@Serializable
data class IFormItem(
    val id: Int,
    val type: String,                           // leave / warning / advance
    val template_title: String? = null,
    val status: String,                          // pending / filled / approved / rejected
    val driver_signature: String? = null,
    val supervisor_sig: String? = null,
    val rejection_reason: String? = null,
    val created_at: String? = null,
    val driver_signed_at: String? = null,
    val reviewed_at: String? = null,
    val data_json: String? = null,              // البيانات بصيغة JSON
)

@Serializable
data class IFormDetailResponse(
    val ok: Boolean,
    val form: IFormItem? = null,
    val template_html: String? = null,          // HTML للقالب
    val error: String? = null,
)

/* ============================================================
   حالة الحساب: للتحقق من active/on_leave/inactive
   ============================================================ */

@Serializable
data class AccountStatusResponse(
    val ok: Boolean,
    val status: String? = null,                 // active / on_leave / inactive
    val message: String? = null,
    val error: String? = null,
)
