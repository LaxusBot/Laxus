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
package xyz.laxus.db.entities

data class ExperimentAccess
constructor(val id: Long, val level: ExperimentAccess.Level, val type: ExperimentAccess.Type) {
    enum class Level {
        ALPHA, CLOSED_BETA, OPEN_BETA;

        fun canBeAccessedWith(other: Level?): Boolean {
            if(this == OPEN_BETA) return true
            if(other === null) return false
            return this <= other
        }
    }
    enum class Type { USER, GUILD }
}
