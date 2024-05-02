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
package org.grails.web.servlet.view

import grails.util.GrailsWebMockUtil
import org.grails.core.io.MockStringResourceLoader
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.support.MockApplicationContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestContextHolder

import static org.junit.jupiter.api.Assertions.assertEquals

@SuppressWarnings("unused")
class GroovyPageViewTests {

    @Test
    void testGroovyPageView() {
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()

        def rl = new MockStringResourceLoader()

        def url = "/WEB-INF/grails-app/views/test.gsp"

        rl.registerMockResource(url, "<%='success'+foo%>")

        def gpte = new GroovyPagesTemplateEngine()
        gpte.afterPropertiesSet()

        gpte.groovyPageLocator.addResourceLoader(rl)

        def ctx = new MockApplicationContext()
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)

        def view = new GroovyPageView()
        view.url = url
        view.applicationContext = ctx
        view.templateEngine = gpte
        view.afterPropertiesSet()

        def model = [foo:"bar"]
        view.render(model, webRequest.currentRequest, webRequest.currentResponse)

        assertEquals "successbar", webRequest.currentResponse.contentAsString
    }

    @AfterEach
    void tearDown() {
         RequestContextHolder.resetRequestAttributes()
    }
}
