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

import net.dv8tion.jda.core.entities.Message
import xyz.laxus.command.Experiment
import xyz.laxus.util.formattedName
import xyz.laxus.util.readableFormat

/**
 * @author Kaidan Gustave
 */
@Experiment
class LogCommand {
    companion object {
        fun formatMessageForLogFile(message: Message): String = buildString {
            append("[${message.creationTime.readableFormat}] ${message.author.formattedName()}:")
            appendln(message.contentStripped)
        }
    }

    // TODO
}