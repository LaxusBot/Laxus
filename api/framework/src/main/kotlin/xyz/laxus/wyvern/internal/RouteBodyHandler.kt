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
package xyz.laxus.wyvern.internal

import xyz.laxus.util.collections.concurrentHashMap
import xyz.laxus.util.collections.concurrentSet
import xyz.laxus.util.createLogger
import xyz.laxus.wyvern.API
import xyz.laxus.wyvern.context.RouteContext
import xyz.laxus.wyvern.http.body.BodyProvider
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

/**
 * @author Kaidan Gustave
 */
internal object RouteBodyHandler {
    private val Log = createLogger(RouteBodyHandler::class)

    // Cached lazy body type values.
    // These are kept to reduce operations when reoccurring types
    //are returned by handlers that do not have a specific body converter
    //to assist in morphing the response.
    private val lazilyCheckedBodyValueTypes = concurrentSet<KClass<*>>()

    internal val bodyProviders: MutableMap<KClass<*>, BodyProvider<*>> = run {
        val map = concurrentHashMap<KClass<*>, BodyProvider<*>>()
        ServiceLoader.load(BodyProvider::class.java).forEach { provider ->
            provider.kotlinTypes.forEach { map[it] = provider }
        }
        return@run map
    }

    internal fun handleReturns(context: RouteContext, value: Any?) {
        // should we bother?
        if(value !== null) {
            val valueClass = value::class

            // Did they specify a charset? Or are we using our own default charset?
            val charset = context.request.contentType.charset ?: API.DefaultCharset

            // Basically, we needed a way to detect when value is a subtype of
            //a registered provider.
            // This is far from optimal, but unfortunately impossible to get
            //around, so we lazily optimize it's overhead usage.
            val provider = bodyProviders[valueClass]?.also {
                if(valueClass !in lazilyCheckedBodyValueTypes) {
                    lazilyCheckedBodyValueTypes += valueClass
                }
            } ?: run {
                // We've already checked this value class before
                //no point in checking it again.
                // This might change in the future if we allow for
                //body converters to be added/removed in runtime.
                if(valueClass in lazilyCheckedBodyValueTypes) return@run null

                // inform that we're about to look for a valid body provider
                Log.debug("Could not find provider for '$valueClass', will check supertypes!")

                // we're looking for the first valid body provider for this type
                valueClass.allSuperclasses.firstOrNull clazz@ { superClass ->
                    // fast-fail if we've already checked this type
                    if(superClass in lazilyCheckedBodyValueTypes) {
                        return@clazz false
                    } else {
                        lazilyCheckedBodyValueTypes += superClass
                    }

                    // Not already checked and this body provider
                    //isn't null, so it'll be the one we use.
                    return@clazz null !== bodyProviders[superClass]?.also {
                        // This will only run if it's going to be returned by firstOrNull.
                        // We will make sure to map it to the valid provider for future
                        //reference so we don't need to scroll through all superclasses again.
                        bodyProviders[valueClass] = it
                    }
                }?.let {
                    // Query the map with the superclass we found.
                    // This should only run if firstOrNull was successful.
                    bodyProviders[it]
                }
            }

            // There is absolutely no possible provider
            //for this value if this is null.
            val responseContentType = (provider?.contentType ?: API.DefaultContentType).withCharset(charset)
            // set the response content type
            context.response.contentType(responseContentType)
        }
    }
}