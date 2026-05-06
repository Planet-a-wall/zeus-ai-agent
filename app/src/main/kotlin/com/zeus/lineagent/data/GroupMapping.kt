package com.zeus.lineagent.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.UUID

@Serializable
data class GroupMapping(
    val id: String = UUID.randomUUID().toString(),
    /** LINE group ID, room ID, or user ID — the destination the bot pushes to. */
    val lineTargetId: String,
    /** Friendly label shown in the UI (e.g. "Project Athena - core team"). */
    val label: String,
    /** Asana project gid this LINE group is following up on. */
    val asanaProjectGid: String = "",
    /** Optional Asana section gid to filter tasks. */
    val asanaSectionGid: String = "",
    /** Free-form notes (e.g. cadence, owners). */
    val notes: String = "",
) {
    companion object {
        val LIST_SERIALIZER = ListSerializer(serializer())
    }
}
