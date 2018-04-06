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
package xyz.laxus.util.functional

import xyz.laxus.util.internal.impl.SimpleAddRemoveBlockWrapperImpl as AddRemoveImpl
import xyz.laxus.util.internal.impl.SimpleAddBlockWrapperImpl as AddImpl
import xyz.laxus.util.internal.impl.SimpleRemoveBlockWrapperImpl as RemoveImpl

interface AddBlock<in T> {
    fun add(element: T)

    operator fun T.unaryPlus() {
        add(this)
    }

    operator fun Iterable<T>.unaryPlus() {
        for(e in this) + e
    }
}

interface RemovalBlock<in T> {
    fun remove(element: T)

    operator fun T.unaryMinus() {
        remove(this)
    }

    operator fun Iterable<T>.unaryMinus() {
        for(e in this) - e
    }
}

interface AddRemoveBlock<in T> : AddBlock<T>, RemovalBlock<T>

fun <T> additionBlock(collection: MutableCollection<T>): AddBlock<T> = AddImpl(collection)
fun <T> removalBlock(collection: MutableCollection<T>): RemovalBlock<T> = RemoveImpl(collection)
fun <T> addRemoveBlock(collection: MutableCollection<T>): AddRemoveBlock<T> = AddRemoveImpl(collection)
