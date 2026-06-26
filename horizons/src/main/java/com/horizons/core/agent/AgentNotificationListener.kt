package com.horizons.core.agent

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NotificationListenerService — grants read access to the active notification shade.
 * Must be enabled by the user in Settings → Notification access → Horizons.
 *
 * The AgentLoop calls [activeNotifications] to read them; the NLS posts updates here.
 */
class AgentNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        _active.value = try { activeNotifications.toList() } catch (_: Exception) { emptyList() }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        _active.value = try { activeNotifications.toList() } catch (_: Exception) { emptyList() }
    }

    override fun onListenerConnected() {
        _active.value = try { activeNotifications.toList() } catch (_: Exception) { emptyList() }
        instance = this
    }

    override fun onListenerDisconnected() {
        instance = null
    }

    companion object {
        private val _active = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val active: StateFlow<List<StatusBarNotification>> = _active.asStateFlow()

        @Volatile var instance: AgentNotificationListener? = null
    }
}
