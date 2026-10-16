package com.saaf.milkshop.statements

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

internal enum class ReminderChannel { WhatsApp,SMS }
internal object ReminderIntents {
    fun phone(raw: String): String {
        require(raw.length<=80) {"Use one full international number, starting with + and the country code."}
        val normalized=raw.trim().filterNot {it in " ()-"}
        require(Regex("\\+[1-9][0-9]{6,14}").matches(normalized)) {"Use one full international number, starting with + and the country code."}
        return normalized
    }
    fun intent(channel: ReminderChannel,recipient: String,message: String,whatsAppPackage: String="com.whatsapp"): Intent {
        val number=phone(recipient)
        require(message.isNotBlank()) {"No reminder is needed for this balance."}
        return when(channel) {
            ReminderChannel.SMS -> Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",number,null)).putExtra("sms_body",message)
            ReminderChannel.WhatsApp -> {
                require(whatsAppPackage in PACKAGES)
                Intent(Intent.ACTION_VIEW,Uri.Builder().scheme("https").authority("wa.me").appendPath(number.drop(1))
                    .appendQueryParameter("text",message).build()).setPackage(whatsAppPackage)
            }
        }
    }
    fun open(context: Context,channel: ReminderChannel,recipient: String,message: String) {
        if(channel==ReminderChannel.SMS) {context.startActivity(intent(channel,recipient,message));return}
        for(pkg in PACKAGES) {
            val request=intent(channel,recipient,message,pkg)
            if(request.resolveActivity(context.packageManager)!=null) {
                try {context.startActivity(request);return} catch(missing: ActivityNotFoundException) { /* Try the other installed WhatsApp edition. */ }
            }
        }
        throw ActivityNotFoundException("WhatsApp is unavailable. Choose SMS or enable WhatsApp, then try again.")
    }
    private val PACKAGES=listOf("com.whatsapp","com.whatsapp.w4b")
}
