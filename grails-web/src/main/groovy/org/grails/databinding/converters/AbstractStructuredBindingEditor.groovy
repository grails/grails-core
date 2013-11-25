/*
 * Copyright 2013 the original author or authors.
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
package org.grails.databinding.converters

import groovy.transform.CompileStatic

import org.grails.databinding.StructuredBindingEditor

/**
 * An abstract base class for StructuredBindingEditor instances which can be auto-discovered
 * as beans in the Spring application context
 * 
 * @see StructuredBindingEditor
 * 
 * @since 2.3.4
 */
@CompileStatic
abstract class AbstractStructuredBindingEditor<T> implements StructuredBindingEditor<T> {
    abstract Class<? extends T> getTargetType()
}
