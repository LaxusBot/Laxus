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
class TimeOut {
    var delay = 0L
    var unit = TimeUnit.SECONDS

    inline infix fun delay(lazy: () -> Long) { delay = lazy() }
    inline infix fun unit(lazy: () -> TimeUnit) { unit = lazy() }
}
