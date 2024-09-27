/*
 * Copyright 2014 the original author or authors.
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
package org.grails.web.servlet

import javax.servlet.ServletContext

/**
 * An extension that adds methods to the {@link ServletContext} interface
 *
 * @author Jeff Brown
 * @since 3.0
 */
class ServletContextExtension {

    static propertyMissing(ServletContext context, String name, value) {
        context.setAttribute name, value
    }
    
    static propertyMissing(ServletContext context, String name) {
        context.getAttribute name
    }
}
