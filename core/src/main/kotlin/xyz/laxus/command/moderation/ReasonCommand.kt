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

import net.dv8tion.jda.core.exceptions.ErrorResponseException
import net.dv8tion.jda.core.requests.ErrorResponse
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.db.entities.Case
import xyz.laxus.entities.ModLog
import xyz.laxus.jda.util.await
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.casesWithoutReason
import xyz.laxus.util.db.getCase
import xyz.laxus.util.db.modLog
import xyz.laxus.util.doesNotMatch

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Provide a reason to give or specify a case number followed by a reason.")
class ReasonCommand: Command(ModeratorGroup) {
    private companion object {
        private val CASE_NUMBER_REGEX = Regex("\\d{1,5}")
    }

    override val name = "Reason"
    override val arguments = "<Case Number> [Reason]"
    override val help = "Updates a reason for a case in the server's moderation log."

    override suspend fun execute(ctx: CommandContext) {
        val modLog = ctx.guild.modLog ?: return ctx.replyError {
            "The moderation log for this server has not been set."
        }

        // We cannot talk, so we cannot update
        if(!modLog.canTalk()) return ctx.replyError {
            "Cannot update moderation log due to missing of MESSAGE_WRITE permission."
        }

        val moderator = ctx.member
        val parts = ctx.args.split(commandArgs, 2)

        val case: Case
        val number: Int
        val reason: String

        if(parts.size == 1 || parts[0] doesNotMatch CASE_NUMBER_REGEX) {
            // Only one argument
            val cases = moderator.casesWithoutReason

            if(cases.isEmpty()) {
                return ctx.replySuccess("You have no outstanding cases.")
            }

            case = cases[0]
            number = case.number
            reason = ctx.args
        } else {
            // A case number was specified
            number = parts[0].toInt()
            case = ctx.guild.getCase(number) ?: return ctx.replyError {
                "Specify a case number lower than the latest case number!"
            }
            reason = parts[1]
        }

        when {
            case.modId != moderator.user.idLong -> return ctx.replyError {
                "Only the moderator who performed case number `$number` may update it's reason."
            }

            reason.length > 200 -> return ctx.invalidArgs {
                "Reasons must not be longer than 200 characters!"
            }
        }

        val logMessage = try {
            modLog.getMessageById(case.messageId).await()
        } catch(e: ErrorResponseException) {
            if(e.errorResponse == ErrorResponse.UNKNOWN_MESSAGE) return ctx.replyError {
                "No case message was found for case number `$number`!"
            }
            return ctx.replyError {
                "An unexpected error occurred while getting the logged message for case number `$number`!"
            }
        }

        try {
            // Unlike the other moderator commands, this is dependent on the actual
            // ModLog being updated. As such, we do not dispatch this on a separate
            // job, and respect that if it fails we provide an appropriate response
            ModLog.editReason(logMessage, reason)
        } catch(t: Throwable) {
            ModLog.Log.error("Failed to update case number $number for Guild ID: ${ctx.guild.idLong}", t)
            return ctx.replyError {
                "An unexpected error occurred while updating the logged message for case number `$number`"
            }
        }

        ctx.sendSuccess("Updated reason for case number `$number`!")

        // We do this last because this writes to the DB.
        case.reason = reason
    }
}
