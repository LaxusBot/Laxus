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
package xyz.laxus.testing.test

import org.junit.jupiter.params.ParameterizedTest
import xyz.laxus.testing.Arg
import xyz.laxus.testing.Args
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ArgsTests {
    @Arg(strings = ["foo", "bar"])
    @ParameterizedTest fun `Test Arg Test`(arg: String) {
        assertTrue(arg == "foo" || arg == "bar")
    }

    @Args(
        Arg(strings = arrayOf("a", "b", "c")),
        Arg(ints = intArrayOf( 1 ,  2 ,  3 ))
    )
    @ParameterizedTest fun `Test Args Test`(string: String, int: Int) {
        when(int) {
            1 -> assertEquals("a", string)
            2 -> assertEquals("b", string)
            3 -> assertEquals("c", string)

            else -> fail("Unexpected int value: $int")
        }
    }
}
