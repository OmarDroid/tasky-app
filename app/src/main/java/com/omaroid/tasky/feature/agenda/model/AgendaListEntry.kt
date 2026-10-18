package com.omaroid.tasky.feature.agenda.model

import kotlinx.datetime.LocalTime

sealed interface AgendaListEntry {
    data class Item(val item: AgendaItemUi) : AgendaListEntry
    data class Needle(val time: LocalTime) : AgendaListEntry
}

fun buildAgendaListEntries(
    items: List<AgendaItemUi>,
    needleTime: LocalTime?,
): List<AgendaListEntry> = buildList {
    val sorted = items.sortedBy { it.startTime }
    var inserted = needleTime == null
    for (item in sorted) {
        if (!inserted && item.startTime > needleTime!!) {
            add(AgendaListEntry.Needle(needleTime))
            inserted = true
        }
        add(AgendaListEntry.Item(item))
    }
    if (!inserted && needleTime != null) add(AgendaListEntry.Needle(needleTime))
}
