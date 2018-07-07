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
package xyz.laxus.auto.listener

/**
 * Marks a `class` to generate a companion source file that handles and redirects events
 * properly for it.
 *
 * Generated files implement [EventListener][net.dv8tion.jda.core.hooks.EventListener].
 * Methods are also automatically targeted based on a set of properties and traits similar
 * to methods found in [ListenerAdapter][net.dv8tion.jda.core.hooks.ListenerAdapter]:
 *
 *  * The method must be `public`.
 *  * The method must return `void`.
 *  * The method must have a single parameter that is a subclass of [Event][net.dv8tion.jda.core.events.Event].
 *  * The method is not marked with a [@NoEvent][NoEvent] annotation.
 *
 *
 * **Note:** The generated source file will have a name in the format of `XListener` where
 * `X` is the name of the class that has this annotation applied, unless [AutoListener.value]
 * is specified.
 *
 * @author Kaidan Gustave
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoListener(
    /**
     * Target name of the generated source.
     *
     * Developers may choose to implement this personally to avoid
     * naming conflicts with previously existing or future resources/classes.
     *
     * If left unset or provided blank, the generated class will default
     * to `XListener` where `X` is the name of the class this
     * annotation is applied to.
     *
     * @return The generated class name, or blank if it's default.
     */
    val value: String = ""
)