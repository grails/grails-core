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
package org.codehaus.groovy.grails.commons;

import grails.persistence.Entity;
import grails.util.ClosureToMapPopulator;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.util.ConfigObject;
import groovy.util.Eval;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware;

import java.util.Map;

/**
 * Evaluates the conventions that define a domain class in Grails
 *
 * @author Graeme Rocher
 * @author Marc Palmer (marc@anyware.co.uk)
 */
public class DomainClassArtefactHandler extends ArtefactHandlerAdapter implements GrailsConfigurationAware {

    public static final String TYPE = "Domain";
    private Map defaultConstraints;


    public DomainClassArtefactHandler() {
        super(TYPE, GrailsDomainClass.class, DefaultGrailsDomainClass.class, null);
    }


    public GrailsClass newArtefactClass(Class artefactClass) {
        if(defaultConstraints!=null) {
            return new DefaultGrailsDomainClass(artefactClass,defaultConstraints);
        }
        return new DefaultGrailsDomainClass(artefactClass);
    }

    /**
     * Sets up the relationships between the domain classes, this has to be done after
     * the intial creation to avoid looping
     */
    public void initialize(ArtefactInfo artefacts) {
        log.debug("Configuring domain class relationships");
        GrailsDomainConfigurationUtil.configureDomainClassRelationships(
            artefacts.getGrailsClasses(),
            artefacts.getGrailsClassesByName());
    }

    public boolean isArtefactClass( Class clazz ) {
        return isDomainClass(clazz);

    }

    public static boolean isDomainClass(Class clazz) {
        // its not a closure
        if(clazz == null)return false;
        if(Closure.class.isAssignableFrom(clazz)) {
            return false;
        }
        if(GrailsClassUtils.isJdk5Enum(clazz)) return false;
        if(clazz.getAnnotation(Entity.class)!=null) {
            return true;
        }
        Class testClass = clazz;
        boolean result = false;
        while(testClass!=null&&!testClass.equals(GroovyObject.class)&&!testClass.equals(Object.class)) {
            try {
                // make sure the identify and version field exist
                testClass.getDeclaredField( GrailsDomainClassProperty.IDENTITY );
                testClass.getDeclaredField( GrailsDomainClassProperty.VERSION );

                // passes all conditions return true
                result = true;
                break;
            } catch (SecurityException e) {
                // ignore
            } catch (NoSuchFieldException e) {
                // ignore
            }
            testClass = testClass.getSuperclass();
        }
        return result;
    }


    public void setConfiguration(ConfigObject co) {
        Object constraints = Eval.x(co, "x?.grails?.gorm?.default?.constraints");
        if(constraints instanceof Closure) {
            if(defaultConstraints!=null) {
                // repopulate existing map
                defaultConstraints.clear();
                new ClosureToMapPopulator(defaultConstraints).populate((Closure) constraints);
            }
            else {
                ClosureToMapPopulator populator = new ClosureToMapPopulator();
                this.defaultConstraints = populator.populate((Closure) constraints);
            }
        }

    }
}
