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
package org.grails.databinding.events;

import org.grails.databinding.errors.BindingError;

/**
 * A listener which will be notified of events generated during data binding
 * @author Jeff Brown
 * @since 2.3
 */
public interface DataBindingListener {
    
    /**
     * Called when data binding is about to imposed on a property
     * 
     * @param obj The object data binding is being imposed upon
     * @param propertyName The name of the property being bound to
     * @param value The value of the property being bound
     * @return true if data binding should continue, otherwise return false
     */
    Boolean beforeBinding(Object obj, String propertyName, Object value);

    /**
     * Called after data binding has been imposed on a property
     *  
     * @param obj The object data binding is being imposed upon
     * @param propertyName The name of the property that was bound to
     */
    void afterBinding(Object obj, String propertyName);

    /**
     * Called when an error occurs binding to a property
     * @param error encapsulates information about the binding error
     * @see BindingError
     */
    void bindingError(BindingError error);
}
