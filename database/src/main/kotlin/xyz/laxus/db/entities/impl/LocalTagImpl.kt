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
package xyz.laxus.db.entities.impl

import xyz.laxus.db.DBLocalTags
import xyz.laxus.db.entities.LocalTag
import xyz.laxus.db.sql.getNullLong
import java.sql.ResultSet

/**
 * @author Kaidan Gustave
 */
internal data class LocalTagImpl(
    override val name: String,
    override var content: String,
    override var ownerId: Long?,
    override val guildId: Long
): LocalTag {
    private var deleted = false

    constructor(results: ResultSet): this(
        name = results.getString("NAME"),
        content = results.getString("CONTENT"),
        ownerId = results.getNullLong("OWNER_ID"),
        guildId = results.getLong("GUILD_ID")
    )

    override fun edit(newContent: String) {
        checkDeleted()
        content = newContent
        DBLocalTags.updateTag(this)
    }

    override fun delete() {
        checkDeleted()
        DBLocalTags.deleteTag(this)
        deleted = true
    }

    override fun override() {
        checkDeleted()
        ownerId = null
        DBLocalTags.overrideTag(this)
    }

    private fun checkDeleted() {
        check(!deleted) { "Deleted tag cannot be modified!" }
    }
}
