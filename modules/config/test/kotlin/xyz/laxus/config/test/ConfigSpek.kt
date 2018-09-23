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
package xyz.laxus.config.test

import org.spekframework.spek2.Spek
import xyz.laxus.config.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

object ConfigSpek: Spek({
    val config by memoized { loadConfig("/test-config.conf") }

    test("config loads") {
        assertTrue(config.isResolved)
    }

    test("config resolves value") {
        val value = config.int("value")
        assertEquals(123, value)
    }

    test("config fails to resolve unknown value") {
        assertFails { config.string("notavalue") }
    }

    test("config resolves inner value by path") {
        assertEquals("one", config.string("strings.one"))
        assertEquals("2", config.string("strings.two"))
        assertEquals("thr33", config.string("strings.three"))
    }

    test("config returns null when value is not found") {
        val unknown = config.nullLong("unknown")
        assertNull(unknown)
    }

    test("config delegates return values of property name") {
        val value: Int by config
        assertEquals(123, value)
    }

    test("config delegates return null when value is not found") {
        val unknown: Any? by config
        assertNull(unknown)
    }
})
