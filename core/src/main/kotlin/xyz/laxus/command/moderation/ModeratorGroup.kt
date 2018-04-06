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
package xyz.laxus.command.moderation

import com.typesafe.config.Config
import net.dv8tion.jda.core.JDABuilder
import xyz.laxus.command.Command
import xyz.laxus.entities.ModLog
import xyz.laxus.jda.util.listener

/**
 * @author Kaidan Gustave
 */
object ModeratorGroup : Command.Group("Moderation") {
    override val defaultLevel get() = Command.Level.MODERATOR
    override val guildOnly = true
    override val devOnly = false

    override fun JDABuilder.configure() {
        listener { ModLog }
    }

    override fun init(config: Config) {
        + BanCommand()
        + CleanCommand()
        + KickCommand()
        + MuteCommand()
        + ReasonCommand()
        + UnbanCommand()
        + UnmuteCommand()
    }
}