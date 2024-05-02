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
package grails.web.mapping

import grails.web.CamelCaseUrlConverter
import grails.web.mapping.LinkGenerator
import grails.web.mapping.LinkGeneratorFactory
import grails.web.mapping.UrlMappings
import grails.web.mapping.UrlMappingsFactory
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.grails.web.util.WebUtils
import org.springframework.mock.web.MockServletContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
abstract class AbstractUrlMappingsSpec extends Specification{

    static final String CONTEXT_PATH = 'app-context'

    def setup() {
        WebUtils.clearGrailsWebRequest()
    }

    LinkGenerator getLinkGeneratorWithContextPath(Closure mappings) {
        LinkGeneratorFactory linkGeneratorFactory = new LinkGeneratorFactory()
        linkGeneratorFactory.contextPath = CONTEXT_PATH
        linkGeneratorFactory.create(mappings)
    }
    LinkGenerator getLinkGenerator(Closure mappings) {
        new LinkGeneratorFactory().create(mappings)
    }
    UrlMappings getUrlMappingsHolder(Closure mappings) {
        new UrlMappingsFactory().create(mappings)
    }
}
