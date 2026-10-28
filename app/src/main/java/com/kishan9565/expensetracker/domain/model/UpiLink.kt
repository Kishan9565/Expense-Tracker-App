package com.kishan9565.expensetracker.domain.model

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.kishan9565.expensetracker.core.money.Money
import com.kishan9565.expensetracker.core.money.Outcome
import java.math.BigDecimal

data class UpiPayRequest(val vpa: String, val name: String, val amountPaise: Long?, val note: String?)

object UpiLink {
    private val VPA = Regex("""^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z][a-zA-Z0-9]{1,63}$""")
    fun isValidVpa(v: String) = VPA.matches(v.trim())

    fun build(r: UpiPayRequest): Uri = Uri.Builder().scheme("upi").authority("pay")
        .appendQueryParameter("pa", r.vpa.trim())
        .appendQueryParameter("pn", r.name.trim())
        .apply {
            r.amountPaise?.let { appendQueryParameter("am", BigDecimal.valueOf(it, 2).toPlainString()) }
            r.note?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("tn", it.take(50)) }
        }
        .appendQueryParameter("cu", "INR")
        .build()

    /** Parses a scanned UPI QR ("upi://pay?pa=...&pn=...&am=..."). Returns null if invalid. */
    fun parse(raw: String): UpiPayRequest? {
        val uri = Uri.parse(raw.trim())
        if (!uri.scheme.equals("upi", true) || !uri.host.equals("pay", true)) return null
        val vpa = uri.getQueryParameter("pa")?.takeIf(::isValidVpa) ?: return null
        val amount = uri.getQueryParameter("am")?.let { (Money.parse(it) as? Outcome.Ok)?.value }
        return UpiPayRequest(vpa, uri.getQueryParameter("pn").orEmpty(), amount, uri.getQueryParameter("tn"))
    }
}

fun launchUpi(launcher: ActivityResultLauncher<Intent>, r: UpiPayRequest): Boolean {
    val view = Intent(Intent.ACTION_VIEW, UpiLink.build(r))
    return try { launcher.launch(Intent.createChooser(view, "Pay with")); true }
    catch (e: ActivityNotFoundException) { false }
}
