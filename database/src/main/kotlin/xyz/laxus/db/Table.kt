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
package xyz.laxus.db

import xyz.laxus.db.schema.Column
import xyz.laxus.db.schema.Columns
import xyz.laxus.db.schema.TableName
import xyz.laxus.util.delegation.annotation

/**
 * @author Kaidan Gustave
 */
abstract class Table : AutoCloseable {
    protected val connection by lazy { Database.connection }
    val name by annotation<TableName, String?> { it.value }
    val columns by annotation<Columns, Array<out Column>?> { it.value }

    override fun close() {
        // Can be overriden if necessary
    }
}
