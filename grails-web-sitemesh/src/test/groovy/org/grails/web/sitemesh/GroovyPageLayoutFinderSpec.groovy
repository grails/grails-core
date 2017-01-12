/*
 * Copyright 2012 the original author or authors.
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

package org.grails.web.sitemesh

import org.grails.web.servlet.view.GrailsViewResolver
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll
import javax.servlet.http.HttpServletRequest

/**
 * @author Colin Harrington
 */
class GroovyPageLayoutFinderSpec extends Specification {
    @Issue("https://github.com/grails/grails-core/issues/10371")
    @Unroll("Layout name mapping: #layoutName => #expectedPath")
    void "testing layout name resolution"() {
        given:
            GroovyPageLayoutFinder layoutFinder = new GroovyPageLayoutFinder()
            HttpServletRequest request = new MockHttpServletRequest()
            layoutFinder.viewResolver = Mock(GrailsViewResolver)
        when:
            layoutFinder.getNamedDecorator(request, layoutName, false)

        then:
            1 * layoutFinder.viewResolver.resolveViewName(expectedPath, Locale.ENGLISH)
            notThrown(Exception)

        where:
            layoutName         | expectedPath
            "foo"              | "/layouts/foo"
            "foo/bar"          | "/layouts/foo/bar"
            "../foo"           | "/foo"
            "../foo/bar"       | "/foo/bar"
            "../../foo"        | "/../foo"
            "../foo/../../bar" | "/../bar"
            "foo/../../bar"    | "/bar"
    }
}
