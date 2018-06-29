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
@file:Suppress("unused")
package xyz.laxus.api.handlers.annotations

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(FUNCTION)
@Retention(RUNTIME)
@Suppress("ANNOTATION_CLASS_MEMBER")
annotation class Code(val value: Int) {
    companion object {
        const val Continue = 100
        const val SwitchingProtocols = 101
        const val Processing = 102

        const val OK = 200
        const val Created = 201
        const val Accepted = 202
        const val NonAuthoritativeInformation = 203
        const val NoContent = 204
        const val ResetContent = 205
        const val PartialContent = 206

        const val MultipleChoices = 300
        const val MovedPermanently = 301
        const val Found = 302
        const val SeeOther = 303
        const val NotModified = 304
        const val UseProxy = 305
        const val SwitchProxy = 306
        const val TemporaryRedirect = 307
        const val PermanentRedirect = 308

        const val BadRequest = 400
        const val Unauthorized = 401
        const val PaymentRequired = 402
        const val Forbidden = 403
        const val NotFound = 404
        const val MethodNotAllowed = 405
        const val NotAcceptable = 406
        const val ProxyAuthenticationRequired = 407
        const val RequestTimeout = 408
        const val Conflict = 409
        const val Gone = 410
        const val LengthRequired = 411
        const val PreconditionFailed = 412
        const val PayloadTooLarge = 413
        const val RequestURITooLong = 414

        const val UnsupportedMediaType = 415
        const val RequestedRangeNotSatisfiable = 416
        const val ExceptionFailed = 417
        const val UpgradeRequired = 426
        const val TooManyRequests = 429
        const val RequestHeaderFieldTooLarge = 431

        const val InternalServerError = 500
        const val NotImplemented = 501
        const val BadGateway = 502
        const val ServiceUnavailable = 503
        const val GatewayTimeout = 504
        const val VersionNotSupported = 505
        const val VariantAlsoNegotiates = 506
    }
}