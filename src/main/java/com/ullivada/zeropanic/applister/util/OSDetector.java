package com.ullivada.zeropanic.applister.util;

import java.util.Locale;

/**
 * Utility for detecting operating system.
 * Centralized OS detection to avoid code duplication.
 */
public final class OSDetector {
    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    
    private OSDetector() {
        // Utility class
    }
    
    /**
     * Check if running on Windows.
     */
    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }
    
    /**
     * Check if running on Linux.
     */
    public static boolean isLinux() {
        return OS_NAME.contains("nux") || OS_NAME.contains("nix");
    }
    
    /**
     * Check if running on macOS.
     */
    public static boolean isMac() {
        return OS_NAME.contains("mac") || OS_NAME.contains("darwin");
    }
}
