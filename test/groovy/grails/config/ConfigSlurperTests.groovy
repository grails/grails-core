/* Copyright 2006-2007 Graeme Rocher
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
package grails.config
/**
 * Tests for the ConfigSlurper class
 
 * @author Graeme Rocher
 * @since 0.6
  *
 * Created: Jun 19, 2007
 * Time: 6:29:33 PM
 * 
 */

class ConfigSlurperTests extends GroovyTestCase {
    
    void testSimpleProperties() {
        def slurper = new ConfigSlurper()

        def config = slurper.parse('''
smtp.server.url = "localhost"
smtp.username = "fred"
''')

        assert config
        assertEquals "localhost", config.smtp.server.url
        assertEquals "fred", config.smtp.username
    }

    void testScopedProperties() {
        def slurper = new ConfigSlurper()
        def config = slurper.parse('''
    smtp {
        mail.host = 'smtp.myisp.com'
        mail.auth.user = 'server'
    }
    resources.URL = "http://localhost:80/resources"
        ''')

        assert config
        assertEquals "smtp.myisp.com", config.smtp.mail.host
        assertEquals "server", config.smtp.mail.auth.user
        assertEquals "http://localhost:80/resources", config.resources.URL                

    }

    void testScopedPropertiesWithNesting() {
        def slurper = new ConfigSlurper()
        def config = slurper.parse('''
    smtp {
        mail {
            host = 'smtp.myisp.com'
            auth.user = 'server'
        }
    }
    resources.URL = "http://localhost:80/resources"
        ''')

        assert config
        println config
        assertEquals "smtp.myisp.com", config.smtp.mail.host
        assertEquals "server", config.smtp.mail.auth.user
        assertEquals "http://localhost:80/resources", config.resources.URL

    }

    void testEnvironmentSpecificConfig() {
        // coming soon!
    }

}