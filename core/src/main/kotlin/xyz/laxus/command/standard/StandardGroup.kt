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
package xyz.laxus.command.standard

import com.typesafe.config.Config
import xyz.laxus.command.Command
import xyz.laxus.util.string

/**
 * @author Kaidan Gustave
 */
object StandardGroup : Command.Group("Standard") {
    override val defaultLevel get() = Command.Level.STANDARD
    override val guildOnly = false
    override val devOnly = false

    override fun init(config: Config) {
        + AboutCommand()
        + AFKCommand()
        + AvatarCommand()
        + ChannelCommand()
        + ColorMeCommand()
        + EmoteCommand()
        + GoogleCommand()
        + HelpCommand()
        + ImageCommand()
        + InfoCommand()
        + InviteCommand()
        + InviteInfoCommand()
        + PingCommand()
        + QuoteCommand()
        + ReminderCommand()
        + RoleMeCommand()
        + ServerCommand()
        + TagCommand()
        + TimeCommand()
        + YouTubeCommand(config.string("keys.youtube"))
    }
}