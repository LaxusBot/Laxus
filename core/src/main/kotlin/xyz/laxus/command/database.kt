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
package xyz.laxus.command

import net.dv8tion.jda.core.entities.Guild
import xyz.laxus.db.DBCommandLevels

// TODO Reallocate this file when merging :database:bot module with :core module

fun Guild.getCommandLevel(command: Command): Command.Level? {
    return DBCommandLevels.getCommandLevel(idLong, command.fullname)?.let { level ->
        Command.Level.values().firstOrNull { it.name == level }
    }
}

fun Guild.setCommandLevel(command: Command, level: Command.Level?) {
    if(level == null || level == command.defaultLevel) {
        return DBCommandLevels.setCommandLevel(idLong, command.fullname, null)
    }
    DBCommandLevels.setCommandLevel(idLong, command.fullname, level.name)
}
