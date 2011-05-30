/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;

/**
 * @author Graeme Rocher
 */
public interface GrailsDomainConfiguration {

    /**
     * Adds a domain class to the configuration.
     * @param domainClass
     * @return this
     */
    GrailsDomainConfiguration addDomainClass(GrailsDomainClass domainClass);

    /**
     * Sets the grails application instance.
     * @param application The grails application to use or null if none.
     */
    void setGrailsApplication(GrailsApplication application);

    /**
     * The Spring bean name of the SessionFactory.
     * @param name the name
     */
    void setSessionFactoryBeanName(String name);

    /**
     * The Spring bean name of the DataSource.
     * @param name the name
     */
    void setDataSourceName(String name);
}
