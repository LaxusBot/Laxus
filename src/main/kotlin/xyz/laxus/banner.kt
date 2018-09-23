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
@file:JvmName("Banner")
package xyz.laxus

import ch.qos.logback.core.pattern.color.ANSIConstants.*
import xyz.laxus.utils.resourceStreamOf
import kotlin.math.max

private const val RESET = "${ESC_START}0;$DEFAULT_FG$ESC_END"
private fun highlight(text: String, color: String): String = "$ESC_START$color$ESC_END$text$RESET"
private val colors = arrayOf(
    RED_FG,
    YELLOW_FG,
    GREEN_FG,
    CYAN_FG,
    BLUE_FG,
    MAGENTA_FG
)

fun sendBanner() {
    val bannerTxt = Main::class.resourceStreamOf("/banner.txt")?.use { it.reader().readText() } ?: return
    var forward = true
    var i = 0
    val highlighted = bannerTxt.trimIndent().split('\n').joinToString("\n") {
        val color = colors[i]
        if(forward) {
            i++
        } else {
            i--
        }
        if(i !in 0 until colors.size - 1) {
            forward = !forward
            i = max(i, 0)
        }

        highlight(it.trimEnd(), color)
    }
    println(highlighted)
    println() // Ending newline
}
