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

import xyz.laxus.api.internal.context.RouteContext
import xyz.laxus.api.util.ContentType
import kotlin.reflect.full.starProjectedType

/**
 * @author Kaidan Gustave
 */
class StringBodyConverter: BodyConverter<String> {
    override val contentType = ContentType.Any
    override val kotlinType = String::class.starProjectedType

    override fun RouteContext.convert(): String? = request.body
}