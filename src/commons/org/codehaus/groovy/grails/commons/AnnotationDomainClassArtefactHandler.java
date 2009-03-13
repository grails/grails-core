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
package org.codehaus.groovy.grails.commons;

import javax.persistence.Entity;

/**
 * Detects annotated domain classes for EJB3 style mappings
 *
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Dec 6, 2007
 */
public class AnnotationDomainClassArtefactHandler extends DomainClassArtefactHandler{
    private static final String JPA_MAPPING_STRATEGY = "JPA";

    public boolean isArtefactClass(Class clazz) {
        return super.isArtefactClass(clazz) || isJPADomainClass(clazz);
    }

    public static boolean isJPADomainClass(Class clazz){
        return clazz != null && clazz.getAnnotation(Entity.class) != null;
    }

    public GrailsClass newArtefactClass(Class artefactClass) {
        GrailsDomainClass grailsClass = (GrailsDomainClass) super.newArtefactClass(artefactClass);
        grailsClass.setMappingStrategy(JPA_MAPPING_STRATEGY);        
        return grailsClass;
    }
}
