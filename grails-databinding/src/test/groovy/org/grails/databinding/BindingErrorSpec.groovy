/* Copyright 2013 the original author or authors.
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
package org.grails.databinding

import org.grails.databinding.errors.BindingError
import org.grails.databinding.events.DataBindingListenerAdapter

import spock.lang.Specification

class BindingErrorSpec extends Specification {

    void 'Test conversion error'() {
        given:
        def binder = new SimpleDataBinder()
        def box = new Box()
        def listener = new BoxBindingListener()

        when:
        binder.bind box, new SimpleMapDataBindingSource([width: 42, height: 'nine']), listener

        then:
        box.width == 42
        box.height == null
        listener.errors.size() == 1
        listener.errors[0].rejectedValue == 'nine'
        listener.errors[0].propertyName == 'height'
    }
}

class BoxBindingListener extends DataBindingListenerAdapter {

    def errors = []

    @Override
    void bindingError(BindingError error) {
        errors << error
    }
}

class Box {
    Integer width
    Integer height
}
