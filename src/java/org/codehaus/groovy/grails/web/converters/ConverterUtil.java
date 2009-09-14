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
 */

import groovy.lang.Closure;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ConverterUtil {

    private final static Log log = LogFactory.getLog(ConverterUtil.class);

    private static ConverterUtil INSTANCE;

    private static final String PERSISTENCE_BEAN_WRAPPER_CLASS = "org.codehaus.groovy.grails.orm.hibernate.support.HibernateBeanWrapper";
    
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static BeanWrapper createBeanWrapper(Object o) {
        BeanWrapper beanWrapper;
        try {
            Class c = Class.forName(PERSISTENCE_BEAN_WRAPPER_CLASS);
            Constructor init = c.getConstructor(new Class[]{Object.class});
            beanWrapper = (BeanWrapper)init.newInstance(new Object[]{o});
        } catch (Exception e) {
            beanWrapper = new BeanWrapperImpl(o);
        }
        return beanWrapper;
    }

    /**
     * Reads the value of the primary identifier property of a domain class instance
     *
     * @param domainObject The domain class instance
     * @param idProperty   The GrailsDomainClassProperty
     * @return The primary identifier of the specified domain object
     */
    protected Object extractIdValue(Object domainObject, GrailsDomainClassProperty idProperty) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainObject);
        return beanWrapper.getPropertyValue(idProperty.getName());
    }



    public static Object createConverter(Class converterClass, Object target) throws ConverterException {
        return createConverter(converterClass, target, null);
    }

    public static Object createConverter(Class converterClass, Object target, ApplicationContext applicationContext) throws ConverterException{
        try {
            AbstractConverter converter = (AbstractConverter) converterClass.newInstance();
            if(converter instanceof ApplicationContextAware && applicationContext!=null) {
                ((ApplicationContextAware)converter).setApplicationContext(applicationContext);
            }
            converter.setTarget(target);
            return converter;
        } catch (Exception e) {
            throw new ConverterException("Initialization of Converter Object " + converterClass.getName()
                    + " failed for target " + target.getClass().getName(), e);
        }
    }

//      TODO: REMOVE commented Code!

//    private static Map XSTREAM_MAP = new HashMap();
//    private static Map ALIAS_MAP = new HashMap();

//    public static XStream
//    getXStream(Class clazz) {
//        XStream xs = (XStream) XSTREAM_MAP.get(clazz);
//        if (xs == null) {
//            xs = setupXStream(clazz);
//        }
//        return xs;
//    }

//    protected static XStream setupXStream(Class converter) {
//        XStream xs = new XStream();
//
//        xs.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
//        for (Iterator iterator = ALIAS_MAP.keySet().iterator(); iterator.hasNext();) {
//            Class cls = (Class) iterator.next();
//            String alias = (String) ALIAS_MAP.get(cls);
//            xs.alias(alias, cls);
//        }
//        xs.aliasType("errors", Errors.class);
//        try {
//            XStreamXML instance = (XStreamXML) converter.newInstance();
//            instance.configureXStream(xs);
//
//            XSTREAM_MAP.put(converter, xs);
//        } catch (Exception e) {
//            log.error("Error configuring XStream for Converter " + converter.getName(), e);
//        }
//        return xs;
//    }

//    public static void addAlias(String alias, Class clazz) {
//        ALIAS_MAP.put(clazz, alias);
//        XSTREAM_MAP.clear();
//    }
//
//    public static String getAlias(Class cls) {
//        return (String) ALIAS_MAP.get(cls);
//    }
//
//    public static String resolveAlias(Class cls) {
//        String alias = (String) ALIAS_MAP.get(cls);
//        if (alias == null) return cls.getName();
//        else return alias;
//    }
//
    public static GrailsDomainClass getDomainClass(String name) {
        // deal with proxies
        name = trimProxySuffix(name);
        return (GrailsDomainClass) getGrailsApplication().getArtefact(DomainClassArtefactHandler.TYPE, name);
    }

    private static String trimProxySuffix(String name) {
        int i = name.indexOf("$$");
        if(i > -1) {
            name = name.substring(0,i);
        }
        return name;
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
        String name = trimProxySuffix(clazz.getName());
        return getGrailsApplication() != null && getGrailsApplication().isArtefactOfType(DomainClassArtefactHandler.TYPE, name);
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
    	if(clazz.isInstance(delegate))
    		return delegate;
    	else if(delegate instanceof Collection && clazz.isArray()) {
    		if(clazz.getComponentType() == Object.class) {
    			int size=((Collection)delegate).size();
    			if(size==0) {
    				return EMPTY_OBJECT_ARRAY;
    			} else {
    				return ((Collection)delegate).toArray((Object[])Array.newInstance(clazz.getComponentType(), size));
    			}
    		} else {
    			return DefaultTypeTransformation.asArray(delegate, clazz);
    		}
    	} else  if (delegate instanceof Collection)
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

    public static ConverterException resolveConverterException(Throwable t) {
        return t instanceof ConverterException ? (ConverterException) t : new ConverterException(t);
    }

    public static Converter.CircularReferenceBehaviour resolveCircularReferenceBehaviour(String str) {
        return Converter.CircularReferenceBehaviour.valueOf(str);
    }
}

