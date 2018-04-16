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
package xyz.laxus.command.owner

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.connectedChannel
import xyz.laxus.util.collections.splitWith
import xyz.laxus.util.collections.toArrayOrEmpty
import xyz.laxus.util.modifyIf
import java.util.concurrent.TimeUnit
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class EvalCommand: Command(OwnerGroup) {
    private companion object {
        private const val SCRIPT_ENGINE_EXT = "kts"
        private val ENGINE_MANAGER = ScriptEngineManager()
        private val SYS_EXIT_REGEX = Regex("(?:System\\.)?exit(?:Process)?\\(\\d+\\);?")
        private val DEFAULT_IMPORTS = arrayOf(
            "xyz.laxus.util.db.*",
            "xyz.laxus.jda.*",
            "xyz.laxus.jda.util.*",
            "xyz.laxus.util.*",
            "xyz.laxus.*",
            "kotlinx.coroutines.experimental.*",
            "kotlin.io.*",
            "java.util.function.*",
            "java.util.*",
            "me.kgustave.json.*"
        )
    }

    override val name = "Eval"
    override val help = "Evaluates using a ScriptEngine."
    override val arguments = "[Script]"
    override val hasAdjustableLevel = false

    private val engine: ScriptEngine = ENGINE_MANAGER.getEngineByExtension(SCRIPT_ENGINE_EXT)
    private val engineContext = Context()

    init {
        launch {
            // The engine takes some time to start up, so we load it asynchronously here
            suspendCoroutine<Unit> {
                try {
                    engine.put("Log", Laxus.Log)
                    engine.eval("""
                        val logger = bindings["Log"] as org.slf4j.Logger
                        logger.info("Started ScriptEngine")
                    """.trimIndent())
                    it.resume(Unit)
                } catch(e: Throwable) {
                    it.resumeWithException(e)
                }
            }
        }
    }

    override suspend fun execute(ctx: CommandContext) {
        // Trim off code block if present.
        val args = ctx.args.modifyIf(
            condition = { it.startsWith("```") && it.endsWith("```") },
            block = { it.substring(it.indexOf('\n') + 1, it.length - 3) }
        )

        when {
            args matches SYS_EXIT_REGEX -> {
                val message = ctx.sendWarning("Shutting Down...")
                delay(4, TimeUnit.SECONDS)
                message.editMessage("Naaaah, just kidding!").queue()
            }

            else -> {
                engineContext.clear()
                try {
                    engineContext.load(ctx)
                    val lines = args.split('\n').splitWith { it.startsWith("import ") }
                    lines.first.forEach { engineContext.import(it.substring(7)) }
                    val script = lines.second.joinToString("\n")
                    val evaluation = "${engineContext.scriptPrefix}\n$script"
                    val output = engine.eval(evaluation)
                    ctx.reply("```kotlin\n$args```Evaluated:\n```\n$output```")
                } catch (e: ScriptException) {
                    val error = buildString {
                        for((i, line) in e.message?.split('\n').toArrayOrEmpty().withIndex()) {
                            append(line)
                            if(i == 4) {
                                appendln()
                                append("...")
                                break
                            } else {
                                appendln()
                            }
                        }
                    }

                    ctx.reply("```kotlin\n$args```A ScriptException was thrown:\n```\n$error```")
                } catch (e: Exception) {
                    ctx.reply("```kotlin\n$args```An exception was thrown:\n```\n$e```")
                }
            }
        }
    }

    private inner class Context {
        val properties = HashMap<String, Any?>()
        val imports = HashSet<String>()

        operator fun set(key: String, value: Any?) {
            properties[key] = value
            if(value !== null) {
                val qualifiedName = value::class.qualifiedName
                if(qualifiedName !== null && qualifiedName !in imports) {
                    imports += qualifiedName
                }
            }
            engine.put(key, value)
        }

        fun import(import: String) {
            if(import !in imports) {
                imports += import
            }
        }

        fun clear() {
            properties.clear()
            imports.clear()
        }

        fun load(ctx: CommandContext) {
            // STANDARD
            this["ctx"] = ctx
            this["jda"] = ctx.jda
            this["author"] = ctx.author
            this["channel"] = ctx.channel
            this["bot"] = ctx.bot

            // GUILD
            if(ctx.isGuild) {
                this["guild"] = ctx.guild
                this["member"] = ctx.member
                this["textChannel"] = ctx.textChannel

                // VOICE
                if(ctx.selfMember.connectedChannel !== null) {
                    this["voiceChannel"] = ctx.selfMember.connectedChannel
                    this["voiceState"] = ctx.member.voiceState
                } else {
                    this["voiceChannel"] = null
                    this["voiceState"] = null
                }
            } else {
                this["guild"] = null
                this["member"] = null
                this["textChannel"] = null
                this["voiceChannel"] = null
                this["voiceState"] = null
            }

            // PRIVATE
            if(ctx.isPrivate) {
                this["privateChannel"] = ctx.privateChannel
            } else {
                this["privateChannel"] = null
            }
        }

        val scriptPrefix get() = buildString {
            for(import in imports) {
                appendln("import $import")
            }

            for(import in DEFAULT_IMPORTS.filter { it !in imports }) {
                appendln("import $import")
            }

            for((key, value) in properties) {
                if(value === null) {
                    appendln("val $key = null")
                    continue
                }

                val simpleName = value::class.simpleName

                if(simpleName === null) {
                    appendln("val $key = bindings[\"$key\"]")
                    continue
                }

                appendln("val $key = bindings[\"$key\"] as $simpleName")
            }
        }
    }
}
