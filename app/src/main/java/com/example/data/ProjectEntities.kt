package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val updatedAt: Long,
    val durationMs: Long,
    val clipCount: Int,
    val thumbnailUri: String?,
    val payloadJson: String
)

@Entity(tableName = "exports")
data class ExportEntity(
    @PrimaryKey val id: String,
    val projectTitle: String,
    val filePath: String,
    val mediaStoreUri: String?,
    val durationMs: Long,
    val resolutionLabel: String,
    val fileSizeBytes: Long,
    val createdAt: Long
)
