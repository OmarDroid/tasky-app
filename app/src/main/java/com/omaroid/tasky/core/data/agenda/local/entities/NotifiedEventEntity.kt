package com.omaroid.tasky.core.data.agenda.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records event ids we've already shown an invite notification for, so each invite notifies exactly
 * once. Deliberately a **separate** table (not a column on [EventEntity] and no foreign key) so it
 * survives a clear of the events cache — e.g. logout/login — and is therefore not re-flooded.
 * [com.omaroid.tasky.core.data.agenda.AgendaSyncRepositoryImpl] prunes it to the current agenda on
 * every sync, so a re-invite notifies again and the table never grows without bound.
 */
@Entity(tableName = "notified_event_ids")
data class NotifiedEventEntity(
    @PrimaryKey val eventId: String,
)
