package org.grails.spring.context

import org.springframework.context.support.GenericApplicationContext
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
class ApplicationContextExtensionSpec extends Specification {

    void "Test that beans can be accessed via the dot operator thanks to the extension"() {
        given:"An application context instance"
            def ctx = new GenericApplicationContext()
            def myBean = new Object()
            ctx.beanFactory.registerSingleton("myBean", myBean)
            ctx.refresh()

        expect:"That the bean can be accessed by property access or the subscript operator"
            ctx.myBean != null
            ctx.myBean.is(myBean)
            ctx['myBean'] != null
            ctx['myBean'].is(myBean)
    }
}
