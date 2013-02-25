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

import grails.validation.DeferredBindingActions;
import grails.validation.ValidationException;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.lifecycle.ShutdownOperations;
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.orm.hibernate.validation.AbstractPersistentConstraint;
import org.codehaus.groovy.grails.validation.CascadingValidator;
import org.grails.datastore.mapping.engine.event.ValidationEvent;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Abstract class for different implementations that perform saving to implement.
 *
 * @author Graeme Rocher
 * @since 0.3
 */
public abstract class AbstractSavePersistentMethod extends AbstractDynamicPersistentMethod {

    private HibernateDatastore datastore;
    private static final String ARGUMENT_VALIDATE = "validate";
    private static final String ARGUMENT_DEEP_VALIDATE = "deepValidate";
    private static final String ARGUMENT_FLUSH = "flush";
    private static final String ARGUMENT_INSERT = "insert";
    private static final String ARGUMENT_FAIL_ON_ERROR = "failOnError";
    private static final String FAIL_ON_ERROR_CONFIG_PROPERTY = "grails.gorm.failOnError";
    private static final String AUTO_FLUSH_CONFIG_PROPERTY = "grails.gorm.autoFlush";

    /**
     * When a domain instance is saved without validation, we put it
     * into this thread local variable. Any code that needs to know
     * whether the domain instance should be validated can just check
     * the value. Note that this only works because the session is
     * flushed when a domain instance is saved without validation.
     */
    private static ThreadLocal<Set<Integer>> disableAutoValidationFor = new ThreadLocal<Set<Integer>>();

    static {
        ShutdownOperations.addOperation(new Runnable() {
            public void run() {
                disableAutoValidationFor.remove();
            }
        });
    }

    private static Set<Integer> getDisableAutoValidationFor() {
        Set<Integer> identifiers = disableAutoValidationFor.get();
        if (identifiers == null)
            disableAutoValidationFor.set(identifiers = new HashSet<Integer>());
        return identifiers;
    }

    public static boolean isAutoValidationDisabled(Object obj) {
        Set<Integer> identifiers = getDisableAutoValidationFor();
        return obj != null && identifiers.contains(System.identityHashCode(obj));
    }

    public static void clearDisabledValidations(Object obj) {
        getDisableAutoValidationFor().remove(System.identityHashCode(obj));
    }

    public static void clearDisabledValidations() {
        getDisableAutoValidationFor().clear();
    }

    public AbstractSavePersistentMethod(Pattern pattern, SessionFactory sessionFactory,
              ClassLoader classLoader, GrailsApplication application,
              GrailsDomainClass domainClass, HibernateDatastore datastore) {
        super(pattern, sessionFactory, classLoader, application);

        this.datastore = datastore;
    }

    public AbstractSavePersistentMethod(Pattern pattern, SessionFactory sessionFactory,
             ClassLoader classLoader, GrailsApplication application, HibernateDatastore datastore) {
       this(pattern, sessionFactory, classLoader, application, null, datastore);
    }

