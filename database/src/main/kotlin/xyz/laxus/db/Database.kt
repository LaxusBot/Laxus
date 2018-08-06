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
import xyz.laxus.db.annotation.AllPrimary
import xyz.laxus.db.annotation.Columns
import xyz.laxus.db.annotation.TableName
import xyz.laxus.util.*
import xyz.laxus.util.reflect.loadClass
import java.sql.Connection
import java.sql.DriverManager
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

/**
 * @author Kaidan Gustave
 */
object Database: AutoCloseable {
    @PublishedApi internal val log = createLogger(Database::class)

    private lateinit var _connection: Connection
    private val _tables = hashMapOf<String, Table>()

    val tables: Map<String, Table> get() = _tables
    val isConnected get() = this::_connection.isInitialized && !this._connection.isClosed
    val connection: Connection get() {
        check(isConnected) { "Database is not connected yet!" }
        return _connection
    }

    fun start(resource: String = "database.conf") {
        val config = requireNotNull(loadConfig(resource).takeIf { it.isResolved })

        if(config.boolean("database.shutdown.hook") == true) {
            createShutdownHook()
        }

        log.debug("Logging in using configuration: $resource")

        val (url, user, pass) = loginValues(config)

        log.info("Connecting to database: $url")
        _connection = DriverManager.getConnection(url, user, pass)

        config.list("database.tables")?.forEach {
            val path = "${it.unwrapped()}"
            val klazz = requireNotNull(loadClass(path)) {
                "Could not find class '$path' specified in '$resource'!"
            }

            val tableName = requireNotNull(klazz.findAnnotation<TableName>()?.value) {
                "$klazz didn't have a @TableName annotation!"
            }

            val instance = klazz.objectInstance ?: klazz.createInstance()
            val table = requireNotNull(instance as? Table) { "$klazz didn't extend Table!" }

            val columns = klazz.findAnnotation<Columns>()?.value ?: emptyArray()
            val allPrimary = klazz.findAnnotation<AllPrimary>() !== null
            val primaryColumns = if(!allPrimary) columns.filter { it.primary } else columns.toList()

            val createStatement = buildString {
                append("CREATE TABLE IF NOT EXISTS ${tableName.toLowerCase()}(")
                columns.forEachIndexed { index, column ->
                    append("${column.name.toLowerCase()} ${column.type.toUpperCase()}")
                    val default = column.def

                    if(default.toUpperCase() == "NULL" || column.nullable) {
                        append(" NULL")
                    } else {
                        append(" NOT NULL")
                    }

                    if(default.isNotBlank()) {
                        append(" DEFAULT $default")
                    }

                    if(index != columns.lastIndex || primaryColumns.isNotEmpty()) {
                        append(", ")
                    }

                    if(index == columns.lastIndex && primaryColumns.isNotEmpty()) {
                        append("PRIMARY KEY(")
                        primaryColumns.forEachIndexed { uniqueIndex, primaryColumn ->
                            append(primaryColumn.name.toLowerCase())
                            if(uniqueIndex != primaryColumns.lastIndex) {
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

            _tables[tableName] = table
        }
    }

    private fun createShutdownHook() = onJvmShutdown("Database Shutdown", this::close)

    private fun loginValues(config: Config): Triple<String, String, String> {
        val user = requireNotNull(config.string("database.login.user"))
        val pass = requireNotNull(config.string("database.login.pass"))
        val url = buildString {
            append(requireNotNull(config.string("database.url.prefix")))
            append(requireNotNull(config.string("database.url.path")))
//            config.obj("database.url.options")?.forEach { key, value ->
//                append(";$key=${value.unwrapped()}")
//            }
        }

        return Triple(url, user, pass)
    }

    override fun close() {
        if(!isConnected) return

        tables.values.forEach {
            try {
                it.close()
            } catch(t: Throwable) {
                log.warn("Experienced an exception while closing a table '${it::class}' connection:\n$t")
            }
        }

        try {
            connection.close()
        } catch(t: Throwable) {
            log.warn("Experienced an exception while closing JDBC connection:\n$t")
        }
    }
}
