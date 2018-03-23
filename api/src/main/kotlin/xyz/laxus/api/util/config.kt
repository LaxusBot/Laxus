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
import io.ktor.application.ApplicationFeature
import io.ktor.application.featureOrNull
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.pipeline.Pipeline
import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.ApplicationEngineEnvironmentBuilder as AEEBuilder

inline fun AEEBuilder.connector(type: ConnectorType, block: EngineConnectorBuilder.() -> Unit) {
    val builder = EngineConnectorBuilder(type)
    builder.block()
    connectors.add(builder)
}

internal inline fun ContentNegotiation.Configuration.configure(
    contentType: ContentType,
    configure: ConfigurableContentConverter.() -> Unit
) = register(contentType, ConfigurableContentConverter().apply(configure))

inline fun <P: Pipeline<*, ApplicationCall>, F: Any>
    P.installOrApply(feature: ApplicationFeature<P, F, F>, configure: F.() -> Unit = {}): F {
    return (featureOrNull(feature) ?: install(feature)).apply(configure)
}