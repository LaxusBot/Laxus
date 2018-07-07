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
import net.dv8tion.jda.core.entities.ISnowflake
import net.dv8tion.jda.core.entities.User
import xyz.laxus.db.DBExperimentAccess as DB
import xyz.laxus.db.entities.ExperimentAccess
import xyz.laxus.db.entities.ExperimentAccess.Type.*

var User.experimentAccessLevel: ExperimentAccess.Level?
    get() = DB.getExperimentAccess(idLong, USER)?.level
    set(value) = setAccessLevelOf(this, value)

var Guild.experimentAccessLevel: ExperimentAccess.Level?
    get() = DB.getExperimentAccess(idLong, GUILD)?.level
    set(value) = setAccessLevelOf(this, value)

private fun setAccessLevelOf(snowflake: ISnowflake, level: ExperimentAccess.Level?) {
    val type = when(snowflake) {
        is User -> ExperimentAccess.Type.USER
        is Guild -> ExperimentAccess.Type.GUILD
        else -> throw IllegalStateException("Invalid snowflake $snowflake")
    }

    if(level !== null) {
        DB.setExperimentAccess(ExperimentAccess(snowflake.idLong, level, type))
    } else {
        DB.removeExperimentAccess(snowflake.idLong, type)
    }
}
