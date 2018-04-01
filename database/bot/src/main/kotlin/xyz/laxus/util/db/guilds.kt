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
import xyz.laxus.db.DBGuilds
import xyz.laxus.db.DBGuilds.Type.*

var Guild.isMusic: Boolean
    get() = DBGuilds.isGuild(idLong, MUSIC)
    set(value) = setGuildTypeOf(value, MUSIC)

private fun Guild.setGuildTypeOf(value: Boolean, type: DBGuilds.Type) {
    if(value) DBGuilds.addGuild(idLong, type) else DBGuilds.removeGuild(idLong, type)
}
