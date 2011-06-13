/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.support

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import grails.build.logging.GrailsConsole

/**
 * A shutdown hook that closes the application context when CTRL+C is hit in dev mode.
 *
 * @author Graeme Rocher
 * @since 1.1.1
 */
class DevelopmentShutdownHook implements ApplicationContextAware {

    def console = GrailsConsole.instance
    void setApplicationContext(ApplicationContext applicationContext) {
        if (System.getProperty("grails.shutdown.hook.installed")) {
            return
        }

        Runtime.runtime.addShutdownHook {
            console.verbose "Application context shutting down..."
            applicationContext.close()
            console.verbose "Application context shutdown."
        }
        System.setProperty("grails.shutdown.hook.installed", "true")
    }
}
