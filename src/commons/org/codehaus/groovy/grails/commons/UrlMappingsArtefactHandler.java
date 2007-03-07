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

import groovy.lang.Script;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeanUtils;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;

/**
 * A handler for UrlMappings
 *
 * @author Graeme Rocher
 * @since 0.5
 * 
 *        <p/>
 *        Created: Mar 6, 2007
 *        Time: 6:20:30 PM
 */
public class UrlMappingsArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "UrlMappings";

    public UrlMappingsArtefactHandler() {
        super(TYPE, GrailsClass.class,DefaultGrailsClass.class,TYPE);

    }
    public GrailsClass newArtefactClass(Class artefactClass) {
        try {
            Constructor c = DefaultGrailsClass.class.getDeclaredConstructor(new Class[] { Class.class, String.class } );
            return (GrailsClass)BeanUtils.instantiateClass(c, new Object[]{ artefactClass, TYPE});
        } catch (NoSuchMethodException e) {
            throw new GrailsConfigurationException("Unable to create new instance of Grails artefact ["+TYPE+"] for class ["+artefactClass+"]:" + e.getMessage(), e);
        }
    }

    public boolean isArtefactClass(Class clazz) {
        return clazz != null && (super.isArtefactClass(clazz) && Script.class.isAssignableFrom(clazz));
    }
}
