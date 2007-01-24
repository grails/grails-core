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
package org.codehaus.groovy.grails.validation;

import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Collection;
import java.util.Iterator;

/**
 * A validator that validates a domain class based on the applied constraints 
 * 
 * @author Graeme Rocher
 * @since 07-Nov-2005
 */
public class GrailsDomainClassValidator implements Validator {

    private Class targetClass;
    private GrailsDomainClass domainClass;
    private MessageSource messageSource;
    private static final String ERRORS_PROPERTY = "errors";

    public boolean supports(Class clazz) {
        return this.targetClass.equals( clazz );
    }


    /**
     * @param domainClass The domainClass to set.
     */
    public void setDomainClass(GrailsDomainClass domainClass) {
        this.domainClass = domainClass;
        this.domainClass.setValidator(this);
        this.targetClass = this.domainClass.getClazz();
    }


    public GrailsDomainClass getDomainClass() {
        return domainClass;
    }

    /**
	 * @param messageSource The messageSource to set.
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}


	public void validate(Object obj, Errors errors) {
        if(!domainClass.getClazz().isInstance(obj))
            throw new IllegalArgumentException("Argument ["+obj+"] is not an instance of ["+domainClass.getClazz()+"] which this validator is configured for");

        BeanWrapper bean = new BeanWrapperImpl(obj);

        Collection constrainedProperties = domainClass.getConstrainedProperties().values();
        for (Iterator i = constrainedProperties.iterator(); i.hasNext();) {

            ConstrainedProperty c = (ConstrainedProperty)i.next();
            c.setMessageSource(this.messageSource);
            c.validate(obj, bean.getPropertyValue( c.getPropertyName() ),errors);
        }

         if(obj instanceof GroovyObject) {
            ((GroovyObject)obj).setProperty(ERRORS_PROPERTY, errors);
         }
         else {
            InvokerHelper.setProperty(obj,ERRORS_PROPERTY,errors);
         }
    }

}
