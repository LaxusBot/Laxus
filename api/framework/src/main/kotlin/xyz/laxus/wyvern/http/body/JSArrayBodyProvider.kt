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

import me.kgustave.json.JSArray
import me.kgustave.json.exceptions.JSSyntaxException
import me.kgustave.json.readJSArray
import xyz.laxus.util.reflect.loadClass
import xyz.laxus.wyvern.API
import xyz.laxus.wyvern.context.RouteContext
import xyz.laxus.wyvern.http.error.badRequest
import xyz.laxus.wyvern.http.header.ContentType

/**
 * @author Kaidan Gustave
 */
class JSArrayBodyProvider: BodyProvider<JSArray> {
    override val contentType = ContentType.Application.Json
    override val kotlinTypes = listOfNotNull(
        loadClass("me.kgustave.json.internal.JSArrayImpl"), // load
        JSArray::class
    )

    override fun RouteContext.convert(): JSArray? {
        val requestContentType = request.contentType
        try {
            request.byteBody.inputStream().use {
                it.reader(requestContentType.charset ?: API.DefaultCharset).use {
                    return it.readJSArray()
                }
            }
        } catch(e: JSSyntaxException) {
            throw badRequest("Unable to read JSON body!")
        } catch(e: Exception) {
            return null
        }
    }
}