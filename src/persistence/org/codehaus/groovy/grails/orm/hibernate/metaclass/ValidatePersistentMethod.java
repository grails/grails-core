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



import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethodsMetaClass;
import org.codehaus.groovy.grails.metaclass.DomainClassMethods;
import org.codehaus.groovy.grails.validation.CascadingValidator;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.hibernate.SessionFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.regex.Pattern;
import java.util.Map;

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
        Errors errors = new BindException(target, target.getClass().getName());
        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,
            target.getClass().getName() );
        Validator validator = null;

        if(domainClass != null)
            validator = ((GrailsDomainClass)application.getArtefact(DomainClassArtefactHandler.TYPE, 
                target.getClass().getName() )).getValidator();

        Boolean valid = Boolean.TRUE;
        if(validator != null) {
            // should evict?
            boolean evict = false;
            boolean deepValidate = true;
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
            }
            if(deepValidate && (validator instanceof CascadingValidator)) {
                ((CascadingValidator)validator).validate(target, errors, deepValidate);
            }
            else {
                validator.validate(target,errors);
            }


            if(errors.hasErrors()) {
                valid = Boolean.valueOf(false);
                if(evict) {
                    // if an boolean argument 'true' is passed to the method
                    // and validation fails then the object will be evicted
                    // from the session, ensuring it is not saved later when
                    // flush is called
                    if(getHibernateTemplate().contains(target)) {
                        getHibernateTemplate().evict(target);
                    }
                }
                DynamicMethodsMetaClass metaClass = (DynamicMethodsMetaClass)InvokerHelper.getInstance().getMetaRegistry().getMetaClass(target.getClass());
                metaClass.setProperty(target.getClass(),target,DomainClassMethods.ERRORS_PROPERTY,errors, false,false);
            }
        }
        return valid;
    }

}
