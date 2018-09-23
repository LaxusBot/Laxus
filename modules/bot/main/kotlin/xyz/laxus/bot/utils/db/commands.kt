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
package xyz.laxus.bot.utils.db

import net.dv8tion.jda.core.entities.Guild
import xyz.laxus.bot.command.Command
import xyz.laxus.bot.command.CommandLevel
import xyz.laxus.db.DBCommandLevels

fun Guild.getCommandLevel(command: Command): CommandLevel? {
    return DBCommandLevels.getCommandLevel(idLong, command.fullname)?.let { level ->
        CommandLevel.values().firstOrNull { it.name == level }
    }
}

fun Guild.setCommandLevel(command: Command, level: CommandLevel?) {
    if(level == null || level == command.defaultLevel) {
        return DBCommandLevels.setCommandLevel(idLong, command.fullname, null)
    }
    DBCommandLevels.setCommandLevel(idLong, command.fullname, level.name)
}
