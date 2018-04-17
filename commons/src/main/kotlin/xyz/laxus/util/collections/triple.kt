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
package xyz.laxus.util.collections

/**
 * @author Kaidan Gustave
 */
data class Triple<out F, out S, out T>(
    val first: F,
    val second: S,
    val third: T
)

inline fun <reified F, reified S, reified T> Pair<F, S>.to(third: T) = Triple(first, second, third)
inline fun <reified F, reified S, reified T> F.to(pair: Pair<S, T>) = Triple(this, pair.first, pair.second)