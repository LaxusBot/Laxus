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
@file:JvmName("NetUtil")
package xyz.laxus.utils

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * Encodes the [url] string fragment with the provided [charset]
 * encoding (`UTF-8` by default).
 *
 * @param url The URL string fragment to encode.
 * @param charset The [Charset] encoding to use.
 *
 * @return The encoded url string fragment.
 *
 * @throws UnsupportedEncodingException If the [charset] encoding name is not supported.
 */
@Throws(UnsupportedEncodingException::class)
fun encodeUrl(url: String, charset: Charset = Charsets.UTF_8): String {
    return URLEncoder.encode(url, charset.name())
}

/**
 * Decodes the [url] string fragment with the provided [charset]
 * encoding (`UTF-8` by default).
 *
 * @param url The URL string fragment to decode.
 * @param charset The [Charset] encoding to use.
 *
 * @return The decoded url string fragment.
 *
 * @throws UnsupportedEncodingException If the [charset] encoding name is not supported.
 */
@Throws(UnsupportedEncodingException::class)
fun decodeUrl(url: String, charset: Charset = Charsets.UTF_8): String {
    return URLDecoder.decode(url, charset.name())
}
