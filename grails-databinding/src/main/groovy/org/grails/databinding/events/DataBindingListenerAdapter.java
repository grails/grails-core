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
 * @author Jeff Brown
 * @since 2.3
 */
public abstract class DataBindingListenerAdapter implements DataBindingListener {

    public Boolean beforeBinding(Object obj, String propertyName, Object value) {
        return true;
    }

    public void afterBinding(Object obj, String propertyName) {
    }

    public void bindingError(BindingError error) {
    }
}
