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
package xyz.laxus.bot.command.standard

import xyz.laxus.bot.command.*
import java.time.temporal.ChronoUnit.*

class PingCommand: Command(StandardGroup) {
    override val name = "Ping"
    override val aliases = arrayOf("Pong", "Pang", "Pung", "Peng", "Pyng", "Png")
    override val help = "Gets the bot's REST latency."
    override val guildOnly = false
    override val hasAdjustableLevel = false
    override val children = arrayOf(PingWebSocketCommand())

    override suspend fun execute(ctx: CommandContext) {
        // Send "Ping...", suspend until RestAction completes
        val message = ctx.send("Ping...")
        // Get the creation time of the calling message,
        //measure the time between it and the calling message
        val ping = ctx.message.creationTime.until(message.creationTime, MILLIS)
        // Edit the message
        message.editMessage("Ping: ${ping}ms").queue()
    }

    inner class PingWebSocketCommand: Command(this@PingCommand) {
        override val name = "WebSocket"
        override val aliases = arrayOf("WS", "Gateway")
        override val help = "Gets the bot's Gateway latency."
        override val guildOnly = false

        override suspend fun execute(ctx: CommandContext) {
            ctx.reply("Ping: ${ctx.jda.ping}ms")
        }
    }
}
