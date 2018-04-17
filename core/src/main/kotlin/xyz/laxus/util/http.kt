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
package xyz.laxus.util

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException

inline fun OkHttpClient.newRequest(lazy: Request.Builder.() -> Unit): Call {
    val builder = Request.Builder()
    builder.lazy()
    return newCall(builder.build())
}

operator fun Request.Builder.set(header: String, value: String): Request.Builder = header(header, value)

@Throws(IOException::class)
suspend inline fun <reified C: Call> C.await(): Response = suspendCancellableCoroutine(holdCancellability = true) { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if(call.isCanceled) {
                cont.initCancellability()
                val req = call.request().let { "${it.method()} - ${it.url()}" }
                cont.cancel(CancellationException("Request $req was cancelled!"))
            } else {
                cont.resumeWithException(e)
            }
        }

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
            if(call.isCanceled) {
                cont.initCancellability()
                val req = call.request().let { "${it.method()} - ${it.url()}" }
                cont.cancel(CancellationException("Request $req was cancelled!"))
            } else {
                cont.resume(response)
            }
        }
    })
}
