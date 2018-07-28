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

import xyz.laxus.db.annotation.Column
import xyz.laxus.db.annotation.Columns
import xyz.laxus.db.annotation.TableName
import xyz.laxus.db.entities.OAuth2Data
import xyz.laxus.db.sql.*
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * @author Kaidan Gustave
 */
@TableName("oauth2_sessions")
@Columns(
    Column("access_token", "VARCHAR(100)", primary = true),
    Column("refresh_token", "VARCHAR(100)"),
    Column("token_type", "VARCHAR(20)"),
    Column("expiration", "TIMESTAMP WITH TIME ZONE")
)
object DBOAuth2Session: Table() {
    fun remove(accessToken: String) {
        connection.prepare("SELECT * FROM oauth2_sessions WHERE access_token = ?") { statement ->
            statement[1] = accessToken
            statement.executeQuery {
                while(it.next()) {
                    it.deleteRow()
                }
            }
        }
    }

    fun retrieve(accessToken: String): OAuth2Data? {
        connection.prepare("SELECT * FROM oauth2_sessions WHERE access_token = ?") { statement ->
            statement[1] = accessToken
            statement.executeQuery {
                if(it.next()) {
                    return OAuth2Data(
                        accessToken = it.getString("access_token"),
                        refreshToken = it.getString("refresh_token"),
                        tokenType = it.getString("token_type"),
                        expiration = it.getTimestamp("expiration").time
                    )
                }
            }
        }

        return null
    }

    fun store(data: OAuth2Data) {
        connection.prepare("SELECT * FROM oauth2_sessions WHERE access_token = ?") { statement ->
            statement[1] = data.accessToken
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["access_token"] = data.accessToken
                    it["refresh_token"] = data.refreshToken
                    it["token_type"] = data.tokenType
                    it["expiration"] = Timestamp.valueOf(LocalDateTime.now().plusSeconds(data.expiration))
                } else it.update {
                    it["access_token"] = data.accessToken
                    it["refresh_token"] = data.refreshToken
                    it["token_type"] = data.tokenType
                    it["expiration"] = Timestamp.valueOf(LocalDateTime.now().plusSeconds(data.expiration))
                }
            }
        }
    }
}