/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.scaffolding;

import org.codehaus.groovy.grails.commons.*;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * Contains utility methods for configuration scaffolding
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 6:31:47 PM
 */
public class GrailsScaffoldingUtil {

    /**
     * Configures all the scaffolders registered within the ApplicationContext. Associating the name of the
     * domain class identity property with the scaffold domain and setting a reference to the Validator
     *
     * @param application The GrailsApplication instance
     * @param appContext The ApplicationContext
     */
    public static void configureScaffolders(GrailsApplication application, ApplicationContext appContext) {
        GrailsClass[] controllerClasses = application.getArtefacts(ControllerArtefactHandler.TYPE);
        for (int i = 0; i < controllerClasses.length; i++) {
            GrailsControllerClass controllerClass = (GrailsControllerClass) controllerClasses[i];
            if(controllerClass.isScaffolding()) {
                try {
                    GrailsScaffolder gs = (GrailsScaffolder)appContext.getBean(controllerClass.getFullName() + "Scaffolder");
                    if(gs != null) {
                        ScaffoldDomain sd = gs.getScaffoldRequestHandler()
                                                .getScaffoldDomain();

                        GrailsDomainClass dc = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,
                            sd.getPersistentClass().getName());
                        if(dc != null) {
                            sd.setIdentityPropertyName(dc.getIdentifier().getName());
                            sd.setValidator(dc.getValidator());
                        }
                    }
                } catch (NoSuchBeanDefinitionException e) {
                    // ignore
                }
            }
        }
    }
}
