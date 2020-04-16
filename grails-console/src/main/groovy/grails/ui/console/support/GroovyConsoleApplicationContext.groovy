package grails.ui.console.support

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.springframework.context.support.GenericApplicationContext

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
 * An {@link org.springframework.context.ApplicationContext} that loads the GroovyConsole and makes the ApplicationContext and Grails environment available to the console
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@InheritConstructors
class GroovyConsoleApplicationContext extends GenericApplicationContext {

    @Override
    protected void finishRefresh() {
        super.finishRefresh()
        startConsole()
    }

    protected void startConsole() {
        Binding binding = new Binding()
        binding.setVariable("ctx", this)
        binding.setVariable(GrailsApplication.APPLICATION_ID, getBean(GrailsApplication.class))

        final GroovyConsoleApplicationContext self = this
        groovy.console.ui.Console groovyConsole = new groovy.console.ui.Console(binding) {
            @Override
            boolean exit(EventObject evt) {
                boolean exit = super.exit(evt)
                self.close()
                exit
            }
        }
        groovyConsole.run()

    }
}
