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
 * See the License for the specific language
 *  governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.commons.cfg

import org.springframework.core.io.DefaultResourceLoader

class ConfigurationHelperTests extends GroovyTestCase {

    def PACKAGE_PATH = "org/codehaus/groovy/grails/commons/cfg"

    void testLoadingExternalConfig() {
        def config = initConfig {
            it.grails.config.locations = [
                "classpath:${PACKAGE_PATH}/ExampleConfigScript.groovy", // a = 1
                "classpath:${PACKAGE_PATH}/ExampleConfigCompiledClass.class", // b = 1
                "classpath:${PACKAGE_PATH}/ExampleConfig.properties", // c = 1
                ExampleConfigClassObject // d = 1
            ]
        }

        ["a", "b", "c", "d"].each {
            assertEquals("merged config should contain value for key '$it'", "1", config."$it".toString())
        }
    }

    void testLoadingExternalConfigWithDefaults() {
        // load just defaults
        def config = initConfig {
            it.grails.config.defaults.locations = [
                "classpath:${PACKAGE_PATH}/ExampleConfigDefaults.groovy" // a = 2
            ]
        }
        assertEquals("value from defaults 'a'", 2, config.a)

        // load the same value in the app config, should override
        config = initConfig {
            it.a = 3
            it.grails.config.defaults.locations = [
                "classpath:${PACKAGE_PATH}/ExampleConfigDefaults.groovy" // a = 2
            ]
        }
        assertEquals("value from main config for 'a'", 3, config.a)

        // load the same value in the app config and external locations, external should win
        config = initConfig {
            it.a = 3
            it.grails.config.defaults.locations = [
                "classpath:${PACKAGE_PATH}/ExampleConfigDefaults.groovy" // a = 2
            ]
            it.grails.config.locations = [
                "classpath:${PACKAGE_PATH}/ExampleConfigScript.groovy" // a = 1
            ]
        }
        assertEquals("value from main config for 'a'", 1, config.a)
    }

    protected initConfig(Closure callback) {
        def classLoader = Thread.currentThread().contextClassLoader
        def resourceLoader = new DefaultResourceLoader(classLoader)

        def config = new ConfigObject()
        callback(config)
        ConfigurationHelper.initConfig(config, resourceLoader, classLoader)
        config
    }
}