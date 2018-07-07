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
 * Marks a method that might be considered a listener method to avoid targeting when events
 * are fired in the generated [EventListener][net.dv8tion.jda.core.hooks.EventListener].
 *
 * An example of this behavior might look like this:
 *
 * ```kotlin
 * @AutoListener
 * public void MyListener
 * {
 *     // This method will be fired when a message is received.
 *     public void handleMessages(MessageReceivedEvent event)
 *     {
 *         // code
 *     }
 *
 *     // This method will not be fired when a message is received
 *     @NoEvent
 *     public void handleExternalMessages(MessageReceivedEvent event)
 *     {
 *         // code
 *     }
 * }
 * ```
 *
 * Note this is not necessary for `private` methods or methods with more than one
 * parameter, as only `public` methods that return `void` with a single parameter
 * whose type is a subclass of [Event][net.dv8tion.jda.core.events.Event] are eligible
 * to be used as auto-listener methods.
 *
 * @author Kaidan Gustave
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoEvent