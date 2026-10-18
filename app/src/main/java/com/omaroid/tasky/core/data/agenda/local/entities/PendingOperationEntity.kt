package com.omaroid.tasky.core.data.agenda.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.omaroid.tasky.core.data.sync.SyncIntent
import com.omaroid.tasky.core.data.sync.SyncItemType

/**
 * One row per item ([id] = "$itemType:$itemId"), recording only the *intent* the server hasn't heard
 * about yet — never a frozen DTO snapshot. The [SyncWorker] re-reads the item's current body from its
 * DAO at sync time, so the latest offline edit always wins and the queue can't grow unbounded.
 */
@Entity(tableName = "pending_operations")
@TypeConverters(SyncEnumConverters::class)
data class PendingOperationEntity(
    @PrimaryKey val id: String,          // "$itemType:$itemId" — one row per item
    val itemType: SyncItemType,
    val itemId: String,
    val intent: SyncIntent,
    val payloadJson: String? = null,     // only EVENT uses this (EventSyncPayload); null = no payload
    val createdAt: Long,                 // first-enqueue time; drives replay order
)
