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
package xyz.laxus.test

import com.typesafe.config.Config
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import xyz.laxus.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DisplayName("Test Hocon")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestHocon {
    private lateinit var config: Config

    @BeforeAll fun `Load Config`() {
        this.config = loadConfig("test.conf")
    }

    @Test fun `Test Expand Flattened Paths To Expanded Config`() {
        val flattened = config.config("test.flattened")
        assertNotNull(flattened)
        assertEquals(flattened!!.entrySet().size, 2)
        val name = flattened.string("name")
        val age = flattened.int("age")
        assertEquals(name, "Kaidan")
        assertEquals(age, 19)
    }

    @Test fun `Test Load Class From Config`() {
        val c = config.klass("test.class")
        assertNotNull(c)
        val unit = c!!.objectInstance
        assertEquals(unit, Unit)
    }
}