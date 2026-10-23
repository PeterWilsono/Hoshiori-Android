package com.hoshiori.ai.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hoshiori.ai.service.DailyDiaryWorker
import java.util.Calendar

/**
 * 每日日记闹钟接收器
 * 使用AlarmManager确保23:00准时执行
 */
class DailyDiaryAlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d(TAG, "📔 收到日记闹钟触发")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // 设备重启后重新调度
                android.util.Log.d(TAG, "📔 设备重启，重新调度日记闹钟")
                scheduleDailyDiaryAlarm(context)
            }
            ACTION_DAILY_DIARY -> {
                // 触发日记生成
                android.util.Log.d(TAG, "📔 开始生成日记")
                DailyDiaryWorker.triggerNow(context)
                
                // 重新调度下一次（明天23:00）
                scheduleDailyDiaryAlarm(context)
            }
        }
    }
    
    companion object {
        private const val TAG = "DailyDiaryAlarm"
        private const val ACTION_DAILY_DIARY = "com.hoshiori.ai.ACTION_DAILY_DIARY"
        private const val DIARY_REQUEST_CODE = 1001
        
        /**
         * 调度每日日记闹钟
         * 每天23:00执行
         */
        fun scheduleDailyDiaryAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, DailyDiaryAlarmReceiver::class.java).apply {
                action = ACTION_DAILY_DIARY
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                DIARY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // 计算下一个23:00
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // 如果今天的23:00已经过了，设置为明天的23:00
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }
            
            android.util.Log.d(TAG, "📔 日记闹钟已设置: ${calendar.time}")
            
            // ⭐ 使用setExactAndAllowWhileIdle确保在Doze模式下也能执行
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                android.util.Log.d(TAG, "✅ 日记闹钟设置成功")
            } catch (e: SecurityException) {
                android.util.Log.e(TAG, "❌ 设置闹钟失败，需要SCHEDULE_EXACT_ALARM权限", e)
            }
        }
        
        /**
         * 取消日记闹钟
         */
        fun cancelDailyDiaryAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, DailyDiaryAlarmReceiver::class.java).apply {
                action = ACTION_DAILY_DIARY
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                DIARY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            android.util.Log.d(TAG, "📔 日记闹钟已取消")
        }
    }
}
