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
package org.grails.databinding;

import grails.databinding.SimpleDataBinder;

/**
 * StructuredBindingEditors convert structured data in a Map
 * into an object.  Typically a structured editor will pull
 * several values out of the Map that are necessary to initialize
 * the state of the object.
<pre>
class Address {
    String state
    String city
}
class StructuredAddressBindingEditor implements StructuredBindingEditor {

    public Object getPropertyValue(Object obj, String propertyName, Map<String, Object> source) {
        def address = new Address()

        address.state = source[propertyName + '_someState']
        address.city = source[propertyName + '_someCity']

        address
    }
}

def binder = new SimpleDataBinder()
binder.registerStructuredEditor Address, new StructuredAddressBindingEditor()
def resident = new Resident()
def bindingSource = [:]
bindingSource.name = 'Scott'
bindingSource.homeAddress_someCity = "Scott's Home City"
bindingSource.homeAddress_someState = "Scott's Home State"
bindingSource.workAddress_someState = "Scott's Work State"
bindingSource.workAddress = 'struct'
bindingSource.homeAddress = 'struct'

binder.bind resident, bindingSource

resident.name == 'Scott'
resident.homeAddress
assert resident.homeAddress.city == "Scott's Home City"
assert resident.homeAddress.state == "Scott's Home State"
assert resident.workAddress
assert resident.workAddress.state == "Scott's Work State"
assert resident.workAddress.city == null
</pre>
 *
 * @author Jeff Brown
 * @since 2.3
 * @see SimpleDataBinder#registerStructuredEditor(Class, StructuredBindingEditor)
 * @deprecated Use {@link grails.databinding.StructuredBindingEditor}
 */
@Deprecated
public interface StructuredBindingEditor<T> extends grails.databinding.StructuredBindingEditor<T> {
}
