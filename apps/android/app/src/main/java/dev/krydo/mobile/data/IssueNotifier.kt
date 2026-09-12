package dev.krydo.mobile.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.krydo.mobile.MainActivity

class IssueNotifier(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "krydo_credentials"
        private const val NOTIF_ID = 4201
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Credential updates",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "When an issuer issues a credential to you"
        }
        mgr.createNotificationChannel(channel)
    }

    fun notifyIssued(title: String, body: String) {
        ensureChannel()
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        }
    }
}
