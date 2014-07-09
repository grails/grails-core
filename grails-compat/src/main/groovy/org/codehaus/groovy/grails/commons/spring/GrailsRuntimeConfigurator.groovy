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
package org.codehaus.groovy.grails.commons.spring

import grails.spring.BeanBuilder
import groovy.transform.CompileStatic
import grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.grails.core.legacy.LegacyGrailsApplication
import org.grails.spring.RuntimeSpringConfigUtilities
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext

/**
 * Handles the runtime configuration of the Grails ApplicationContext.
 *
 * @author Graeme Rocher
 * @since 0.3
 * @deprecated Use {@link org.grails.web.servlet.context.support.GrailsRuntimeConfigurator} instead
 */
@CompileStatic
@Deprecated
class GrailsRuntimeConfigurator extends org.grails.web.servlet.context.support.GrailsRuntimeConfigurator {
    GrailsRuntimeConfigurator(GrailsApplication application) {
        super(((LegacyGrailsApplication)application).grailsApplication)
    }

    GrailsRuntimeConfigurator(GrailsApplication application, ApplicationContext parent) {
        super((((LegacyGrailsApplication)application).grailsApplication), parent)
    }

    GrailsRuntimeConfigurator(GrailsApplication application, ApplicationContext parent, GrailsPluginManager pluginManager) {
        super((((LegacyGrailsApplication)application).grailsApplication), parent, pluginManager)
    }

    /**
     * Loads any external Spring configuration into the given RuntimeSpringConfiguration object.
     * @param config The config instance
     */
    public static void loadExternalSpringConfig(RuntimeSpringConfiguration config, final GrailsApplication application) {
        RuntimeSpringConfigUtilities.loadExternalSpringConfig(config, (((LegacyGrailsApplication)application).grailsApplication));
    }

    public static BeanBuilder reloadSpringResourcesConfig(RuntimeSpringConfiguration config, GrailsApplication application, Class<?> groovySpringResourcesClass) throws InstantiationException, IllegalAccessException {
        return RuntimeSpringConfigUtilities.reloadSpringResourcesConfig(config, (((LegacyGrailsApplication)application).grailsApplication), groovySpringResourcesClass);
    }

    public static void loadSpringGroovyResources(RuntimeSpringConfiguration config, GrailsApplication application) {
        RuntimeSpringConfigUtilities.loadExternalSpringConfig(config, (((LegacyGrailsApplication)application).grailsApplication));
    }

    public static void loadSpringGroovyResourcesIntoContext(RuntimeSpringConfiguration config, GrailsApplication application,
                                                            GenericApplicationContext context) {
        RuntimeSpringConfigUtilities.loadSpringGroovyResourcesIntoContext(config, (((LegacyGrailsApplication)application).grailsApplication), context);
    }

    /**
     * Resets the GrailsRumtimeConfigurator.
     */
    public static void reset() {
        RuntimeSpringConfigUtilities.reset();
    }

}
