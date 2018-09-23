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
package xyz.laxus.utils.test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import xyz.laxus.utils.lineSeparator
import kotlin.test.assertEquals

class SystemUtilTests {
    @EnabledOnOs(OS.WINDOWS)
    @Test fun `Test Line Separator On Windows`() {
        assertEquals("\r\n", lineSeparator)
    }

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test fun `Test Line Separator On Linux & Mac`() {
        assertEquals("\n", lineSeparator)
    }
}
