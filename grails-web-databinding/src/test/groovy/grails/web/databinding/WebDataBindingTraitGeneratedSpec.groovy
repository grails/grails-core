/*
 * Copyright 2024 original authors
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

import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method

class WebDataBindingTraitGeneratedSpec extends Specification {

    void "test that all WebDataBinding trait methods are marked as Generated"() {
        expect: "all WebDataBinding methods are marked as Generated on implementation class"
        WebDataBinding.getMethods().each { Method traitMethod ->
            assert TestWebDataBinding.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class TestWebDataBinding implements WebDataBinding {

}
