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
package xyz.laxus.bot.requests.dbots

import kotlinx.serialization.*
import net.dv8tion.jda.core.JDA

@Serializable
data class DiscordBotsStats(
    @Optional @SerialName("shard_id") val shardId: Int? = null,
    @Optional @SerialName("shard_total") val shardTotal: Int? = null,
    @SerialName("server_count") val serverCount: Long
) {
    constructor(shardInfo: JDA.ShardInfo?, serverCount: Long):
        this(shardInfo?.shardId, shardInfo?.shardTotal, serverCount)

    @Serializer(forClass = DiscordBotsStats::class)
    companion object {
        override fun serialize(output: Encoder, obj: DiscordBotsStats) {
            require(output is CompositeEncoder) { "Encoder must be a CompositeEncoder!" }
            output.beginStructure(descriptor)

            obj.shardId?.let { shardId ->
                val shardIdIndex = descriptor.getElementIndex("shard_id")
                output.encodeNullableSerializableElement(descriptor, shardIdIndex, Int.serializer(), shardId)
            }

            obj.shardTotal?.let { shardTotal ->
                val shardTotalIndex = descriptor.getElementIndex("shard_total")
                output.encodeNullableSerializableElement(descriptor, shardTotalIndex, Int.serializer(), shardTotal)
            }

            val serverCountIndex = descriptor.getElementIndex("server_count")
            output.encodeLongElement(descriptor, serverCountIndex, obj.serverCount)

            output.endStructure(descriptor)
        }
    }

    @Serializable
    data class Info(val stats: List<DiscordBotsStats>)
}
