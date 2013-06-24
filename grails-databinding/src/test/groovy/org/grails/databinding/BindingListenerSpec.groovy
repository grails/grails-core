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

import org.grails.databinding.events.DataBindingListenerAdapter

import spock.lang.Specification

class BindingListenerSpec extends Specification {

    void 'Test using beforeBinding to initialize property'() {
        given:
        def binder = new SimpleDataBinder()
        def p = new Package()
        def listener = new EmployeeBindingListener()

        when:
        binder.bind p, new SimpleMapDataBindingSource([recipient: [name: 'Jenny', employeeNumber: '8675309']]), listener

        then:
        p.recipient instanceof Employee
        p.recipient.name == 'Jenny'
        p.recipient.employeeNumber == '8675309'
    }

    void 'Test afterBinding'() {
        given:
        def binder = new SimpleDataBinder()
        def p = new Person()
        def listener = new PersonBindingListener()

        when:
        binder.bind(p, new SimpleMapDataBindingSource([name: 'Phil']), listener)

        then:
        listener.valuesAfterBinding.name == 'Phil'
    }

    void 'Test that beforeBinding can return null or true to indicate that binding should proceed'() {
        given:
        def binder = new SimpleDataBinder()
        def p = new Person()
        def listener = new PersonBindingListener2()

        when:
        binder.bind p, new SimpleMapDataBindingSource([name: 'one']), listener

        then:
        p.name == 'one'

        when:
        p.name = null
        binder.bind p, new SimpleMapDataBindingSource([name: 'two']), listener

        then:
        p.name == 'two'

        when:
        p.name = null
        binder.bind p, new SimpleMapDataBindingSource([name: 'three']), listener

        then:
        p.name == null

        when:
        binder.bind p, new SimpleMapDataBindingSource([name: 'four']), listener

        then:
        p.name == null
    }
}

class PersonBindingListener2 extends DataBindingListenerAdapter {

    @Override
    Boolean beforeBinding(Object obj, String propertyName, Object value) {
        if(value == 'one') {
            return true
        } else if(value == 'two') {
            return null
        } else if(value == 'three') {
            return false
        } else {
            return false
        }
    }
}

class PersonBindingListener extends DataBindingListenerAdapter {

    def valuesAfterBinding = [:]

    @Override
    void afterBinding(Object obj, String propertyName) {
        valuesAfterBinding[propertyName] = obj[propertyName]
    }
}
class EmployeeBindingListener extends DataBindingListenerAdapter {

    @Override
    Boolean beforeBinding(Object obj, String propertyName, Object value) {
        if('recipient' == propertyName && obj['recipient'] == null) {
            obj['recipient'] = new Employee()
            return false
        }
        return true
    }
}
class Person {
    String name
}

class Employee extends Person {
    String employeeNumber
}

class Package {
    Person recipient
}
