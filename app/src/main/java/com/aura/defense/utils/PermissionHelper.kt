package com.aura.defense.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionHelper(private val context: Context) {
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getMissingPermissions(permissions: List<String>): List<String> {
        return permissions.filterNot(::hasPermission)
    }

    fun hasAllPermissions(permissions: List<String>): Boolean {
        return permissions.all(::hasPermission)
    }
}