    @SuppressWarnings("rawtypes")
    private boolean shouldFail(GrailsApplication grailsApplication, GrailsDomainClass domainClass) {
        boolean shouldFail = false;
        final Map config = grailsApplication.getFlatConfig();
        if (config.containsKey(FAIL_ON_ERROR_CONFIG_PROPERTY)) {
            Object configProperty = config.get(FAIL_ON_ERROR_CONFIG_PROPERTY);
            if (configProperty instanceof Boolean) {
                shouldFail = Boolean.TRUE == configProperty;
            }
            else if (configProperty instanceof List) {
                if (domainClass != null) {
                    final Class theClass = domainClass.getClazz();
                    List packageList = (List) configProperty;
                    shouldFail = GrailsClassUtils.isClassBelowPackage(theClass, packageList);
                }
            }
        }
        return shouldFail;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractDynamicPersistentMethod#doInvokeInternal(java.lang.Object, java.lang.Object[])
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected Object doInvokeInternal(final Object target, Object[] arguments) {
        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,
                target.getClass().getName());

        DeferredBindingActions.runActions();
        boolean shouldFlush = shouldFlush(arguments);
        boolean shouldValidate = shouldValidate(arguments, domainClass);
        if (shouldValidate) {
            Validator validator = domainClass.getValidator();

            if (domainClass instanceof DefaultGrailsDomainClass) {
                GrailsHibernateUtil.autoAssociateBidirectionalOneToOnes((DefaultGrailsDomainClass) domainClass, target);
            }
            Errors errors = setupErrorsProperty(target);

            if (validator != null) {
                application.getMainContext().publishEvent(new ValidationEvent(datastore, target));

                boolean deepValidate = true;
                Map argsMap = null;
                if (arguments.length > 0 && arguments[0] instanceof Map) {
                    argsMap = (Map) arguments[0];
                }
                if (argsMap != null && argsMap.containsKey(ARGUMENT_DEEP_VALIDATE)) {
                    deepValidate = GrailsClassUtils.getBooleanFromMap(ARGUMENT_DEEP_VALIDATE, argsMap);
                }

                AbstractPersistentConstraint.sessionFactory.set(datastore.getSessionFactory());
                try {
                    if (deepValidate && (validator instanceof CascadingValidator)) {
                        ((CascadingValidator)validator).validate(target, errors, deepValidate);
                    }
                    else {
                        validator.validate(target,errors);
                    }
                }
                finally {
                    AbstractPersistentConstraint.sessionFactory.remove();
                }

                if (errors.hasErrors()) {
                    handleValidationError(domainClass,target,errors);
                    boolean shouldFail = shouldFail(application, domainClass);
                    if (argsMap != null && argsMap.containsKey(ARGUMENT_FAIL_ON_ERROR)) {
                        shouldFail = GrailsClassUtils.getBooleanFromMap(ARGUMENT_FAIL_ON_ERROR, argsMap);
                    }
                    if (shouldFail) {
                        throw new ValidationException("Validation Error(s) occurred during save()", errors);
                    }
                    return null;
                }

                setObjectToReadWrite(target);
            }
        }

        // this piece of code will retrieve a persistent instant
        // of a domain class property is only the id is set thus
        // relieving this burden off the developer
        if (domainClass != null) {
            autoRetrieveAssocations(domainClass, target);
        }

        if (!shouldValidate) {
            Set<Integer> identifiers = getDisableAutoValidationFor();
            identifiers.add(System.identityHashCode(target));
        }

        if (shouldInsert(arguments)) {
            return performInsert(target, shouldFlush);
        }

        return performSave(target, shouldFlush);
    }

    @SuppressWarnings("rawtypes")
    private boolean shouldInsert(Object[] arguments) {
        return arguments.length > 0 && arguments[0] instanceof Map &&
            GrailsClassUtils.getBooleanFromMap(ARGUMENT_INSERT, (Map) arguments[0]);
    }

    @SuppressWarnings("rawtypes")
    private boolean shouldFlush(Object[] arguments) {
        final boolean shouldFlush;
        if (arguments.length > 0 && arguments[0] instanceof Boolean) {
            shouldFlush = (Boolean)arguments[0];
        }
        else if (arguments.length > 0 && arguments[0] instanceof Map && ((Map) arguments[0]).containsKey(ARGUMENT_FLUSH)) {
            shouldFlush = GrailsClassUtils.getBooleanFromMap(ARGUMENT_FLUSH, ((Map) arguments[0]));
        }
        else {
            final Map config = application.getFlatConfig();
            shouldFlush = Boolean.TRUE == config.get(AUTO_FLUSH_CONFIG_PROPERTY);
        }
        return shouldFlush;
    }

