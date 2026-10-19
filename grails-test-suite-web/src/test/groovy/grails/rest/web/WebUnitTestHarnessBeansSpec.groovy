/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package grails.rest.web

import org.springframework.web.servlet.i18n.FixedLocaleResolver
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import spock.lang.Specification

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.DefaultUrlMappingsHolder

/**
 * The URL mappings holder, link generator and locale resolver a spec defines are kept, where the web test traits defined
 * their own in their place.
 */
class WebUnitTestHarnessBeansSpec extends Specification implements ControllerUnitTest<HarnessBeansController> {

    Closure doWithSpring() {
        { ->
            grailsUrlMappingsHolder(OwnUrlMappingsHolder)
            grailsLinkGenerator(DefaultLinkGenerator, 'https://own.example')
            localeResolver(FixedLocaleResolver, Locale.JAPAN)
        }
    }

    void 'the beans a spec defines are kept'() {
        expect:
        applicationContext.getBean('grailsUrlMappingsHolder') instanceof OwnUrlMappingsHolder
        applicationContext.getBean('grailsLinkGenerator').serverBaseURL == 'https://own.example'
        applicationContext.getBean('localeResolver').resolveLocale(request) == Locale.JAPAN
    }
}

/** The locale resolver the application configures, as the i18n plugin defines it, resolves the locale of a request. */
class WebUnitTestConfiguredLocaleResolverSpec extends Specification implements ControllerUnitTest<HarnessBeansController> {

    Closure doWithConfig() {
        { config -> config['grails.i18n.localeResolver'] = 'fixed' }
    }

    void 'a web unit test resolves the locale with the locale resolver the application configures'() {
        expect:
        applicationContext.getBean('localeResolver') instanceof FixedLocaleResolver
    }
}

class WebUnitTestDefaultLocaleResolverSpec extends Specification implements ControllerUnitTest<HarnessBeansController> {

    void 'without a configured locale resolver, a web unit test resolves the locale of the session, as an application does'() {
        expect:
        applicationContext.getBean('localeResolver') instanceof SessionLocaleResolver
    }
}

@Artefact('Controller')
class HarnessBeansController { }

class OwnUrlMappingsHolder extends DefaultUrlMappingsHolder {

    OwnUrlMappingsHolder() {
        super([])
    }
}
