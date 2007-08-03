/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.web.converters;

/**
 * A utility class for creating and dealing with Converter objects
 *
 * @author Siegfried Puchbauer
 * @since 0.6
 *        <p/>
 *        Created: Aug 3, 2007
 *        Time: 6:10:44 PM
 */

import groovy.lang.Closure;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.hsqldb.lib.Collection;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class ConverterUtil {

    private static ConverterUtil INSTANCE;

    public static Object createConverter(Class converterClass, Object target) throws ConverterException {
        try {
            AbstractConverter converter = (AbstractConverter) converterClass.newInstance();
            converter.setTarget(target);
            return converter;
        } catch (Exception e) {
            throw new ConverterException("Initialization of Converter Object " + converterClass.getName()
                    + " failed for target " + target.getClass().getName(), e);
        }
    }

    public static GrailsDomainClass getDomainClass(String name) {
        return (GrailsDomainClass) getGrailsApplication().getArtefact(DomainClassArtefactHandler.TYPE, name);
    }

    private static GrailsApplication getGrailsApplication() {
        return getInstance().grailsApplication;
    }

    protected static ConverterUtil getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConverterUtil();
        }
        return INSTANCE;
    }

    public static boolean isConverterClass(Class clazz) {
        return Converter.class.isAssignableFrom(clazz);
    }

    public static boolean isDomainClass(Class clazz) {
        return getGrailsApplication().isArtefactOfType(DomainClassArtefactHandler.TYPE, clazz);
    }

    public static Set getDomainClassNames() {
        return getGrailsApplication().getArtefactInfo(DomainClassArtefactHandler.TYPE).getClassesByName().keySet();
    }

    public static void setGrailsApplication(GrailsApplication grailsApp) {
        getInstance().grailsApplication = grailsApp;
    }

    private GrailsApplication grailsApplication;

    protected ConverterUtil() {

    }

    public static Object invokeOriginalAsTypeMethod(Object delegate, Class clazz) {
        if (delegate instanceof Collection)
            return DefaultGroovyMethods.asType((Collection) delegate, clazz);
        else if (delegate instanceof Closure)
            return DefaultGroovyMethods.asType((Closure) delegate, clazz);
        else if (delegate instanceof Map)
            return DefaultGroovyMethods.asType((Map) delegate, clazz);
        else if (delegate instanceof Number)
            return DefaultGroovyMethods.asType((Number) delegate, clazz);
        else if (delegate instanceof File)
            return DefaultGroovyMethods.asType((File) delegate, clazz);
        else if (delegate instanceof String)
            return DefaultGroovyMethods.asType((String) delegate, clazz);
        else
            return DefaultGroovyMethods.asType(delegate, clazz);
    }
}

