package com.omaroid.tasky.core.data.agenda.local.entities

import androidx.room.TypeConverter
import com.omaroid.tasky.core.data.sync.SyncIntent
import com.omaroid.tasky.core.data.sync.SyncItemType

/**
 * No-arg converters for the pending-operation enums. Unlike [EventConverters] (which needs an injected
 * [kotlinx.serialization.json.Json]), these have no dependencies, so letting Room instantiate them via
 * `@TypeConverters` on [PendingOperationEntity] is fine — the `@ProvidedTypeConverter`/`addTypeConverter`
 * rule only applies to converters with injected collaborators.
 */
class SyncEnumConverters {
    @TypeConverter fun itemTypeToString(v: SyncItemType) = v.name
    @TypeConverter fun stringToItemType(v: String) = SyncItemType.valueOf(v)
    @TypeConverter fun intentToString(v: SyncIntent) = v.name
    @TypeConverter fun stringToIntent(v: String) = SyncIntent.valueOf(v)
}
