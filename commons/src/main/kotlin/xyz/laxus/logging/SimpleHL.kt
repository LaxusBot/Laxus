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
package xyz.laxus.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase

import ch.qos.logback.core.pattern.color.ANSIConstants.*
import ch.qos.logback.classic.Level.*

/**
 * @author Kaidan Gustave
 */
class SimpleHL : ForegroundCompositeConverterBase<ILoggingEvent>() {
    override fun getForegroundColorCode(event: ILoggingEvent): String {
        return when(event.level.levelInt) {
            ERROR_INT -> BOLD + RED_FG
            WARN_INT  -> RED_FG
            INFO_INT  -> GREEN_FG
            DEBUG_INT -> YELLOW_FG
            else      -> DEFAULT_FG
        }
    }
}
