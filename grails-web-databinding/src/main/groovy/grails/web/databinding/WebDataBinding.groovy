/*
 * Copyright 2014 the original author or authors.
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
package grails.web.databinding

import groovy.transform.CompileStatic

import org.grails.web.databinding.DataBindingLazyMetaPropertyMap
import org.springframework.validation.BindingResult

/**
 *
 * @author Jeff Brown
 * @since 3.0
 *
 */
@CompileStatic
trait WebDataBinding {
    /**
     * Binds the source object to the properties of the target instance converting any types as necessary
     *
     * @param bindingSource The binding source
     * @return The BindingResult
     */
    BindingResult setProperties(final bindingSource) {
        DataBindingUtils.bindObjectToInstance(this, bindingSource)
    }

    /**
     * Returns a map of the objects properties that can be used to during binding to bind a subset of properties
     *
     * @return An instance of {
     @link DataBindingLazyMetaPropertyMap
     }
     */
    Map<?, ?> getProperties() {
        new DataBindingLazyMetaPropertyMap(this)
    }
}
