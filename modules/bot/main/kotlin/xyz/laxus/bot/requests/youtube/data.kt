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
package xyz.laxus.bot.requests.youtube

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class SearchListResponse(
    @Optional val etag: String? = null,
    @Optional val eventId: String? = null,
    @Optional val items: List<SearchResult> = emptyList(),
    @Optional val kind: String? = null,
    @Optional val nextPageToken: String? = null,
    @Optional val pageInfo: PageInfo? = null,
    @Optional val prevPageToken: String? = null,
    @Optional val regionCode: String? = null,
    @Optional val visitorId: String? = null
)

@Serializable
data class SearchResult(
    @Optional val etag: String? = null,
    @Optional val id: ResourceId? = null,
    @Optional val kind: String? = null,
    @Optional val snippet: SearchResult.Snippet? = null
) {
    @Serializable
    data class Snippet(
        @Optional val title: String? = null,
        @Optional val channelId: String? = null,
        @Optional val channelTitle: String? = null,
        @Optional val description: String? = null,
        @Optional val liveBroadcastContent: String? = null,
        @Optional val publishedAt: String? = null,
        @Optional val thumbnails: ThumbnailDetails? = null
    )
}

@Serializable
data class ResourceId(
    @Optional val channelId: String? = null,
    @Optional val kind: String? = null,
    @Optional val playlistId: String? = null,
    @Optional val videoId: String? = null
)

@Serializable
data class ThumbnailDetails(
    @Optional val default: Thumbnail? = null,
    @Optional val high: Thumbnail? = null,
    @Optional val maxres: Thumbnail? = null,
    @Optional val medium: Thumbnail? = null,
    @Optional val standard: Thumbnail? = null,
    @Optional val title: String? = null
)

@Serializable
data class Thumbnail(
    @Optional val height: Long? = null,
    @Optional val url: String? = null,
    @Optional val width: Long? = null
)

@Serializable
data class PageInfo(
    @Optional val resultsPerPage: Int? = null,
    @Optional val totalResults: Int? = null
)
