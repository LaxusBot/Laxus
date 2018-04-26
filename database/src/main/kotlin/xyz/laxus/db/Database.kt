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
@file:Suppress("ObjectPropertyName", "MemberVisibilityCanBePrivate")
package xyz.laxus.db

import com.typesafe.config.Config
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
object Database: AutoCloseable {
    val Log = createLogger(Database::class)

    private lateinit var _connection: Connection
    private val _tables = HashMap<String, Table>()

    val tables: Map<String, Table> get() = _tables
    val connection: Connection get() {
        if(!isConnected()) initializeDB()
        return _connection
    }

    fun start() = initializeDB()
    fun isConnected(): Boolean = ::_connection.isInitialized && !_connection.isClosed

    private fun connect(url: String, user: String, pass: String) {
        Log.info("Connecting to database: $url")
        _connection = DriverManager.getConnection(url, user, pass)
    }

    private fun initializeDB() {
        val config = findConfig()
        val databaseNode = checkNotNull(config.config("database")) { nodeNotFound("database") }
        val shutdownHook = databaseNode.boolean("shutdown.hook") ?: false

        if(shutdownHook) {
            createShutdownHook()
        }

        Log.debug("Logging in using configuration...")
        val (url, user, pass) = databaseNode.dbValues()
        connect(url, user, pass)

        val tablesList = checkNotNull(databaseNode.list("tables")) { nodeNotFound("tables") }
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

    private fun createShutdownHook() = onJvmShutdown("Database Shutdown", this::close)

    private fun findConfig(): Config {
        return checkNotNull(loadConfig("database.conf").takeIf { it.isResolved }) {
            "Could not find 'database.conf' in resources!"
        }
    }

    private fun Config.dbValues(): Triple<String, String, String> {
        val user = checkNotNull(string("login.user")) { nodeNotFound("login.user") }
        val pass = checkNotNull(string("login.pass")) { nodeNotFound("login.pass") }
        val prefix = checkNotNull(string("url.prefix")) { nodeNotFound("url.prefix") }
        val path = checkNotNull(string("url.path")) { nodeNotFound("url.path") }
        val url = buildString {
            append("$prefix$path")
            obj("options")?.entries?.forEach {
                append(";")
                append(it.key)
                append("=")
                append("${it.value.unwrapped()}")
            }
        }
        return Triple(url, user, pass)
    }

    private fun nodeNotFound(node: String): String = "Could not find '$node' node!"

    override fun close() {
        if(!isConnected()) return

        tables.values.forEach {
            try {
                it.close()
            } catch(t: Throwable) {
                Log.warn("Experienced an exception while closing a table '${it.name}' connection:\n$t")
            }
        }

        try {
            connection.close()
        } catch(t: Throwable) {
            Log.warn("Experienced an exception while closing JDBC connection:\n$t")
        }
    }
}
