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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import grails.validation.ValidationErrors;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore;
import org.codehaus.groovy.grails.orm.hibernate.validation.AbstractPersistentConstraint;
import org.codehaus.groovy.grails.validation.CascadingValidator;
import org.grails.datastore.mapping.engine.event.ValidationEvent;
import org.hibernate.SessionFactory;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

/**
 * Validates an instance of a domain class against its constraints.
 *
 * @author Graeme Rocher
 * @since 07-Nov-2005
 */
public class ValidatePersistentMethod extends AbstractDynamicPersistentMethod {

    public static final String METHOD_SIGNATURE = "validate";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');
    public static final String ARGUMENT_DEEP_VALIDATE = "deepValidate";
    private static final String ARGUMENT_EVICT = "evict";
    private Validator validator;
    private HibernateDatastore datastore;

    public ValidatePersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader, GrailsApplication application) {
        this(sessionFactory, classLoader, application, null, null);
    }

    public ValidatePersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader,
             GrailsApplication application, Validator validator, HibernateDatastore datastore) {
        super(METHOD_PATTERN, sessionFactory, classLoader, application);
        Assert.notNull(application, "Constructor argument 'application' cannot be null");
        this.validator = validator;
        this.datastore = datastore;
    }

    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    protected Object doInvokeInternal(final Object target, Object[] arguments) {
        Errors errors = setupErrorsProperty(target);

        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,
                target.getClass().getName());

        if (validator == null && domainClass != null) {
            validator = domainClass.getValidator();
        }

        if (validator == null) {
            return true;
        }

        Boolean valid = Boolean.TRUE;
        // should evict?
        boolean evict = false;
        boolean deepValidate = true;
        Set validatedFields = null;
        List validatedFieldsList = null;

        if (arguments.length > 0) {
            if (arguments[0] instanceof Boolean) {
                evict = (Boolean)arguments[0];
            }
            if (arguments[0] instanceof Map) {
                Map argsMap = (Map)arguments[0];

                if (argsMap.containsKey(ARGUMENT_DEEP_VALIDATE)) {
                    deepValidate = GrailsClassUtils.getBooleanFromMap(ARGUMENT_DEEP_VALIDATE, argsMap);
                }

                evict = GrailsClassUtils.getBooleanFromMap(ARGUMENT_EVICT, argsMap);
            }
            if (arguments[0] instanceof List) {
                validatedFieldsList = (List)arguments[0];
                validatedFields = new HashSet(validatedFieldsList);
            }
        }

        fireEvent(target, validatedFieldsList);

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

        int oldErrorCount = errors.getErrorCount();
        errors = filterErrors(errors, validatedFields, target);

        if (errors.hasErrors()) {
            valid = Boolean.FALSE;
            if (evict) {
                // if an boolean argument 'true' is passed to the method
                // and validation fails then the object will be evicted
                // from the session, ensuring it is not saved later when
                // flush is called
                if (getHibernateTemplate().contains(target)) {
                    getHibernateTemplate().evict(target);
                }
            }
        }

        // If the errors have been filtered, update the 'errors' object attached to the target.
        if (errors.getErrorCount() != oldErrorCount) {
            MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());
            metaClass.setProperty(target, ERRORS_PROPERTY, errors);
        }

        return valid;
    }

    private void fireEvent(Object target, List<?> validatedFieldsList) {
        ValidationEvent event = new ValidationEvent(datastore, target);
        event.setValidatedFields(validatedFieldsList);
        application.getMainContext().publishEvent(event);
    }

    @SuppressWarnings("rawtypes")
    private Errors filterErrors(Errors errors, Set validatedFields, Object target) {
        if (validatedFields == null) return errors;

        ValidationErrors result = new ValidationErrors(target);

        final List allErrors = errors.getAllErrors();
        for (Object allError : allErrors) {
            ObjectError error = (ObjectError) allError;

            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                if (!validatedFields.contains(fieldError.getField())) continue;
            }

            result.addError(error);
        }

        return result;
    }
}
