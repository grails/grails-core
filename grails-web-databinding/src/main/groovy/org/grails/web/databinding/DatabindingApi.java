/*
 * Copyright 2012 SpringSource
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
package org.grails.web.databinding;

import java.util.Map;

import org.codehaus.groovy.grails.web.binding.DataBindingUtils;
import org.springframework.validation.BindingResult;

public class DatabindingApi {
    /**
     * Binds the source object to the properties of the target instance converting any types as necessary
     *
     * @param instance The instance
     * @param bindingSource The binding source
     * @return The BindingResult
     */
    public BindingResult setProperties(final Object instance, final Object bindingSource) {
        return DataBindingUtils.bindObjectToInstance(instance, bindingSource);
    }

    /**
     * Returns a map of the objects properties that can be used to during binding to bind a subset of properties
     *
     * @param instance The instance
     * @return An instance of {@link DataBindingLazyMetaPropertyMap}
     */
    public Map<?, ?> getProperties(final Object instance) {
        return new DataBindingLazyMetaPropertyMap(instance);
    }
}
