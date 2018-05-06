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
package xyz.laxus.jda.menus

import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
@Menu.Dsl
data class TimeOut(var delay: Long = 0L, var unit: TimeUnit = TimeUnit.SECONDS) {
    @Menu.Dsl
    inline fun delay(lazy: () -> Long) { delay = lazy() }

    @Menu.Dsl
    inline fun unit(lazy: () -> TimeUnit) { unit = lazy() }
}
