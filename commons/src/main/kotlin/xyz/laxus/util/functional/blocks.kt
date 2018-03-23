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

/**
 * @author Kaidan Gustave
 */
interface AdditionBlock<in T> {
    fun add(element: T)

    operator fun T.unaryPlus() {
        add(this)
    }
}

/**
 * @author Kaidan Gustave
 */
interface RemovalBlock<in T> {
    fun remove(element: T)

    operator fun T.unaryMinus() {
        remove(this)
    }
}

/**
 * @author Kaidan Gustave
 */
interface AddRemoveBlock<in T> : AdditionBlock<T>, RemovalBlock<T>
