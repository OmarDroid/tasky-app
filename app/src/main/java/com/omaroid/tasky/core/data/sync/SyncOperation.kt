package com.omaroid.tasky.core.data.sync

/** Which kind of agenda item a pending operation refers to. */
enum class SyncItemType { TASK, REMINDER, EVENT }

/** The intent recorded for a pending operation — never a frozen copy of the payload. */
enum class SyncIntent { CREATE, UPDATE, DELETE }
