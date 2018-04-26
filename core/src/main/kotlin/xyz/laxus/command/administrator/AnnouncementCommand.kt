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
package xyz.laxus.command.administrator

import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify a role to announce to, and a message.")
class AnnouncementCommand: Command(AdministratorGroup) {
    override val name = "Announcement"
    override val aliases = arrayOf("Announce")
    override val arguments = "[Role] [Message]"
    override val help = "Mentions a role with a message in the server's announcements channel."

    override suspend fun execute(ctx: CommandContext) {
        // parse args
    }
}