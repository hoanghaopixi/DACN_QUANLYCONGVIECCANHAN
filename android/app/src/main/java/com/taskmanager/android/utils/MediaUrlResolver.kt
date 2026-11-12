package com.taskmanager.android.utils

import java.io.File

/**
 * Utility to resolve relative media URLs from PostgreSQL DB
 * (e.g., "/api/uploads/avatars/xyz.jpg" or "/uploads/backgrounds/bg.png")
 * to absolute URLs accessible by the Android Emulator ("http://10.0.2.2:8085/api/uploads/...").
 */
object MediaUrlResolver {

    private const val EMULATOR_HOST = "http://10.0.2.2:8085"

    fun resolve(pathOrUrl: String?): Any? {
        if (pathOrUrl.isNullOrBlank()) return null
        val trimmed = pathOrUrl.trim()

        return when {
            trimmed.startsWith("http://", ignoreCase = true) || 
            trimmed.startsWith("https://", ignoreCase = true) || 
            trimmed.startsWith("data:image", ignoreCase = true) -> trimmed
            trimmed.startsWith("/data/") || trimmed.startsWith("/storage/") -> File(trimmed)
            trimmed.startsWith("/api/") -> "$EMULATOR_HOST$trimmed"
            trimmed.startsWith("/uploads/") -> "$EMULATOR_HOST/api$trimmed"
            trimmed.startsWith("uploads/") -> "$EMULATOR_HOST/api/$trimmed"
            trimmed.startsWith("/") -> "$EMULATOR_HOST/api$trimmed"
            else -> "$EMULATOR_HOST/api/$trimmed"
        }
    }
}
