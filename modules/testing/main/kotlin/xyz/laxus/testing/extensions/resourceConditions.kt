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
package xyz.laxus.testing.extensions

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import xyz.laxus.testing.utility.evaluateDisabled
import xyz.laxus.testing.utility.evaluateEnabled
import java.lang.reflect.GenericDeclaration
import java.lang.reflect.Method

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(EnabledIfResourcePresentCondition::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class EnabledIfResourcePresent(val named: String)

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(EnabledIfResourcePresentCondition::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class EnabledIfResourcesPresent(vararg val names: String)

class EnabledIfResourcePresentCondition: ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val target = context.testClass.orElse(null) ?: context.testMethod.orElse(null) ?: return evaluateEnabled()
        target.getAnnotation(EnabledIfResourcePresent::class.java)?.let { annotation ->
            return resourceIsPresent(target.targetClass(), annotation.named) ?:
                   evaluateEnabled("Resource named '${annotation.named}' is present.")
        }

        target.getAnnotation(EnabledIfResourcesPresent::class.java)?.let { annotation ->
            annotation.names.forEach { name ->
                if(resourceIsPresent(target.targetClass(), name) != null) {
                    return evaluateDisabled("Resource named '$name' is not present.")
                }
            }
            return evaluateEnabled("All resources are present.")
        }

        return evaluateEnabled()
    }

    private companion object {
        private fun GenericDeclaration.targetClass(): Class<*> = this as? Class<*> ?: (this as Method).declaringClass
        private fun resourceIsPresent(target: Class<*>, name: String): ConditionEvaluationResult? {
            target.getResource(name) ?: return evaluateDisabled("Resource named '$name' is not present.")
            return null
        }
    }
}
