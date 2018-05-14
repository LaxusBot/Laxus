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
package xyz.laxus.api.conversions

import me.kgustave.json.JSArray
import me.kgustave.json.readJSArray
import xyz.laxus.api.API
import xyz.laxus.api.internal.context.RouteContext
import xyz.laxus.api.util.ContentType
import xyz.laxus.util.ignored
import kotlin.reflect.full.starProjectedType

/**
 * @author Kaidan Gustave
 */
class JSArrayBodyConverter: BodyConverter<JSArray> {
    override val contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8)
    override val kotlinType = JSArray::class.starProjectedType

    override fun RouteContext.convert(): JSArray? {
        val requestContentType = request.contentType ?: API.DefaultContentType
        ignored {
            request.byteBody.inputStream().use {
                it.reader(requestContentType.charset ?: Charsets.UTF_8).use {
                    return it.readJSArray()
                }
            }
        }

        return null
    }
}