    /**
     * Performs automatic association retrieval
     * @param domainClass The domain class to retrieve associations for
     * @param target The target object
     */
    @SuppressWarnings("unchecked")
    private void autoRetrieveAssocations(GrailsDomainClass domainClass, Object target) {
        BeanWrapper bean = new BeanWrapperImpl(target);
        HibernateTemplate t = getHibernateTemplate();
        GrailsDomainClassProperty[] props = domainClass.getPersistentProperties();
        for (int i = 0; i < props.length; i++) {
            GrailsDomainClassProperty prop = props[i];
            if (!prop.isManyToOne() && !prop.isOneToOne()) {
                continue;
            }

            Object propValue = bean.getPropertyValue(prop.getName());
            if (propValue == null || t.contains(propValue)) {
                continue;
            }

            GrailsDomainClass otherSide = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,
                    prop.getType().getName());
            if (otherSide == null) {
                continue;
            }

            BeanWrapper propBean = new BeanWrapperImpl(propValue);
            try {
                Serializable id = (Serializable)propBean.getPropertyValue(otherSide.getIdentifier().getName());
                if (id != null) {
                    final Object propVal = t.get(prop.getType(), id);
                    if (propVal != null) {
                        bean.setPropertyValue(prop.getName(), propVal);
                    }
                }
            }
            catch (InvalidPropertyException ipe) {
                // property is not accessable
            }
        }
    }

    /**
     * Sets the flush mode to manual. which ensures that the database changes are not persisted to the database
     * if a validation error occurs. If save() is called again and validation passes the code will check if there
     * is a manual flush mode and flush manually if necessary
     *
     * @param domainClass The domain class
     * @param target The target object that failed validation
     * @param errors The Errors instance  @return This method will return null signaling a validation failure
     */
    protected Object handleValidationError(GrailsDomainClass domainClass, final Object target, Errors errors) {
        // if a validation error occurs set the object to read-only to prevent a flush
        setObjectToReadOnly(target);
        if (domainClass instanceof DefaultGrailsDomainClass) {
            DefaultGrailsDomainClass dgdc = (DefaultGrailsDomainClass) domainClass;
            List<GrailsDomainClassProperty> associations = dgdc.getAssociations();
            for (GrailsDomainClassProperty association : associations) {
                if (association.isOneToOne() || association.isManyToOne()) {
                    BeanWrapper bean = new BeanWrapperImpl(target);
                    Object propertyValue = bean.getPropertyValue(association.getName());
                    if (propertyValue != null) {
                        setObjectToReadOnly(propertyValue);
                    }
                }
            }
        }
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
        if (target instanceof GroovyObject) {
            ((GroovyObject)target).setProperty(ERRORS_PROPERTY,errors);
        }
        else {
            MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());
            metaClass.setProperty(target.getClass() ,target, ERRORS_PROPERTY,errors, false, false);
        }
    }

    /**
     * Checks whether validation should be performed
     * @return true if the domain class should be validated
     * @param arguments  The arguments to the validate method
     * @param domainClass The domain class
     */
    @SuppressWarnings("rawtypes")
    private boolean shouldValidate(Object[] arguments, GrailsDomainClass domainClass) {
        if (domainClass == null) {
            return false;
        }

        if (arguments.length == 0) {
            return true;
        }

        if (arguments[0] instanceof Boolean) {
            return (Boolean)arguments[0];
        }

        if (arguments[0] instanceof Map) {
            Map argsMap = (Map)arguments[0];
            if (argsMap.containsKey(ARGUMENT_VALIDATE)) {
                return GrailsClassUtils.getBooleanFromMap(ARGUMENT_VALIDATE, argsMap);
            }

            return true;
        }

        return true;
    }

    /**
     * Subclasses should override and perform a save operation, flushing the session if the second argument is true
     *
     * @param target The target object to save
     * @param shouldFlush Whether to flush
     * @return The target object
     */
    protected abstract Object performSave(Object target, boolean shouldFlush);

    /**
     * Subclasses should override and perform an insert operation, flushing the session if the second argument is true
     *
     * @param target The target object to save
     * @param shouldFlush Whether to flush
     * @return The target object
     */
    protected abstract Object performInsert(Object target, boolean shouldFlush);
}
