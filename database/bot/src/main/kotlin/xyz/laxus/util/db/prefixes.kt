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
@file:Suppress("unused")
package xyz.laxus.util.db

import net.dv8tion.jda.core.entities.Guild
import xyz.laxus.db.DBPrefixes

val Guild.prefixes: Set<String> get() {
    return DBPrefixes.getPrefixes(idLong)
}

fun Guild.hasPrefix(prefix: String) {
    DBPrefixes.hasPrefix(idLong, prefix)
}

fun Guild.addPrefix(prefix: String) {
    require(prefix.length <= 50) { "Prefix cannot be longer than 50 characters" }
    DBPrefixes.addPrefix(idLong, prefix)
}

fun Guild.removePrefix(prefix: String) {
    DBPrefixes.removePrefix(idLong, prefix)
}