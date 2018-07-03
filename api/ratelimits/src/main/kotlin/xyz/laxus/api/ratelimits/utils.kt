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
package xyz.laxus.api.ratelimits

import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

internal fun chronoUnitOf(unit: TimeUnit): ChronoUnit = when(unit) {
    TimeUnit.NANOSECONDS -> ChronoUnit.NANOS
    TimeUnit.MICROSECONDS -> ChronoUnit.MICROS
    TimeUnit.MILLISECONDS -> ChronoUnit.MILLIS
    TimeUnit.SECONDS -> ChronoUnit.SECONDS
    TimeUnit.MINUTES -> ChronoUnit.MINUTES
    TimeUnit.HOURS -> ChronoUnit.HOURS
    TimeUnit.DAYS -> ChronoUnit.DAYS
}