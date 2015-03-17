/*
 * Copyright 2011 SpringSource
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
package org.grails.plugins.web.controllers.api;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.util.GrailsMetaClassUtils;
import grails.util.Holders;
import grails.web.databinding.DataBindingUtils;

import java.util.Map;

import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * Enhancements made to domain classes for data binding.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class ControllersDomainBindingApi {

    public static final String AUTOWIRE_DOMAIN_METHOD = "autowireDomain";


    /**
     * Autowires the instance
     *
     * @param instance The target instance
     */
    public static void initialize(Object instance) {
        autowire(instance);
    }

    /**
     * A map based constructor that binds the named arguments to the target instance
     *
     * @param instance The target instance
     * @param namedArgs The named arguments
     */
    public static void initialize(Object instance, Map namedArgs) {
        GrailsDomainClass dc = getDomainClass(instance);
        if (dc == null) {
            DataBindingUtils.bindObjectToInstance(instance, namedArgs);
        }
        else {
            DataBindingUtils.bindObjectToDomainInstance(dc, instance, namedArgs);
            DataBindingUtils.assignBidirectionalAssociations(instance, namedArgs, dc);
        }
        autowire(instance);
    }

    private static GrailsDomainClass getDomainClass(Object instance) {
        GrailsDomainClass domainClass = GrailsMetaClassUtils.getPropertyIfExists(instance, GrailsDomainClassProperty.DOMAIN_CLASS, GrailsDomainClass.class);
        if (domainClass == null) {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup();
            if (webRequest != null) {
                ApplicationContext applicationContext = webRequest.getApplicationContext();
                if(applicationContext != null) {
                    GrailsApplication grailsApplication = applicationContext.containsBean(GrailsApplication.APPLICATION_ID) ?
                        applicationContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class) : null;
                    if (grailsApplication != null) {
                        domainClass = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, instance.getClass().getName());
                    }
                }
            }
        }

        return domainClass;
    }

    private static void autowire(Object instance) {
        final ApplicationContext applicationContext = Holders.findApplicationContext();
        final Config config = Holders.getConfig();
        boolean autowire = config != null ? config.getProperty(Settings.GORM_AUTOWIRE_INSTANCES, Boolean.class, true) : true;
        if(autowire && applicationContext != null) {
            applicationContext
                    .getAutowireCapableBeanFactory()
                    .autowireBeanProperties(instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
        }
    }
}
