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
@file:Suppress("Unused")
package xyz.laxus.jda.util

typealias Activity = net.dv8tion.jda.core.entities.Game

fun playing(name: String): Activity = Activity.playing(name)
fun watching(name: String): Activity = Activity.watching(name)
fun listeningTo(name: String): Activity = Activity.listening(name)
fun streaming(name: String, url: String): Activity {
    if(Activity.isValidStreamingUrl(url)) {
        return Activity.streaming(name, url)
    }
    return Activity.playing(name)
}
