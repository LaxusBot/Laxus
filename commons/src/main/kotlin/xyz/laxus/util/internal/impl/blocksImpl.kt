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
@file:JvmName("internal_blocks_implKt")
package xyz.laxus.util.internal.impl

import xyz.laxus.annotation.Implementation
import xyz.laxus.util.functional.AddRemoveBlock
import xyz.laxus.util.functional.AddBlock
import xyz.laxus.util.functional.RemovalBlock

@Implementation
internal class SimpleAddBlockWrapperImpl<T>(
    private val collection: MutableCollection<T>
): AddBlock<T> {
    override fun add(element: T) {
        collection.add(element)
    }
}

@Implementation
internal class SimpleRemoveBlockWrapperImpl<T>(
    private val collection: MutableCollection<T>
): RemovalBlock<T> {
    override fun remove(element: T) {
        collection.remove(element)
    }
}

@Implementation
internal class SimpleAddRemoveBlockWrapperImpl<T>(
    private val collection: MutableCollection<T>
): AddRemoveBlock<T> {
    override fun add(element: T) {
        collection.add(element)
    }

    override fun remove(element: T) {
        collection.remove(element)
    }
}