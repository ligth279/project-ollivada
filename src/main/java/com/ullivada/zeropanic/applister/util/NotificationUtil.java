package com.ullivada.zeropanic.applister.util;

import java.io.IOException;

/**
 * Simple notification utility for Linux desktop notifications.
 * Uses notify-send command available on most Linux systems.
 */
public class NotificationUtil {
    
    /**
     * Show a desktop notification for a threat alert.
     */
    public static void showThreatNotification(String appName, String severity) {
        try {
            // Check if notify-send is available
            if (!isNotifySendAvailable()) {
                return; // Silently skip if not available
            }
            
            String icon = getSeverityIcon(severity);
            String urgency = getUrgencyLevel(severity);
            String title = icon + " Security Alert";
            String message = "Threat detected in: " + appName + "\nSeverity: " + severity.toUpperCase();
            
            ProcessBuilder pb = new ProcessBuilder(
                "notify-send",
                "-u", urgency,
                "-i", "security-medium",
                "-a", "ZeroPanic",
                title,
                message
            );
            
            pb.start();
            
        } catch (Exception e) {
            // Fail silently - notifications are optional
            System.err.println("[Notification] Failed to show notification: " + e.getMessage());
        }
    }
    
    /**
     * Check if notify-send command is available.
     */
    private static boolean isNotifySendAvailable() {
        try {
            Process p = new ProcessBuilder("which", "notify-send").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static String getSeverityIcon(String severity) {
        if (severity == null) return "⚠️";
        return switch (severity.toLowerCase()) {
            case "critical" -> "🔴";
            case "high" -> "🟠";
            case "medium" -> "🟡";
            case "low" -> "🟢";
            default -> "⚠️";
        };
    }
    
    private static String getUrgencyLevel(String severity) {
        if (severity == null) return "normal";
        return switch (severity.toLowerCase()) {
            case "critical", "high" -> "critical";
            case "medium" -> "normal";
            case "low" -> "low";
            default -> "normal";
        };
    }
}
