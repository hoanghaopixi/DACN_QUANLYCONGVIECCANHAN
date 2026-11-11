package com.hhpc.smarttask.data.model

data class Workspace(
    val id: Long? = null,
    val userId: Long? = null,
    val name: String,
    val iconName: String? = "User",
    val isDefault: Boolean? = false
)

data class Category(
    val id: Long? = null,
    val userId: Long? = null,
    val workspaceId: Long? = null,
    val name: String,
    val colorCode: String? = "#8B5CF6",
    val iconName: String? = "Briefcase",
    val isDefault: Boolean? = false
)
