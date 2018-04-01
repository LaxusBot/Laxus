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
package xyz.laxus.command.music

import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.music.MusicManager
import xyz.laxus.util.niceName
import xyz.laxus.util.ignored

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class VolumeCommand(manager: MusicManager): MusicCommand(manager) {
    override val name = "Volume"
    override val arguments = "[Volume]"
    override val help = "Changes the bot's playing volume."
    override val defaultLevel = Level.MODERATOR

    override suspend fun execute(ctx: CommandContext) {
        if(!ctx.guild.isPlaying) return ctx.notPlaying()
        if(!ctx.member.inPlayingChannel) return ctx.notInPlayingChannel()

        val args = ctx.args
        val volume = ignored(VolumeLevel.fromArgs(args)?.volume) { args.toInt() }

        if(volume === null || volume !in 0..150) return ctx.replyError {
            "$args is not a valid volume measurement!"
        }

        val queue = checkNotNull(manager[ctx.guild]) {
            "Expected non-null guild queue for Guild (ID: ${ctx.guild.idLong})"
        }

        queue.volume = volume
        ctx.replySuccess("Set volume to `$volume`!")
    }

    private enum class VolumeLevel(val volume: Int) {
        LOW(25),
        MEDIUM(75),
        HIGH(125);

        override fun toString(): String = "$niceName ($volume)"

        companion object {
            fun fromArgs(args: String): VolumeLevel? = values().firstOrNull { it.name.equals(args, true) }
        }
    }
}
