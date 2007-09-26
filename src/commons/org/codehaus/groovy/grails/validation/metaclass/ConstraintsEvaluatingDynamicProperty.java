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
package org.codehaus.groovy.grails.validation.metaclass;

import groovy.lang.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicProperty;
import org.codehaus.groovy.grails.commons.metaclass.ProxyMetaClass;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder;

import java.io.InputStream;
import java.util.*;

/**
 * This is a dynamic property that instead of returning the closure sets a new proxy meta class for the scope 
 * of the call and invokes the closure itself which builds up a list of ConstrainedProperty instances
 * 
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 * @since 07-Nov-2005
 */
public class ConstraintsEvaluatingDynamicProperty extends AbstractDynamicProperty {

    private static final String CONSTRAINTS_GROOVY = "Constraints.groovy";

    private static final Log LOG = LogFactory.getLog(ConstraintsDynamicProperty.class);

    public static final String PROPERTY_NAME = "constraints";

	private GrailsDomainClassProperty[] properties;

    public ConstraintsEvaluatingDynamicProperty(GrailsDomainClassProperty[] properties) {
        super(PROPERTY_NAME);
        this.properties = properties;
    }
    
    public ConstraintsEvaluatingDynamicProperty() {
        super(PROPERTY_NAME);
    }    

    public Object get(Object object)  {

        // Suppress recursion problems if a GroovyObject
        if (object instanceof GroovyObject)
        {
            GroovyObject go = (GroovyObject)object;

            if (go.getMetaClass() instanceof ProxyMetaClass)
            {
                go.setMetaClass(((ProxyMetaClass)go.getMetaClass()).getAdaptee());
            }
        }

        // Compile list of ancestors to query for constraints
        LinkedList classChain = new LinkedList();
        Class clazz = object.getClass();
        while (clazz != Object.class)
        {
            classChain.addFirst( clazz);
            clazz = clazz.getSuperclass();
        }

        ConstrainedPropertyBuilder delegate = new ConstrainedPropertyBuilder(object);

        // Evaluate all the constraints closures in the inheritance chain
        for (Iterator it = classChain.iterator(); it.hasNext();)
        {
            clazz = (Class)it.next();
            Closure c = (Closure)GrailsClassUtils.getStaticPropertyValue(clazz, PROPERTY_NAME);
            if (c == null) {
                c = getConstraintsFromScript(object);
            }

            if (c != null) {
                c.setDelegate(delegate);
                c.call();
            } else {
                LOG.debug("User-defined constraints not found on class ["+clazz+"], applying default constraints");
            }
        }


        Map constrainedProperties = delegate.getConstrainedProperties();
        if(this.properties != null) {
            for (int i = 0; i < this.properties.length; i++) {
                GrailsDomainClassProperty p = this.properties[i];
                ConstrainedProperty cp = (ConstrainedProperty)constrainedProperties.get(p.getName());
                if(cp == null) {
                    cp = new ConstrainedProperty(p.getDomainClass().getClazz(), p.getName(), p.getType());
                    cp.setOrder(constrainedProperties.size()+1);
                    constrainedProperties.put(p.getName(), cp);
                }
                // Make sure all fields are required by default, unless specified otherwise by the constraints
                if(!cp.hasAppliedConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT)
                        && !p.getName().equals(GrailsDomainClassProperty.DATE_CREATED)
                        && !p.getName().equals(GrailsDomainClassProperty.LAST_UPDATED)) {
                	// TODO remove "p.isOptional()" in 0.6
//                    cp.applyConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT, Boolean.valueOf(p.isAssociation() || p.isOptional()));
                    cp.applyConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT, Boolean.valueOf(
                            p.isOptional() ||
                            Collection.class.isAssignableFrom( p.getType() ) ||
                            Map.class.isAssignableFrom( p.getType() ))
                    );
                }
            }
        }

        return constrainedProperties;
    }

    private Closure getConstraintsFromScript(Object object) {
        // Fallback to xxxxConstraints.groovy script for Java domain classes
        String className = object.getClass().getName();
        String constraintsScript = className.replaceAll("\\.","/") + CONSTRAINTS_GROOVY;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(constraintsScript);

        if(stream!=null) {
            GroovyClassLoader gcl = new GroovyClassLoader();
            try {
                Class scriptClass = gcl.parseClass(stream);
                Script script = (Script)scriptClass.newInstance();
                script.run();
                Binding binding = script.getBinding();
                if(binding.getVariables().containsKey(PROPERTY_NAME)) {
                    return (Closure)binding.getVariable(PROPERTY_NAME);
                } else {
                    LOG.warn("Unable to evaluate constraints from ["+constraintsScript+"], constraints closure not found!");
                    return null;
                }
            }
            catch (CompilationFailedException e) {
                LOG.error("Compilation error evaluating constraints for class ["+object.getClass()+"]: " + e.getMessage(),e );
                return null;
            } catch (InstantiationException e) {
                LOG.error("Instantiation error evaluating constraints for class ["+object.getClass()+"]: " + e.getMessage(),e );
                return null;
            } catch (IllegalAccessException e) {
                LOG.error("Illegal access error evaluating constraints for class ["+object.getClass()+"]: " + e.getMessage(),e );
                return null;
            }
        }
        else {
            return null;
        }
    }

    public void set(Object object, Object newValue) {
        throw new UnsupportedOperationException("Cannot set read-only property ["+PROPERTY_NAME+"]");
    }

}
