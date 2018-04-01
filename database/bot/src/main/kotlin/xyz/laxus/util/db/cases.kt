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
import net.dv8tion.jda.core.entities.Member
import xyz.laxus.db.DBCases
import xyz.laxus.db.entities.Case

val Guild.cases: List<Case> get() {
    return DBCases.getCases(idLong)
}

val Guild.currentCaseNumber: Int get() {
    return DBCases.getCurrentCaseNumber(idLong)
}

val Guild.lastCaseNumber: Int get() = currentCaseNumber - 1

val Member.cases: List<Case> get() {
    return DBCases.getCasesByModId(guild.idLong, user.idLong)
}

val Member.casesWithoutReason: List<Case> get() {
    return DBCases.getCasesWithoutReasonByModId(guild.idLong, user.idLong)
}

fun Guild.addCase(case: Case) {
    DBCases.addCase(case)
}

fun Guild.getCase(number: Int): Case? {
    return DBCases.getCase(number, idLong)
}
