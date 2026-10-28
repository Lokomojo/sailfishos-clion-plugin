package io.github.lokomojo.sailfishclion

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

object SailfishNotifications {

    const val GROUP = "Sailfish OS"

    fun error(project: Project, message: String, action: NotificationAction? = null) {
        ApplicationManager.getApplication().invokeLater {
            val notification = NotificationGroupManager.getInstance().getNotificationGroup(GROUP).createNotification(GROUP, message, NotificationType.ERROR)
            if (action != null) {
                notification.addAction(action)
            }
            notification.notify(project)
        }
    }
}
