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
package xyz.laxus.api.util

import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.http.ContentType
import io.ktor.pipeline.PipelineContext
import io.ktor.request.ApplicationReceiveRequest

/**
 * @author Kaidan Gustave
 */
internal class ConfigurableContentConverter : ContentConverter {
    private lateinit var receive: suspend PipelineContext<ApplicationReceiveRequest, ApplicationCall>.() -> Any?
    private lateinit var send: suspend PipelineContext<Any, ApplicationCall>.(ContentType, Any) -> Any?

    override suspend fun convertForReceive(
        context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>
    ): Any? = receive(context)

    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any
    ): Any? = send(context, contentType, value)

    fun receive(handle: suspend PipelineContext<ApplicationReceiveRequest, ApplicationCall>.() -> Any?) { receive = handle }
    fun send(handle: suspend PipelineContext<Any, ApplicationCall>.(ContentType, Any) -> Any?) { send = handle }
}
