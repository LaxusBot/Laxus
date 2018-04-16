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
package xyz.laxus.annotation

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

/**
 * Marks an entity as "experimental" meaning it
 * is subject to change or removal.
 *
 * @param details The details of the experimental status.
 */
@MustBeDocumented
@SinceKotlin("1.2")
@Retention(SOURCE)
@Target(
    CLASS, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER,
    FILE, FIELD, ANNOTATION_CLASS, TYPE_PARAMETER, VALUE_PARAMETER,
    CONSTRUCTOR
)
annotation class Experimental(val details: String = "")

/**
 * Marks a class that extends a supertype as an
 * internal implementation of that supertype.
 */
@MustBeDocumented
@Retention(SOURCE)
@Target(CLASS)
annotation class Implementation