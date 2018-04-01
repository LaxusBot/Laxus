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
package xyz.laxus.util.db

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import xyz.laxus.db.DBChannels
import xyz.laxus.db.DBChannels.Type.*

var Guild.modLog: TextChannel?
    get() = getRoleTypeOf(MOD_LOG)
    set(value) = setChannelTypeOf(value, MOD_LOG)

val Guild.hasModLog: Boolean get() {
    return modLog !== null
}

private fun Guild.getRoleTypeOf(type: DBChannels.Type): TextChannel? {
    return DBChannels.getChannel(idLong, type)?.let { getTextChannelById(it) }
}

private fun Guild.setChannelTypeOf(value: TextChannel?, type: DBChannels.Type) {
    if(value !== null) DBChannels.setChannel(idLong, value.idLong, type) else DBChannels.removeChannel(idLong, type)
}
