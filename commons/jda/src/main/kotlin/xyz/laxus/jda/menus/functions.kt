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
@file:Suppress("unused")

package xyz.laxus.jda.menus

@Menu.Dsl
inline fun paginatorBuilder(block: Paginator.Builder.() -> Unit): Paginator.Builder {
    val builder = Paginator.Builder()
    builder.block()
    return builder
}

@Menu.Dsl
inline fun orderedMenuBuilder(block: OrderedMenu.Builder.() -> Unit): OrderedMenu.Builder {
    val builder = OrderedMenu.Builder()
    builder.block()
    return builder
}

@Menu.Dsl
inline fun slideshowBuilder(block: Slideshow.Builder.() -> Unit): Slideshow.Builder {
    val builder = Slideshow.Builder()
    builder.block()
    return builder
}

@Menu.Dsl
inline fun updatingMenuBuilder(block: UpdatingMenu.Builder.() -> Unit): UpdatingMenu.Builder {
    val builder = UpdatingMenu.Builder()
    builder.block()
    return builder
}

@Menu.Dsl
inline fun paginator(
    builder: Paginator.Builder = Paginator.Builder(),
    block: Paginator.Builder.() -> Unit
): Paginator = Paginator(builder.apply(block))

@Menu.Dsl
inline fun orderedMenu(
    builder: OrderedMenu.Builder,
    block: OrderedMenu.Builder.() -> Unit
): OrderedMenu = OrderedMenu(builder.apply(block))

@Menu.Dsl
inline fun slideshow(
    builder: Slideshow.Builder,
    block: Slideshow.Builder.() -> Unit
): Slideshow = Slideshow(builder.apply(block))

@Menu.Dsl
inline fun updatingMenu(
    builder: UpdatingMenu.Builder,
    block: UpdatingMenu.Builder.() -> Unit
): UpdatingMenu = UpdatingMenu(builder.apply(block))
