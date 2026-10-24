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
package grails.boot

import org.springframework.boot.WebApplicationType
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.RestoreSystemProperties

import grails.boot.config.GrailsAutoConfiguration
import grails.config.Config
import grails.core.GrailsApplication
import grails.util.Environment
import grails.util.Holders

/**
 * Verifies that an application starts from an {@code application.groovy} whose lists are immutable, and reads
 * them as it reads the lists of a YAML file: whole, by element and by an entry of an element.
 */
@RestoreSystemProperties
class GrailsAppGroovyConfigSpec extends Specification {

    @TempDir
    File configDirectory

    ConfigurableApplicationContext context

    void setup() {
        System.setProperty(Environment.KEY, Environment.TEST.getName())
    }

    void cleanup() {
        context?.close()
        Holders.clear()
        Environment.setInitializing(false)
        Environment.reset()
    }

    void 'an application starts from an application.groovy whose lists are immutable and reads them'() {
        given:
        File applicationFile = new File(configDirectory, 'application.groovy')
        applicationFile.text = '''
            app {
                names = ['one'].asImmutable()
                items = [[name: 'row'].asImmutable()].asImmutable()
            }
        '''.stripIndent()
        GrailsApp app = new GrailsApp(GroovyConfigTestApplication)
        app.webApplicationType = WebApplicationType.NONE

        when:
        context = app.run("--spring.config.location=file:${applicationFile.absolutePath}".toString())
        Config config = context.getBean(GrailsApplication).config

        then:
        config.getProperty('app.names', List) == ['one']
        config.getProperty('app.names[0]') == 'one'
        config.getProperty('app.items', List) == [[name: 'row']]
        config.getProperty('app.items[0].name') == 'row'
        context.environment.getProperty('app.names[0]') == 'one'
    }
}

class GroovyConfigTestApplication extends GrailsAutoConfiguration {
}
