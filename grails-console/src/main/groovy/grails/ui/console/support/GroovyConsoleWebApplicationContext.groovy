package grails.ui.console.support

import grails.core.GrailsApplication
import grails.persistence.support.PersistenceContextInterceptor
import grails.ui.support.DevelopmentWebApplicationContext
import grails.util.BuildSettings
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.springframework.mock.web.MockServletConfig
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.support.GenericWebApplicationContext

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
 * A {@org.springframework.web.context.WebApplicationContext} for use in the embedded Grails console
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@InheritConstructors
@CompileStatic
class GroovyConsoleWebApplicationContext extends DevelopmentWebApplicationContext {

    @Override
    protected void finishRefresh() {
        super.finishRefresh()
        startConsole()
    }

    protected void startConsole() {
        Binding binding = new Binding()
        binding.setVariable("ctx", this)
        binding.setVariable(GrailsApplication.APPLICATION_ID, getBean(GrailsApplication.class))

        final GroovyConsoleWebApplicationContext self = this
        groovy.console.ui.Console groovyConsole = new groovy.console.ui.Console(binding) {
            @Override
            boolean exit(EventObject evt) {
                boolean exit = super.exit(evt)
                self.close()
                System.exit(0)
                exit
            }
        }

        def interceptors = getBeansOfType(PersistenceContextInterceptor).values()
        groovyConsole.beforeExecution = {
            for(i in interceptors) {
                i.init()
            }
        }

        groovyConsole.afterExecution = {
            for(i in interceptors) {
                i.destroy()
            }
        }
        groovyConsole.run()

    }
}
