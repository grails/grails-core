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
package grails.ui.shell.support

import grails.core.GrailsApplication
import grails.ui.support.DevelopmentWebApplicationContext
import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.IO
import org.springframework.context.support.GenericApplicationContext


/**
 * @author Graeme Rocher
 * @since 3.0
 */
class GroovyshApplicationContext extends GenericApplicationContext {

    @Override
    protected void finishRefresh() {
        super.finishRefresh()
        startConsole()
    }

    protected void startConsole() {
        Binding binding = new Binding()
        binding.setVariable("ctx", this)
        binding.setVariable(GrailsApplication.APPLICATION_ID, getBean(GrailsApplication.class))

        final GroovyshWebApplicationContext self = this

        new Groovysh(binding, new IO()).run("")
    }
}