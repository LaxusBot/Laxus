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
package xyz.laxus.commons.test

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import xyz.laxus.commons.collections.CaseInsensitiveHashMap
import xyz.laxus.testing.Arg
import xyz.laxus.testing.Args
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionTests {
    @Nested inner class CaseInsensitiveMapTests {
        private val map = CaseInsensitiveHashMap<Int>()

        @Arg(strings = ["fOo", "FoO", "foo", "FOO"])
        @ParameterizedTest fun `Test Case-Insensitive Add And Remove`(key: String) {
            map[key] = 56

            assertTrue(key in map)
            assertTrue(key.toLowerCase() in map)
            assertTrue(key.toUpperCase() in map)

            map -= key

            assertFalse(key in map)
            assertFalse(key.toLowerCase() in map)
            assertFalse(key.toUpperCase() in map)
        }

        @Arg(strings = ["bOo", "BoO", "boo", "BOO"])
        @ParameterizedTest fun `Test Retain Key Casing`(key: String) {
            map[key] = 56
            val keys = map.keys

            assertTrue(key in keys)

            if(!key.all { it.isLowerCase() }) {
                assertFalse(key.toLowerCase() in keys)
            }

            if(!key.all { it.isUpperCase() }) {
                assertFalse(key.toUpperCase() in keys)
            }
        }

        @AfterEach fun `Clear Map`() = map.clear()
    }
}
