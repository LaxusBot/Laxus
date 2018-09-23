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
@file:Suppress("unused")
package xyz.laxus.reflect.test

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.spekframework.spek2.Spek
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.style.specification.describe
import xyz.laxus.reflect.*
import xyz.laxus.reflect.meta.Meta
import xyz.laxus.reflect.meta.MetaData
import xyz.laxus.reflect.meta.meta
import java.util.stream.IntStream
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.streams.asSequence
import kotlin.test.*

@Retention(AnnotationRetention.RUNTIME)
annotation class A

enum class TestEnum { A, B, C }

object ReflectionSpek: Spek({
    group("class loading") {
        describe("class loading without a classloader") {
            it("loads a valid class") {
                val pair = loadClass("kotlin.Pair")
                assertNotNull(pair)
                assertEquals(Pair::class, pair!!)
                assertEquals("kotlin.Pair", pair.qualifiedName)
            }

            it("doesn't load an invalid class and returns null instead") {
                val pear = loadClass("kotlin.Pear")
                assertNull(pear)
            }
        }

        describe("class loading with a classloader") {
            val classloader by memoized { Pair::class.java.classLoader }

            it("loads a class using the classloader") {
                val triple = classloader.loadKlass("kotlin.Triple")
                assertNotNull(triple)
                assertEquals(Triple::class, triple!!)
                assertEquals("kotlin.Triple", triple.qualifiedName)
            }

            it("doesn't load an invalid class and returns null instead") {
                val tuple = classloader.loadKlass("kotlin.Tuple")
                assertNull(tuple)
            }
        }
    }

    group("miscellaneous reflection") {
        test("KFunction.isExtension extension") {
            assertTrue(String::capitalize.isExtension)
        }

        test("KFunction.isStatic extension") {
            assertTrue(::assertNull.isStatic)
        }

        test("KParameter.isNullable extension", skip = Skip.Yes()) {
            val ext = checkNotNull(Any?::toString.extensionReceiverParameter)
            assertTrue(ext.isNullable)
        }

        describe("a class with metadata") {
            @Meta("description", "A reference with a name and value.")
            data class Ref<V>(
                @property:Meta("description", "The name of the value.")
                val name: String,
                @property:MetaData(
                    Meta("description", "The value of this Ref."),
                    Meta("type", "The type of this Ref.")
                )
                val value: V
            )

            it("has metadata 'description' on key property") {
                assertEquals("The name of the value.", Ref<*>::name.meta("description"))
            }

            it("has metadata 'description' and 'type' on value property") {
                val value = Ref<*>::value
                assertEquals("The value of this Ref.", value.meta("description"))
                assertEquals("The type of this Ref.", value.meta("type"))
            }
        }
    }
})

@A class Foo {
    @A val bar = "bar"
    @A fun baz() = println("baz!")
}

class ReflectionTests {
    @Nested inner class AnnotationReflectionTests {
        @Test fun `Test hasAnnotation On Annotated Class`() {
            assertTrue(Foo::class.hasAnnotation<A>())
        }

        @Test fun `Test hasAnnotation On Annotated Property`() {
            assertTrue(Foo::bar.hasAnnotation<A>())
        }

        @Test fun `Test hasAnnotation On Annotated Function`() {
            assertTrue(Foo::baz.hasAnnotation<A>())
        }
    }

    @Nested inner class PackageReflectionTests {
        @Test fun `Test KPackage Is Named Correctly`() {
            val pkg = packageOf(ReflectionTests::class)

            assertEquals("xyz.laxus.reflect.test", pkg.name)
        }

        @Test fun `Test All KPackages Are Accounted For`() {
            val packages = KPackage.All

            assertTrue(packages.any { it.name == "kotlin.jvm" })
            assertTrue(packages.any { it.name == "kotlin.reflect" })
            assertTrue(packages.any { it.name == "xyz.laxus.reflect" })
        }
    }

    @Nested inner class EnumReflectionTests {
        @Test fun `Test Reflected Enum Provides Specific Values`() {
            assertEquals(TestEnum.A, TestEnum::class.valueOf("A"))
            assertEquals(TestEnum.C, TestEnum::class.valueOf("C"))
        }

        @Test fun `Test Reflected Enum Does Not Provide Specific Values When None Exist`() {
            assertFails { TestEnum::class.valueOf("BB") }
        }

        @Test fun `Test Reflected Enum Provides All Values In Order Of Declaration`() {
            val values = TestEnum::class.values()

            assertEquals(3, values.size)
            assertEquals(TestEnum.B, values[1])
        }
    }
}
