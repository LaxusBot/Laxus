/*
 * Copyright 2018 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused")
package xyz.laxus.db.entities

import xyz.laxus.db.sql.SQLTimestamp
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.Temporal

data class Reminder(val userId: Long, val remindTime: SQLTimestamp, val message: String, internal val id: Long = -1): Comparable<Reminder> {
    init {
        require(message.length <= MaxMessageLength) { "Message with length > $MaxMessageLength" }
    }

    constructor(userId: Long, remindTime: Temporal, message: String): this(
        userId = userId,
        message = message,
        remindTime = SQLTimestamp.valueOf(
            remindTime as? LocalDateTime ?:
            (remindTime as? OffsetDateTime)?.toLocalDateTime() ?:
            LocalDateTime.ofInstant(remindTime as? Instant ?: Instant.from(remindTime), ZoneOffset.UTC)
        )
    )

    internal constructor(results: ResultSet): this(
        userId = results.getLong("user_id"),
        message = results.getString("message"),
        remindTime = results.getTimestamp("remind_time"),
        id = results.getLong("id")
    )

    override fun compareTo(other: Reminder): Int = id.compareTo(other.id)

    companion object {
        const val MaxMessageLength = 500
    }
}