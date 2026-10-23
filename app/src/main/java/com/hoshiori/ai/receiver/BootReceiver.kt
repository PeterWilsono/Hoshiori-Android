package com.hoshiori.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hoshiori.ai.HoshioriApp
import com.hoshiori.ai.service.ProactiveMessageWorker

/**
 * 开机启动接收器 - 重新调度主动消息服务
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 从设置中读取间隔时间并重新调度主动消息服务
            val dataManager = HoshioriApp.instance.dataManager
            val settings = dataManager.getSettings()
            if (settings.proactiveMessageEnabled) {
                ProactiveMessageWorker.schedule(context, settings.proactiveMessageInterval)
            }
        }
    }
}
