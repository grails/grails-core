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

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.validation.Errors;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Locale;

/**
 * @author Graeme Rocher
 *
 * Abstract class for constraints to implement
 */
public abstract class AbstractConstraint implements Constraint {

    protected String constraintPropertyName;
    protected Class constraintOwningClass;
    protected Object constraintParameter;
    protected String classShortName;
    protected MessageSource messageSource;

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.Constraint#setMessageSource(org.springframework.context.MessageSource)
     */
    public void setMessageSource(MessageSource source) {
            this.messageSource = source;
    }

    public String getPropertyName() {
        return this.constraintPropertyName;
    }

    /**
     * @param constraintOwningClass The constraintOwningClass to set.
     */
    public void setOwningClass(Class constraintOwningClass) {
        this.constraintOwningClass = constraintOwningClass;
        this.classShortName = GrailsClassUtils.getPropertyNameRepresentation(constraintOwningClass);
    }
    /**
     * @param constraintPropertyName The constraintPropertyName to set.
     */
    public void setPropertyName(String constraintPropertyName) {
        this.constraintPropertyName = constraintPropertyName;
    }
    /**
     * @param constraintParameter The constraintParameter to set.
     */
    public void setParameter(Object constraintParameter) {
        this.constraintParameter = constraintParameter;
    }
    public void validate(Object target, Object propertyValue, Errors errors) {
        //ValidationUtils.rejectIfEmpty( errors, constraintPropertyName, constraintPropertyName+".empty" );
        if(StringUtils.isBlank(this.constraintPropertyName))
            throw new IllegalStateException("Property 'propertyName' must be set on the constraint");
        if(constraintOwningClass == null)
            throw new IllegalStateException("Property 'owningClass' must be set on the constraint");
        if(constraintParameter == null)
            throw new IllegalStateException("Property 'constraintParameter' must be set on the constraint");

        processValidate(target, propertyValue, errors);
    }
    public void rejectValue(Errors errors, String code,String defaultMessage) {

        errors.rejectValue(constraintPropertyName,classShortName + '.'  + constraintPropertyName + '.' + code, defaultMessage);
    }
    protected String getDefaultMessage(String code, Object[] args) {
        String defaultMessage;
        try {
            if(messageSource != null)
                defaultMessage = messageSource.getMessage(code,args, Locale.getDefault());
            else
                defaultMessage = (String)ConstrainedProperty.DEFAULT_MESSAGES.get(code);
        }
        catch(NoSuchMessageException nsme) {
            defaultMessage = (String)ConstrainedProperty.DEFAULT_MESSAGES.get(code);
        }
        return defaultMessage;
    }
    public void rejectValue(Errors errors, String code,Object[] args,String defaultMessage) {
        errors.rejectValue(constraintPropertyName,classShortName + '.'  + constraintPropertyName + '.' + code, args,defaultMessage);
    }
    protected abstract void processValidate(Object target, Object propertyValue, Errors errors);

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return new ToStringBuilder(this)
                        .append( constraintParameter )
                        .toString();
    }


}