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

import org.junit.jupiter.api.*
import xyz.laxus.util.currentTime
import xyz.laxus.util.delegation.AnnotationDelegate
import xyz.laxus.util.delegation.annotation
import xyz.laxus.util.delegation.function
import xyz.laxus.util.randomInt
import xyz.laxus.util.titleName
import java.util.*
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("Test Delegates")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDelegates {
    private companion object {
        private const val NonNullValue = "Hello, World!"
    }

    @Nested
    @DisplayName("Test Function Delegates")
    inner class Functions {
        @Nested
        @DisplayName("Test Random Numbers Function Delegates")
        inner class RandomNumbers {
            private var min = 0
            private var max = 10
            private val randomNumber by function(this::generateRandomNumber)
            private val randomMin by function { randomInt(0, 10) }
            private val randomMax by function { max(this.min, randomMin) + 1 }

            @BeforeEach
            fun randomize() {
                this.min = randomMin
                this.max = randomMax
            }

            @RepeatedTest(20)
            @DisplayName("Test Random Numbers Delegate")
            fun testRandomNumbers() {
                val randomNumber = randomNumber
                assertTrue(randomNumber in min..max)
                println("$randomNumber is in the range [$min, $max]")
            }

            private fun generateRandomNumber(): Int = randomInt(min, max)
        }

        @Nested
        @DisplayName("Test Unique IDs Function Delegates")
        inner class UniqueIDs {
            private val uuid: UUID = UUID.randomUUID()
            private val uniqueId by function(uuid::toString)

            @Test
            @DisplayName("Test Unique IDs Delegate")
            fun testUniqueIDs() {
                val uniqueId = uniqueId
                val uuid = UUID.fromString(uniqueId)
                assertEquals(uuid, this.uuid)
            }
        }
    }

    @Nested
    @DisplayName("Test Annotation Delegates")
    inner class Annotations {
        @BeforeEach
        fun printCacheStatus() {
            println("Global Class Cache:    ${AnnotationDelegate.Cache.GlobalClassCache.size}")
            println("Global Property Cache: ${AnnotationDelegate.Cache.GlobalPropertyCache.size}")
        }

        @RepeatedTest(2) // Repeated to test cached values
        @DisplayName("Test Annotation Delegate")
        fun testAnnotationDelegate() {
            val value = TestOne.value
            assertNotNull(value)
            assertTrue(value!!.isNotBlank())
            assertEquals(value, NonNullValue)
        }

        @RepeatedTest(2) // Repeated to test cached values
        @DisplayName("Test Nullable Annotation Delegate")
        fun testNullableAnnotationDelegate() {
            val value = TestTwo.value
            assertNull(value)
        }

        // Note that depending on future kotlin-reflect updates,
        //this test may become invalid.
        // If this is the case, addition of the @Ignore annotation
        //will be necessary for this class to pass testing.
        // At the moment, this test seems to show fairly reasonable
        //operation runtime decreases, as much as 10 ms for this very
        //simple test.
        @Test
        //@Disabled("Inconsistent passes/fails regarding benchmarking")
        @DisplayName("Test Annotation Delegate Cache Runtime Decrease")
        fun testAnnotationDelegateCacheRuntimeDecrease() {
            val subject = TestThree()

            // First round
            val time1 = currentTime
            val value = subject.value
            val time2 = currentTime

            val runtime1 = time2 - time1
            println("First Runtime: $runtime1 ms")
            // End of first round

            assertNotNull(value)
            assertEquals(value, NonNullValue)

            // Second round
            val time3 = currentTime
            val sameValue = subject.value
            val time4 = currentTime

            val runtime2 = time4 - time3
            println("Second Runtime: $runtime2 ms")
            // End of second round

            assertEquals(value, sameValue)
            assert(runtime2 < runtime1) {
                "Runtime does not show any considerable improvements (1: $runtime1, 2: $runtime2)"
            }
        }

        @RepeatedTest(2)
        @DisplayName("Test Multiple Annotations")
        fun testMultipleAnnotations() {
            println(Kaidan.toString())
        }

        @RepeatedTest(2)
        @DisplayName("Test Anonymous Class Annotations")
        fun testAnonymousClassAnnotations() {
            val person =
                @Name(first = "Shengaero", last = "Heaton")
                @Age(value = 20)
                @EyeColor(value = Color.YELLOW)
                object : Person() {}

            println(person)
        }
    }

    ////////////////////
    // Test Resources //
    ////////////////////

    @Target(CLASS)
    @Retention(RUNTIME)
    private annotation class Value(val value: String = "")

    private abstract class AnnotationTestSubject {
        val value by annotation<Value, String?> { it.value.takeIf { it.isNotBlank() } }
    }

    @Value(NonNullValue)
    private object TestOne: AnnotationTestSubject()

    @Value
    private object TestTwo: AnnotationTestSubject()

    @Value(NonNullValue)
    private class TestThree: AnnotationTestSubject()

    @Target(CLASS)
    @Retention(RUNTIME)
    private annotation class Name(val first: String, val middle: Char = ' ', val last: String)

    @Target(CLASS)
    @Retention(RUNTIME)
    private annotation class Age(val value: Int)

    @Target(CLASS)
    @Retention(RUNTIME)
    private annotation class EyeColor(val value: Color)

    private abstract class Person {
        val firstName by annotation<Name, String> { it.first }
        val lastName by annotation<Name, String> { it.last }
        val middleInitial by annotation<Name, Char?> { it.middle.takeIf { it.isLetter() } }
        val age by annotation<Age, Int> { it.value }
        val eyeColor by annotation<EyeColor, Color> { it.value }
        override fun toString(): String {
            return "$firstName ${middleInitial?.plus(". ") ?: ""}$lastName is " +
                   "$age years old and has ${eyeColor.titleName} eyes."
        }
    }

    @Name(first = "Kaidan", middle = 'F', last = "Gustave")
    @Age(value = 19)
    @EyeColor(value = Color.BLUE)
    private object Kaidan : Person()

    private enum class Color {
        RED, BLUE, GREEN, ORANGE, YELLOW, PURPLE, WHITE
    }
}