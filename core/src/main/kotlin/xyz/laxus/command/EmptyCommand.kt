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
package xyz.laxus.command

/**
 * @author Kaidan Gustave
 */
abstract class EmptyCommand: Command {
    override val arguments by lazy { children.joinToString("|", "[", "]", 4, "...") { it.name } }

    constructor(group: Group): super(group)
    constructor(parent: Command): super(parent)

    override suspend fun execute(ctx: CommandContext) {
        if(ctx.args.isEmpty()) ctx.missingArgs() else ctx.invalidArgs()
    }
}
