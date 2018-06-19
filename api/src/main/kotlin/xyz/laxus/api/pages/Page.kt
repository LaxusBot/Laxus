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
package xyz.laxus.api.pages

import io.ktor.html.Template
import kotlinx.html.*
import java.io.File
import java.net.URL
import java.nio.charset.Charset

/**
 * @author Kaidan Gustave
 */
class Page(config: Config): Template<HTML> {
    private val charset = config.charset
    private val pageTitle = config.pageTitle
    private val metaTags = config.metaTags.toList()
    private val scripts = config.scripts.map { it.relativeTo(File(System.getProperty("user.dir"))) }
    private val styles = config.styles.map { it.relativeTo(File(System.getProperty("user.dir"))) }
    private val embed = config.embed?.copy()

    constructor(config: Config.() -> Unit): this(Config().apply(config))

    override fun HTML.apply() {
        head {
            charset?.let { meta(charset = it.name()) }
            pageTitle?.let { title(it) }
            metaTags.forEach { meta(name = it.name, content = it.content) }
            styles.forEach { link(href = it.path, rel = "stylesheet") }
            embed?.apply {
                this.title?.let { meta(name = "og:title", content = it) }
                this.description?.let { meta(name = "og:description", content = it) }
                this.image?.let { meta(name = "og:image", content = it) }
                this.url?.let { meta(name = "og:url", content = it) }
            }
        }
        body {
            noScript { +"You need to enable JavaScript to run this app." }
            div { id = "root" }
            scripts.forEach {
                script {
                    type = "type/javascript"
                    src = it.toURI().path
                }
            }
        }
    }

    class Config internal constructor() {
        val metaTags = mutableListOf<MetaTag>()
        val scripts = mutableListOf<File>()
        val styles = mutableListOf<File>()
        var pageTitle: String? = null
        var charset: Charset? = null
        var embed: DiscordEmbed? = null

        inline fun metaTag(name: String, content: () -> String) {
            this.metaTags += MetaTag(name, content())
        }

        inline fun embed(config: DiscordEmbed.() -> Unit) {
            this.embed = DiscordEmbed().apply(config)
        }
    }

    data class MetaTag(val name: String, val content: String)
}