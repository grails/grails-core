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
package org.codehaus.groovy.grails.commons.spring;

import org.springframework.context.ApplicationContext;

/**
 * <p>Static singleton holder for the Spring ApplicationContext instance</p>
 * @author Peter Ledbrook
 */
public final class ApplicationContextHolder {
    private static ApplicationContext context;

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    static void setApplicationContext(ApplicationContext context) {
        ApplicationContextHolder.context = context;
    }

    private ApplicationContextHolder() {}
}
