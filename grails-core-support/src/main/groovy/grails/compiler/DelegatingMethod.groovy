/*
 * Copyright 2015 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.compiler

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target


/**
 * Used to indicate to the compiler that a particular method simply delegates to another one. This information is useful in cases where only the final method
 * should be transformed and not the methods that delegate to the said method.
 *
 * @author Graeme Rocher
 * @since 3.0.3
 */

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD])
@interface DelegatingMethod {

}