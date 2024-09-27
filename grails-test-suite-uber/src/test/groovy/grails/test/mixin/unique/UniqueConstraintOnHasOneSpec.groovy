package grails.test.mixin.unique

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import groovy.transform.NotYetImplemented
import spock.lang.Ignore
import spock.lang.Specification

/*
 * Copyright 2014 original authors
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

/**
 * @author graemerocher
 */
class UniqueConstraintOnHasOneSpec extends Specification implements DomainUnitTest<Foo> {

    void "Foo's name should be unique"() {
        given:
        def foo1 = new Foo(name: "FOO1")
        def bar = new Bar(name: "BAR1")
        foo1.bar = bar
        foo1.save()
        assert Foo.count() == 1

        and:
        def foo2 = new Foo(name: "FOO1")
        foo2.bar = new Bar(name: "BAR2")

        when:
        foo2.save()

        then:
        foo2.hasErrors()
        foo2.errors['name']?.code == 'unique'
    }

    @NotYetImplemented
    void "Foo's bar should be unique, but..."() {
        given:
        def foo1 = new Foo(name: "FOO1")
        def bar = new Bar(name: "BAR")
        foo1.bar = bar
        foo1.save()
        assert Foo.count() == 1

        and:
        def foo2 = new Foo(name: "FOO2")
        foo2.bar = bar // using same Bar instance

        when:
        foo2.save()

        then:
        //foo2.hasErrors()
        foo2.errors['bar']?.code == 'unique'
    }
}

@Entity
class Bar {

    String name
    Foo foo

    static constraints = {
    }
}

@Entity
class Foo {

    String name
    static hasOne = [bar: Bar]

    static constraints = {
        name unique: true
        bar unique: true
    }
}
