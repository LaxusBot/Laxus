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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.laxus.command.music

import net.dv8tion.jda.core.JDABuilder
import xyz.laxus.command.Command
import xyz.laxus.jda.util.listener
import xyz.laxus.music.MusicManager

/**
 * @author Kaidan Gustave
 */
object MusicGroup : Command.Group("Music") {
    override val defaultLevel get() = Command.Level.STANDARD
    override val devOnly = false
    override val guildOnly = true

    val Manager by lazy { MusicManager() }

    override fun JDABuilder.configure() {
        listener { Manager }
    }

    override fun init() {
        + PlayCommand(Manager)
        + StopCommand(Manager)
    }
}