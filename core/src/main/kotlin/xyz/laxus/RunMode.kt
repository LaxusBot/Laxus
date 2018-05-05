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
package xyz.laxus

import ch.qos.logback.classic.Level
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

/**
 * @author Kaidan Gustave
 */
enum class RunMode(val level: Level): Bot.CallVerifier {
    SERVICE(Level.INFO),
    IDLE(Level.ERROR) {
        override fun checkCall(event: MessageReceivedEvent, bot: Bot, name: String, args: String): Boolean {
            return Laxus.DevId == event.author.idLong
        }
    },
    DEBUG(Level.DEBUG),
    TEST(Level.DEBUG) {
        override fun checkCall(event: MessageReceivedEvent, bot: Bot, name: String, args: String): Boolean {
            return Laxus.DevId == event.author.idLong // TODO
        }
    };
}