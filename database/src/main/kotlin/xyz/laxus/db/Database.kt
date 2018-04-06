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
@file:Suppress("ObjectPropertyName", "MemberVisibilityCanBePrivate", "unused")
package xyz.laxus.db

import com.typesafe.config.Config
import com.typesafe.config.ConfigValue
import xyz.laxus.db.schema.Columns
import xyz.laxus.db.schema.TableName
import xyz.laxus.util.*
import xyz.laxus.util.reflect.loadClass
import java.sql.Connection
import java.sql.DriverManager
import kotlin.reflect.full.findAnnotation

/**
 * @author Kaidan Gustave
 */
object Database : AutoCloseable {
    @PublishedApi
    internal val LOG = createLogger(Database::class)

    private lateinit var _connection: Connection
    val connection get() = _connection

    private val _tables = HashMap<String, Table>()
    val tables: Map<String, Table> get() = _tables

    init {
        onJvmShutdown("Database Shutdown", this::close)
    }

    fun start() {
        initializeDB(true)
    }

    fun connect(url: String, user: String, pass: String, initialize: Boolean = true) {
        LOG.info("Connecting to database: $url")
        _connection = DriverManager.getConnection(url, user, pass)

        if(initialize) {
            initializeDB(false)
        }
    }

    fun isConnected(): Boolean = ::_connection.isInitialized && !connection.isClosed

    private fun initializeDB(shouldLogin: Boolean) {
        val config = findConfig()

        val databaseNode = checkNotNull(config.config("database")) { nodeNotFound("database") }

        if(shouldLogin) {
            LOG.debug("Logging in using configuration...")

            val loginNode = checkNotNull(databaseNode.config("login")) { nodeNotFound("login") }
            val user = checkNotNull(loginNode.string("user")) { nodeNotFound("user") }
            val pass = checkNotNull(loginNode.string("pass")) { nodeNotFound("pass") }

            val urlNode = checkNotNull(databaseNode.config("url")) { nodeNotFound("url") }

            val prefix = checkNotNull(urlNode.string("prefix")) { nodeNotFound("prefix") }
            val path = checkNotNull(urlNode.string("path")) { nodeNotFound("path") }

            val options = databaseNode.obj("options") ?: emptyMap<String, ConfigValue>()

            val url = buildString {
                append("$prefix$path")
                options.keys.forEach { key ->
                    append(";")
                    append(key)
                    append("=")
                    append("${options[key]?.unwrapped()}")
                }
            }

            connect(url, user, pass, initialize = false)
        }

        val tablesList = checkNotNull(databaseNode.getList("tables")) { nodeNotFound("tables") }

        tablesList.forEach {
            val path = "${it.unwrapped()}"
            val klazz = checkNotNull(loadClass(path)) {
                "Could not find class '$it' specified in database.conf!"
            }

            val tableName = checkNotNull(klazz.findAnnotation<TableName>()?.value) {
                "$it didn't have a @TableName annotation!"
            }

            val objInst = checkNotNull(klazz.objectInstance as? Table) {
                "$it was not an object or didn't extend Table!"
            }

            val columns = klazz.findAnnotation<Columns>()?.value ?: emptyArray()
            val uniqueColumns = columns.filter { it.unique }

            val createStatement = buildString {
                append("CREATE TABLE IF NOT EXISTS $tableName(")
                columns.forEachIndexed { index, column ->
                    append("${column.name} ${column.type}")
                    val default = column.def

                    if(default == "null" || column.nullable) {
                        append(" NULL")
                    }

                    if(default.isNotBlank()) {
                        append(" DEFAULT $default")
                    }

                    if(index != columns.lastIndex || uniqueColumns.isNotEmpty()) {
                        append(", ")
                    }

                    if(index == columns.lastIndex && uniqueColumns.isNotEmpty()) {
                        append("UNIQUE(")
                        uniqueColumns.forEachIndexed { uniqueIndex, uniqueColumn ->
                            append(uniqueColumn.name)
                            if(uniqueIndex != uniqueColumns.lastIndex) {
                                append(", ")
                            }
                        }
                        append(")")
                    }
                }
                append(")")
            }

            connection.createStatement().use {
                it.execute(createStatement)
            }

            _tables[tableName] = objInst
        }
    }

    private fun findConfig(): Config {
        return checkNotNull(loadConfig("database.conf").takeIf { it.isResolved }){
            "Could not find 'database.conf' in resources!"
        }
    }

    private fun nodeNotFound(node: String): String = "Could not find '$node' node!"

    override fun close() {
        if(!::_connection.isInitialized) return
        if(connection.isClosed) return

        try {
            connection.close()
        } catch(t: Throwable) {
            LOG.warn("Experienced an exception while closing JDBC connection:\n$t")
        }
    }
}
