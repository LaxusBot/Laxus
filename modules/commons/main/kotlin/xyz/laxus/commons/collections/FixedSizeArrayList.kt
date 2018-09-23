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
@file:Suppress("LoopToCallChain", "UseWithIndex")
package xyz.laxus.commons.collections

import kotlin.math.max
import kotlin.math.min

internal open class FixedSizeArrayList<T>(vararg elements: T): List<T> {
    private val elements = elements.copyOf()

    override val size get() = elements.size

    override fun contains(element: T): Boolean {
        if(isEmpty())
            return false
        for(e in elements) {
            if(e == element)
                return true
        }
        return false
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for(e in elements) {
            if(e !in this)
                return false
        }
        return true
    }

    override fun get(index: Int): T = elements[index]

    override fun indexOf(element: T): Int {
        if(isEmpty())
            return -1
        var at = 0
        for(e in elements) {
            if(e == element)
                return at
            at++
        }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): Iterator<T> = Iter()

    override fun lastIndexOf(element: T): Int {
        if(isEmpty())
            return -1
        var at = 0
        var i = -1
        for(e in elements) {
            if(e == element)
                i = at
            at++
        }
        return i
    }

    override fun listIterator(): ListIterator<T> = ListIter()

    override fun listIterator(index: Int): ListIterator<T> {
        if(index !in 0..size) {
            throw IndexOutOfBoundsException("Index is out of range [0, $size]")
        }
        return ListIter(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        return FixedSizeArrayList(*elements.copyOfRange(fromIndex, toIndex))
    }

    private open inner class ListIter(index: Int = 0): ListIterator<T>, Iter(index) {
        override fun hasPrevious(): Boolean = index > 0
        override fun nextIndex(): Int = min(index + 1, size)
        override fun previousIndex(): Int = max(index - 1, 0)
        override fun previous(): T {
            if(!hasPrevious())
                noElement(next = false)
            val value = elements[index]
            index--
            return value
        }
    }

    private open inner class Iter(protected var index: Int = 0): Iterator<T> {
        override fun hasNext(): Boolean = index < size
        override fun next(): T {
            if(!hasNext())
                noElement(next = true)
            val value = elements[index]
            index++
            return value
        }

        protected fun noElement(next: Boolean): Nothing {
            throw NoSuchElementException("Iterator has no ${if(next) "next" else "previous"} element!")
        }
    }
}
