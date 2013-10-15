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
package org.codehaus.groovy.grails.web.binding;

import org.grails.databinding.events.DataBindingListenerAdapter;
import org.springframework.validation.BindingResult;

/**
 * @author Burt Beckwith
 * @since 2.3.1
 */
public abstract class DataBindingListener2Adapter extends DataBindingListenerAdapter implements DataBindingListener2 {
	public Boolean beforeBinding(Object target, BindingResult errors) {
        return true;
	}

	public void afterBinding(Object target, BindingResult errors) {
	}
}
