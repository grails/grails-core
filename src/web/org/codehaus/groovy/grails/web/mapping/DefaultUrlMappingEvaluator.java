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
package org.codehaus.groovy.grails.web.mapping;

import groovy.lang.*;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.codehaus.groovy.grails.plugins.support.aware.ClassLoaderAware;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>A UrlMapping evaluator that evaluates Groovy scripts that are in the form:</p>
 *
 * <pre>
 * <code>
 * mappings {
 *    /$post/$year?/$month?/$day?" {  
 *       controller = "blog"
 *       action = "show"
 *       constraints {
 *           year(matches:/\d{4}/)
 *           month(matches:/\d{2}/)
 *       }
 *    }
 * }
 * </code>
 * </pre>
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *
 *        <p/>
 *        Created: Mar 5, 2007
 *        Time: 5:45:32 PM
 */
public class DefaultUrlMappingEvaluator implements UrlMappingEvaluator, ClassLoaderAware {

    private GroovyClassLoader classLoader = new GroovyClassLoader();
    private UrlMappingParser urlParser = new DefaultUrlMappingParser();

    public List evaluateMappings(Resource resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.getInputStream();
            return evaluateMappings(classLoader.parseClass(inputStream));
        } catch (IOException e) {
            throw new UrlMappingException("Unable to read mapping file ["+resource.getFilename()+"]: " + e.getMessage(), e);
        }
        finally {
            if(inputStream!= null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public List evaluateMappings(Class theClass) {
        GroovyObject obj = (GroovyObject)BeanUtils.instantiateClass(theClass);
        
        if(obj instanceof Script) {
            Script script = (Script)obj;
            Binding b = new Binding();
            
            MappingCapturingClosure closure = new MappingCapturingClosure(script);
            b.setVariable("mappings", closure);
            script.setBinding(b);

            script.run();

            Closure mappings = closure.getMappings();

            UrlMappingBuilder builder = new UrlMappingBuilder(script.getBinding());
            mappings.setDelegate(builder);            
            mappings.call();
            builder.urlDefiningMode = false;

            configureUrlMappingDynamicObjects(script);

            return builder.getUrlMappings();
        }
        else {
            throw new UrlMappingException("Unable to configure URL mappings for class ["+theClass+"]. A URL mapping must be an instance of groovy.lang.Script.");
        }
    }
    
    public List evaluateMappings(Closure closure) {
    	UrlMappingBuilder builder = new UrlMappingBuilder();
    	closure.setDelegate(builder);
    	closure.call();
    	builder.urlDefiningMode = false;
    	List mappings = builder.getUrlMappings();
    	configureUrlMappingDynamicObjects(closure);
    	return mappings;
    }

    private void configureUrlMappingDynamicObjects(Script script) {
        GrailsPluginManager manager = PluginManagerHolder.getPluginManager();
        if(manager != null) {
            GrailsPlugin controllerPlugin = manager.getGrailsPlugin("controllers");
            GroovyObject pluginInstance = controllerPlugin.getInstance();

            pluginInstance.invokeMethod("registerCommonObjects", GrailsMetaClassUtils.getExpandoMetaClass(script.getClass()));

        }
    }
    
    private void configureUrlMappingDynamicObjects(Object object) {
        GrailsPluginManager manager = PluginManagerHolder.getPluginManager();
        if(manager != null) {
            GrailsPlugin controllerPlugin = manager.getGrailsPlugin("controllers");
            GroovyObject pluginInstance = controllerPlugin.getInstance();

            pluginInstance.invokeMethod("registerCommonObjects", GrailsMetaClassUtils.getExpandoMetaClass(object.getClass()));

        }
    }

    public void setClassLoader(ClassLoader classLoader) {
        if(classLoader instanceof GroovyClassLoader) {
            this.classLoader = (GroovyClassLoader)classLoader;
        }
        else {
            throw new IllegalArgumentException("Property [classLoader] must be an instance of GroovyClassLoader");
        }
    }

    /**
     * A Closure that captures a call to a method that accepts a single closure
     */
    class MappingCapturingClosure extends Closure {

        private Closure mappings;
        public Closure getMappings() {
            return mappings;
        }
        public MappingCapturingClosure(Object o) {
            super(o);
        }
        public Object call(Object[] args) {
            if(args.length > 0 && (args[0] instanceof Closure)) {
                this.mappings = (Closure)args[0];
            }
            return null;
        }
    }

    /**
     * <p>A modal builder that constructs a UrlMapping instances by executing a closure. The class overrides
     * getProperty(name) and allows the substitution of GString values with the * wildcard.
     *
     * <p>invokeMethod(methodName, args) is also overriden for the creation of each UrlMapping instance
     */
    class UrlMappingBuilder extends GroovyObjectSupport {
        private static final String CAPTURING_WILD_CARD = "(*)";
        private static final String SLASH = "/";
        private static final String CONSTRAINTS = "constraints";

        private boolean urlDefiningMode = true;
        private List previousConstraints = new ArrayList();
        private List urlMappings = new ArrayList();
        private Binding binding;
        private String actionName = null;
        private String controllerName = null;

        public UrlMappingBuilder(Binding b) {
        	this.binding = b;
        }
        
        public UrlMappingBuilder() {
        	this.binding = null;
        }

        public List getUrlMappings() {
            return urlMappings;
        }

        public Object getProperty(String name) {
            if(urlDefiningMode) {                  
                previousConstraints.add(new ConstrainedProperty(UrlMapping.class, name, String.class));
                return CAPTURING_WILD_CARD;
            }
            else {
                return super.getProperty(name);
            }
        }
        
        public void setAction(String action) {
        	actionName = action;
        }
        public String getAction() {
        	return actionName;
        }
        
        public void setController(String controller) {
        	controllerName = controller;
        }
        public String getController() {
        	return controllerName;
        }

        public Object invokeMethod(String methodName, Object arg) {
        	if (binding == null) {
        		return invokeMethodClosure(methodName, arg);
        	}
        	return invokeMethodScript(methodName, arg);
        }

		private Object invokeMethodScript(String methodName, Object arg) {
            Object[] args = (Object[])arg;
            if(methodName.startsWith(SLASH)) {
                try {
                    urlDefiningMode = false;
                    if(args.length > 0 && (args[0] instanceof Closure)) {
                        UrlMappingData urlData = urlParser.parse(methodName);
                        Closure callable = (Closure)args[0];                        
                        callable.call();

                        Object controllerName = binding.getVariables().get(GrailsControllerClass.CONTROLLER);
                        Object actionName = binding.getVariables().get(GrailsControllerClass.ACTION);

                        ConstrainedProperty[] constraints = (ConstrainedProperty[])previousConstraints.toArray(new ConstrainedProperty[previousConstraints.size()]);
                        UrlMapping urlMapping = new RegexUrlMapping(urlData, controllerName, actionName, constraints);
                        urlMappings.add(urlMapping);
                        return urlMapping;
                    }
                    else {
                        throw new UrlMappingException("No controller or action defined for URL mapping ["+ methodName +"]");
                    }
                }
                finally {
                    binding.getVariables().remove(GrailsControllerClass.CONTROLLER);
                     binding.getVariables().remove(GrailsControllerClass.ACTION);
                    previousConstraints.clear();
                    urlDefiningMode = true;
                }
            }
            else if(!urlDefiningMode && CONSTRAINTS.equals(methodName)) {
                ConstrainedPropertyBuilder builder = new ConstrainedPropertyBuilder(this);
                if(args.length > 0 && (args[0] instanceof Closure)) {

                    Closure callable = (Closure)args[0];
                    callable.setDelegate(builder);
                    for (Iterator i = previousConstraints.iterator(); i.hasNext();) {
                        ConstrainedProperty constrainedProperty = (ConstrainedProperty) i.next();
                        builder.getConstrainedProperties().put(constrainedProperty.getPropertyName(), constrainedProperty);
                    }
                    callable.call();
                }
                return builder.getConstrainedProperties();
            }
            else {
                return super.invokeMethod(methodName, arg);
            }
        }

        private Object invokeMethodClosure(String methodName, Object arg) {
            Object[] args = (Object[])arg;
            if(methodName.startsWith(SLASH)) {
                try {
                    urlDefiningMode = false;
                    if(args.length > 0 && (args[0] instanceof Closure)) {
                        UrlMappingData urlData = urlParser.parse(methodName);
                        Closure callable = (Closure)args[0];     
                        callable.setDelegate(this);
                        callable.call();

                        ConstrainedProperty[] constraints = (ConstrainedProperty[])previousConstraints.toArray(new ConstrainedProperty[previousConstraints.size()]);
                        
                        UrlMapping urlMapping = new RegexUrlMapping(urlData, controllerName, actionName, constraints);
                        
                        urlMappings.add(urlMapping);
                        
                        return urlMapping;
                    }
                    else {
                        throw new UrlMappingException("No controller or action defined for URL mapping ["+ methodName +"]");
                    }
                }
                finally {
                	controllerName = null;
                	actionName=null;
                	previousConstraints.clear();
                    urlDefiningMode = true;
                }
            }
            else if(!urlDefiningMode && CONSTRAINTS.equals(methodName)) {
                ConstrainedPropertyBuilder builder = new ConstrainedPropertyBuilder(this);
                if(args.length > 0 && (args[0] instanceof Closure)) {

                    Closure callable = (Closure)args[0];
                    callable.setDelegate(builder);
                    for (Iterator i = previousConstraints.iterator(); i.hasNext();) {
                        ConstrainedProperty constrainedProperty = (ConstrainedProperty) i.next();
                        builder.getConstrainedProperties().put(constrainedProperty.getPropertyName(), constrainedProperty);
                    }
                    callable.call();
                }
                return builder.getConstrainedProperties();
            }
            else {
                return super.invokeMethod(methodName, arg);
            }
        }
    }
}
