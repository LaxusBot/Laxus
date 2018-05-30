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
package xyz.laxus.wyvern.internal

import xyz.laxus.util.createLogger
import xyz.laxus.util.warn
import kotlin.jvm.JvmStatic as static

internal object Reporter {
    // Warning flags
    @static private var hasWarnedAboutDefaultOnNonSuspend = false
    @static private var hasWarnedAboutNoDefaultOnSuspend = false

    @static private val Log = createLogger(Reporter::class)

    @static internal fun warnAboutDefaultOnNonSuspend() {
        if(!hasWarnedAboutDefaultOnNonSuspend) {
            Log.warn {
                "@Default on non-suspend function is not recommended, as support for @Default " +
                "will be discontinued once a proper function for calling a suspend function " +
                "with mapped arguments is included in the standard library!"
            }
            Log.warn("See: https://youtrack.jetbrains.net/issue/KT-21972")
            hasWarnedAboutDefaultOnNonSuspend = true
        }
    }

    @static internal fun warnAboutNoDefaultOnSuspend() {
        if(!hasWarnedAboutNoDefaultOnSuspend) {
            // Only provide deeper insight once
            Log.warn {
                "Currently, suspend functions cannot be invoked reflectively using mapped arguments, " +
                "and thus it is impossible to invoke suspend functions using optional arguments!"
            }
            Log.warn {
                "To provide this functionality, annotate all optional value-parameters " +
                "of suspend functions with @Default!"
            }
            Log.warn("See: https://youtrack.jetbrains.net/issue/KT-21972")
            hasWarnedAboutNoDefaultOnSuspend = true
        }
    }
}
