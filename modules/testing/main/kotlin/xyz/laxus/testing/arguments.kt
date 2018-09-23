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
package xyz.laxus.testing

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.support.AnnotationConsumer
import java.util.stream.Stream

@ArgumentsSource(ArgsProvider::class)
annotation class Args(vararg val args: Arg)

@ArgumentsSource(ArgProvider::class)
annotation class Arg(
    val strings: Array<String> = [],
    val booleans: BooleanArray = [],
    val ints: IntArray = [],
    val longs: LongArray = []
)

internal fun Arg.sequential(): Sequence<Array<out Any>> =
    sequenceOf(strings, booleans.toTypedArray(), ints.toTypedArray(), longs.toTypedArray())

internal class ArgsProvider: ArgumentsProvider, AnnotationConsumer<Args> {
    private lateinit var arguments: List<Array<Any>>

    override fun accept(t: Args) {
        var runs = 0

        val source = t.args.map { arg ->
            val valid = arg.sequential().filter { it.isNotEmpty() }.toList()
            require(valid.size == 1)
            val a = valid[0]
            if(runs == 0) runs = a.size
            require(runs == a.size)
            return@map a
        }

        val arguments = List(runs) { mutableListOf<Any>() }

        for((run, argument) in arguments.withIndex()) {
            for(arg in source) {
                argument += arg[run]
            }
        }

        this.arguments = arguments.map { it.toTypedArray() }
    }

    override fun provideArguments(context: ExtensionContext): Stream<Arguments> {
        return arguments.stream().map { array -> Arguments { array } }
    }
}

internal class ArgProvider: ArgumentsProvider, AnnotationConsumer<Arg> {
    private lateinit var arguments: Array<out Any>

    override fun accept(t: Arg) {
        val valid = t.sequential().filter { it.isNotEmpty() }.toList()
        require(valid.size == 1)
        this.arguments = valid[0]
    }

    override fun provideArguments(context: ExtensionContext): Stream<Arguments> {
        return Stream.of(*arguments).map { Arguments.of(it) }
    }
}
