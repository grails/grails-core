/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.grails.plugins.support.aware;

import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * <p>Convenience interface that can be implemented by classes that are
 * registered by plugins.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 * @see GrailsApplication
 */
public interface GrailsApplicationAware {
    /**
     * <p>This method is called by the {@link org.springframework.context.ApplicationContext} that
     * loads the Grails application. The {@link GrailsApplication} instance that represents
     * the loaded Grails application is injected.</p>
     *
     * @param grailsApplication the {@link GrailsApplication} object that represents this Grails application
     */
    void setGrailsApplication(GrailsApplication grailsApplication);
}
