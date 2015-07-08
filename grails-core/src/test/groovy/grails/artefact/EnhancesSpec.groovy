/*
 * Copyright 2015 original authors
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

package grails.artefact

import grails.compiler.traits.TraitInjector
import spock.lang.Specification


/**
 * @author graemerocher
 */
class EnhancesSpec extends Specification{
    void "Test that the enhances trait transform works as expected"() {

        when:"The generated transformer is loaded"
            def traitInjector = getClass().classLoader.loadClass("grails.artefact.FooTraitInjector").newInstance()

        then:"It is a valid trait injector"
            traitInjector instanceof TraitInjector
            traitInjector.trait == Foo
            traitInjector.artefactTypes == ['Controller'] as String[]
    }
}

@Enhances("Controller")
trait Foo {
    def bar() {
        "bar"
    }
}
