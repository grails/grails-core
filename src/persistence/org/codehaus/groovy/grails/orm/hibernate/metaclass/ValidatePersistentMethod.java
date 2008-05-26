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


import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.validation.CascadingValidator;
import org.hibernate.SessionFactory;
import org.springframework.validation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A method that validates an instance of a domain class against its constraints 
 * 
 * @author Graeme Rocher
 * @since 07-Nov-2005
 */
public class ValidatePersistentMethod extends AbstractDynamicPersistentMethod {

    public static final String METHOD_SIGNATURE = "validate";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');
    private GrailsApplication application;
    private static final String ARGUMENT_DEEP_VALIDATE = "deepValidate";
    private static final String ARGUMENT_EVICT = "evict";


    public ValidatePersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader, GrailsApplication application) {
        super(METHOD_PATTERN, sessionFactory, classLoader);
        if(application == null)
            throw new IllegalArgumentException("Constructor argument 'application' cannot be null");
        this.application = application;
    }

    protected Object doInvokeInternal(Object target, Object[] arguments) {
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());

        Errors errors = new BeanPropertyBindingResult(target, target.getClass().getName());
        mc.setProperty(target, "errors", errors);
        
        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,
            target.getClass().getName() );
        Validator validator = null;

        if(domainClass != null)
            validator = domainClass.getValidator();

        Boolean valid = Boolean.TRUE;
        if(validator != null) {
            // should evict?
            boolean evict = false;
            boolean deepValidate = true;
            Set validatedFields = null;

            if(arguments.length > 0) {
                if(arguments[0] instanceof Boolean) {
                    evict = ((Boolean)arguments[0]).booleanValue();
                }
                if(arguments[0] instanceof Map) {
                    Map argsMap = (Map)arguments[0];

                    if(argsMap.containsKey(ARGUMENT_DEEP_VALIDATE))
                        deepValidate = GrailsClassUtils.getBooleanFromMap(ARGUMENT_DEEP_VALIDATE, argsMap);
                    
                    evict = GrailsClassUtils.getBooleanFromMap(ARGUMENT_EVICT, argsMap);
                }
                if (arguments[0] instanceof List) {
                    validatedFields = new HashSet((List) arguments[0]);
                }
            }
            if(deepValidate && (validator instanceof CascadingValidator)) {
                ((CascadingValidator)validator).validate(target, errors, deepValidate);
            }
            else {
                validator.validate(target,errors);
            }

            int oldErrorCount = errors.getErrorCount();
            errors = filterErrors(errors, validatedFields, target);

            if(errors.hasErrors()) {
                valid = Boolean.FALSE;
                if(evict) {
                    // if an boolean argument 'true' is passed to the method
                    // and validation fails then the object will be evicted
                    // from the session, ensuring it is not saved later when
                    // flush is called
                    if(getHibernateTemplate().contains(target)) {
                        getHibernateTemplate().evict(target);
                    }
                }
            }

            // If the errors have been filtered, update the 'errors'
            // object attached to the target.
            if (errors.getErrorCount() != oldErrorCount) {
                MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());
                metaClass.setProperty(target, DomainClassMethods.ERRORS_PROPERTY, errors);
            }
        }
        return valid;
    }

    private Errors filterErrors(Errors errors, Set validatedFields, Object target) {
        if (validatedFields == null) return errors;

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(target, target.getClass().getName());

        final List allErrors = errors.getAllErrors();
        for (int i = 0; i < allErrors.size(); i++) {
            ObjectError error = (ObjectError) allErrors.get(i);

            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                if (!validatedFields.contains(fieldError.getField())) continue;
            }

            result.addError(error);
        }

        return result;
    }

}
