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
package xyz.laxus.wyvern.http.body

import xyz.laxus.wyvern.http.CallContext
import xyz.laxus.wyvern.http.error.HttpError
import xyz.laxus.wyvern.http.header.ContentType
import kotlin.reflect.KClass

/**
 * Helps an application provide specific body types
 * as parameters for handlers.
 *
 * @author Kaidan Gustave
 */
interface BodyProvider<T> {
    val contentType: ContentType
    val kotlinTypes: List<KClass<*>>

    fun CallContext.convert(): T?

    fun convertError(error: HttpError): T? { return null }
}