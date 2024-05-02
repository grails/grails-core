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
package grails.ui.support

import grails.util.BuildSettings
import groovy.transform.InheritConstructors
import org.springframework.mock.web.MockServletConfig
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.support.GenericWebApplicationContext


/**
 * A {@link org.springframework.web.context.WebApplicationContext} used during development
 * for things like running scripts and loading the console UI
 *
 * @author Graeme Rocher
 * @since
 */
@InheritConstructors
class DevelopmentWebApplicationContext extends GenericWebApplicationContext {

    DevelopmentWebApplicationContext() {
        def context = new MockServletContext("${BuildSettings.BASE_DIR}/src/main/webapp")
        setServletContext(context)
        setServletConfig(new MockServletConfig(context))
    }
}
