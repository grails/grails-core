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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.validation.CascadingValidator;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.BeanPropertyBindingResult;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Abstract class for different implementations that perform saving to implement
 * 
 * @author Graeme Rocher
 * @since 0.3
 *
 */
public abstract class AbstractSavePersistentMethod extends
		AbstractDynamicPersistentMethod {

	private GrailsApplication application;
    private static final String ARGUMENT_VALIDATE = "validate";
    private static final String ARGUMENT_DEEP_VALIDATE = "deepValidate";
    private static final String ARGUMENT_FLUSH = "flush";
    private static final String ARGUMENT_INSERT = "insert";

    public AbstractSavePersistentMethod(Pattern pattern, SessionFactory sessionFactory, ClassLoader classLoader, GrailsApplication application) {
		super(pattern, sessionFactory, classLoader);
        if(application == null)
            throw new IllegalArgumentException("Constructor argument 'application' cannot be null");
		
		this.application = application;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractDynamicPersistentMethod#doInvokeInternal(java.lang.Object, java.lang.Object[])
	 */
	protected Object doInvokeInternal(Object target, Object[] arguments) {
        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,
            target.getClass().getName() );
        
        if(shouldValidate(arguments, domainClass)) {
        	Validator validator = domainClass.getValidator();
            MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());

            Errors errors = new BeanPropertyBindingResult(target, target.getClass().getName());
            mc.setProperty(target, "errors", errors);
            
            if(validator != null) {
                boolean deepValidate = true;
                if(arguments.length > 0) {
                    if(arguments[0] instanceof Map) {
                        Map argsMap = (Map)arguments[0];

                        if(argsMap.containsKey(ARGUMENT_DEEP_VALIDATE))
                            deepValidate = GrailsClassUtils.getBooleanFromMap(ARGUMENT_DEEP_VALIDATE, argsMap);

                    }
                }
                if(deepValidate && (validator instanceof CascadingValidator)) {
                    ((CascadingValidator)validator).validate(target, errors, deepValidate);
                }
                else {
                    validator.validate(target,errors);
                }

                if(errors.hasErrors()) {
                    return handleValidationError(target,errors);
                }
            }
        }

        // this piece of code will retrieve a persistent instant
        // of a domain class property is only the id is set thus
        // relieving this burden off the developer
        if(domainClass != null) {            
            autoRetrieveAssocations(domainClass, target);
        }

        if(shouldInsert(arguments)) {
            return performInsert(target, shouldFlush(arguments));
        }
        else {
            return performSave(target, shouldFlush(arguments));
        }
	}

    private boolean shouldInsert(Object[] arguments) {
        return arguments.length > 0 && arguments[0] instanceof Map && GrailsClassUtils.getBooleanFromMap(ARGUMENT_INSERT, (Map) arguments[0]);
    }

    private boolean shouldFlush(Object[] arguments) {
        if(arguments.length > 0) {
            if(arguments[0] instanceof Boolean) {
                return ((Boolean)arguments[0]).booleanValue();
            }
            else if(arguments[0] instanceof Map) {
                Map argsMap = (Map)arguments[0];
                if(argsMap.containsKey(ARGUMENT_FLUSH)) {
                    return GrailsClassUtils.getBooleanFromMap(ARGUMENT_FLUSH, argsMap);
                }
            }
            else {
            	return false;
            }
        }
        return false;
	}

    /**
	 * Performs automatic association retrieval
     * @param domainClass The domain class to retrieve associations for
     * @param target The target object
     */
	private void autoRetrieveAssocations(GrailsDomainClass domainClass, Object target) {
		BeanWrapper bean = new BeanWrapperImpl(target);
		HibernateTemplate t = getHibernateTemplate();
		GrailsDomainClassProperty[] props = domainClass.getPersistentProperties();
		for (int i = 0; i < props.length; i++) {
		    GrailsDomainClassProperty prop = props[i];
		    if(prop.isManyToOne() || prop.isOneToOne()) {
		        Object propValue = bean.getPropertyValue(prop.getName());
		        if(propValue != null && !t.contains(propValue)) {
		            GrailsDomainClass otherSide = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,
                        prop.getType().getName());
		            if(otherSide != null) {
		                BeanWrapper propBean = new BeanWrapperImpl(propValue);
		                try {
                            Serializable id = (Serializable) propBean.getPropertyValue(otherSide.getIdentifier().getName());
                            if (id != null) {
                                bean.setPropertyValue(prop.getName(), t.get(prop.getType(), id));
                            }
                        } catch (InvalidPropertyException ipe) {
                            // property is not accessable
                        }
		            }
		        }
		    }
		}
	}

    /**
     * This method willl set the save() method will set the flush mode to manual. What this does
     * is ensure that the database changes are not persisted to the database if a validation error occurs.
     * If save() is called again and validation passes the code will check if there is a manual flush mode and
     * flush manually if necessary
     *
     * @param target The target object that failed validation
     * @param errors The Errors instance
     * @return This method will return null signaling a validation failure
     */
	protected Object handleValidationError(Object target, Errors errors) {
		HibernateTemplate t = getHibernateTemplate();
        // if the target is within the session evict it
        // this is so that if validation fails hibernate doesn't save
        // the object automatically when the session is flushed
       t.execute(new HibernateCallback() {

            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.setFlushMode(FlushMode.MANUAL);
                return null;
            }
        });
        setErrorsOnInstance(target, errors);
        return null;
	}

    /**
     * Associates the Errors object on the instance
     *
     * @param target The target instance
     * @param errors The Errors object
     */
    protected void setErrorsOnInstance(Object target, Errors errors) {
        if(target instanceof GroovyObject) {
            ((GroovyObject)target).setProperty(org.codehaus.groovy.grails.orm.hibernate.metaclass.DomainClassMethods.ERRORS_PROPERTY,errors);
        }
        else {
            MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());
            metaClass.setProperty(target.getClass() ,target, org.codehaus.groovy.grails.orm.hibernate.metaclass.DomainClassMethods.ERRORS_PROPERTY,errors, false, false);
        }
    }

    /**
 	 * Checks whether validation should be performed
     * @return True if the domain class should be validated
     * @param arguments  The arguments to the validate method
     * @param domainClass The domain class
     */
	private boolean shouldValidate(Object[] arguments, GrailsDomainClass domainClass) {
		if(domainClass != null) {
            if(arguments.length > 0) {
                if(arguments[0] instanceof Boolean) {
                    return ((Boolean)arguments[0]).booleanValue();
                }
                else if(arguments[0] instanceof Map) {
                     Map argsMap = (Map)arguments[0];
                    if(argsMap.containsKey(ARGUMENT_VALIDATE)) {
                        return GrailsClassUtils.getBooleanFromMap(ARGUMENT_VALIDATE,argsMap);
                    }
                    else {
                        return true;
                    }
                }
                else {
                	return true;
                }
            }
            else {
            	return true;
            }
        }
		return false;
	}

    /**
     * Subclasses should override and perform a save operation, flushing the session if the second argument is true
     *
     * @param target The target object to save
     * @param shouldFlush Whether to flush
     * @return The target object
     */
    abstract protected Object performSave(Object target, boolean shouldFlush);

    /**
     * Subclasses should override and perform an insert operation, flushing the session if the second argument is true
     *
     * @param target The target object to save
     * @param shouldFlush Whether to flush
     * @return The target object
     */
    protected abstract Object performInsert(Object target, boolean shouldFlush);

}
