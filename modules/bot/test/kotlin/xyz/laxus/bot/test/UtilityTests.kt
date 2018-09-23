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
package xyz.laxus.bot.test

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import xyz.laxus.bot.utils.commandArgsOf
import xyz.laxus.bot.utils.parseTimeArgument
import java.time.Duration
import java.time.temporal.ChronoUnit.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UtilityTests {
    @Nested inner class TimeUtilityTests {
        @Test fun `Test Parse Simple Time Argument`() {
            val parsed = parseTimeArgument("5 seconds")

            assertNotNull(parsed)
            assertEquals(Duration.ofSeconds(5), parsed)
        }

        @Test fun `Test Parse Complex Time Argument`() {
            val parsed = parseTimeArgument("5 minutes 25seconds 2 weeks 4 days")
            val expected = Duration.ofMillis(
                MINUTES.duration.toMillis() * 5 +
                SECONDS.duration.toMillis() * 25 +
                WEEKS.duration.toMillis() * 2 +
                DAYS.duration.toMillis() * 4
            )

            assertNotNull(parsed)
            assertEquals(expected, parsed)
        }
    }

    @Nested inner class ArgumentsUtilityTests {
        @Test fun `Test Split Command Arguments`() {
            val arguments = " abc   123  "
            val (a, b) = commandArgsOf(arguments)

            assertEquals("abc", a)
            assertEquals(123, b.toIntOrNull())
        }

        @Test fun `Test Split Command Arguments With Limit`() {
            val arguments = "  foo  barbaz   fiz bar "
            val (a, b, c) = commandArgsOf(arguments, limit = 3)

            assertEquals("foo", a)
            assertEquals("barbaz", b)
            assertEquals("fiz bar", c)
        }
    }
}
