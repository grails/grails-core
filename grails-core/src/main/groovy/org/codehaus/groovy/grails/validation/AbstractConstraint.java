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
package org.codehaus.groovy.grails.validation;

import grails.util.GrailsNameUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

/**
 * Abstract class for constraints to extend.
 *
 * @author Graeme Rocher
 */
public abstract class AbstractConstraint implements Constraint {

    protected String constraintPropertyName;
    protected Class<?> constraintOwningClass;
    protected Object constraintParameter;
    protected String classShortName;
    protected MessageSource messageSource;

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.Constraint#setMessageSource(org.springframework.context.MessageSource)
     */
    public void setMessageSource(MessageSource source) {
        messageSource = source;
    }

    public String getPropertyName() {
        return constraintPropertyName;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.groovy.grails.validation.Constraint#setOwningClass(java.lang.Class)
     */
    @SuppressWarnings("rawtypes")
    public void setOwningClass(Class constraintOwningClass) {
        this.constraintOwningClass = constraintOwningClass;
        classShortName = GrailsNameUtils.getPropertyNameRepresentation(constraintOwningClass);
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

    public Object getParameter() {
        return constraintParameter;
    }

    protected void checkState() {
        Assert.hasLength(constraintPropertyName, "Property 'propertyName' must be set on the constraint");
        Assert.notNull(constraintOwningClass, "Property 'owningClass' must be set on the constraint");
        Assert.notNull(constraintParameter, "Property 'constraintParameter' must be set on the constraint");
    }

    public void validate(Object target, Object propertyValue, Errors errors) {
        checkState();

        // Skip null values if desired.
        if (propertyValue == null && skipNullValues()) {
            return;
        }

        // Skip blank values if desired.
        if (skipBlankValues() && propertyValue instanceof String && StringUtils.isBlank((String) propertyValue)) {
            return;
        }

        // Do the validation for this constraint.
        processValidate(target, propertyValue, errors);
    }

    protected boolean skipNullValues() {
        // a null is not a value we should even check in most cases
        return true;
    }

    protected boolean skipBlankValues() {
        // Most constraints ignore blank values, leaving it to the explicit "blank" constraint.
        return true;
    }

    public void rejectValue(Object target, Errors errors, String defaultMessageCode, Object[] args) {
        rejectValue(target, errors, defaultMessageCode, new String[] {}, args);
    }

    public void rejectValue(Object target,Errors errors, String defaultMessageCode, String code, Object[] args) {
        rejectValue(target,errors, defaultMessageCode, new String[] {code}, args);
    }

    public void rejectValue(Object target,Errors errors, String defaultMessageCode, String[] codes, Object[] args) {
        rejectValueWithDefaultMessage(target, errors, getDefaultMessage(defaultMessageCode), codes, args);
    }

    public void rejectValueWithDefaultMessage(Object target, Errors errors, String defaultMessage, String[] codes, Object[] args) {
        BindingResult result = (BindingResult) errors;
        Set<String> newCodes = new LinkedHashSet<String>();

        if (args.length > 1 && messageSource != null) {
            if ((args[0] instanceof String) && (args[1] instanceof Class<?>)) {
                final Locale locale = LocaleContextHolder.getLocale();
                final Class<?> constrainedClass = (Class<?>) args[1];
                final String fullClassName = constrainedClass.getName();

                String classNameCode = fullClassName + ".label";
                String resolvedClassName = messageSource.getMessage(classNameCode, null, fullClassName, locale);
                final String classAsPropertyName = GrailsNameUtils.getPropertyName(constrainedClass);

                if (resolvedClassName.equals(fullClassName)) {
                    // try short version
                    classNameCode = classAsPropertyName+".label";
                    resolvedClassName = messageSource.getMessage(classNameCode, null, fullClassName, locale);
                }

                // update passed version
                if (!resolvedClassName.equals(fullClassName)) {
                    args[1] = resolvedClassName;
                }

                String propertyName = (String)args[0];
                String propertyNameCode = fullClassName + '.' + propertyName + ".label";
                String resolvedPropertyName = messageSource.getMessage(propertyNameCode, null, propertyName, locale);
                if (resolvedPropertyName.equals(propertyName)) {
                    propertyNameCode = classAsPropertyName + '.' + propertyName + ".label";
                    resolvedPropertyName = messageSource.getMessage(propertyNameCode, null, propertyName, locale);
                }

                // update passed version
                if (!resolvedPropertyName.equals(propertyName)) {
                    args[0] = resolvedPropertyName;
                }
            }
        }

        //Qualified class name is added first to match before unqualified class (which is still resolved for backwards compatibility)
        newCodes.addAll(Arrays.asList(result.resolveMessageCodes(constraintOwningClass.getName() + '.'  + constraintPropertyName + '.' + getName() + ".error", constraintPropertyName)));
        newCodes.addAll(Arrays.asList(result.resolveMessageCodes(classShortName + '.'  + constraintPropertyName + '.' + getName() + ".error", constraintPropertyName)));
        for (String code : codes) {
            newCodes.addAll(Arrays.asList(result.resolveMessageCodes(constraintOwningClass.getName() + '.' + constraintPropertyName + '.' + code, constraintPropertyName)));
            newCodes.addAll(Arrays.asList(result.resolveMessageCodes(classShortName + '.' + constraintPropertyName + '.' + code, constraintPropertyName)));
            //We resolve the error code on it's own last so that a global code doesn't override a class/field specific error
            newCodes.addAll(Arrays.asList(result.resolveMessageCodes(code, constraintPropertyName)));
        }

        FieldError error = new FieldError(
                errors.getObjectName(),
                errors.getNestedPath() + constraintPropertyName,
                getPropertyValue(errors, target),
                false,
                newCodes.toArray(new String[newCodes.size()]),
                args,
                defaultMessage);
        ((BindingResult)errors).addError(error);
    }

    private Object getPropertyValue(Errors errors, Object target) {
        try {
            return errors.getFieldValue(constraintPropertyName);
        }
        catch (Exception nre) {
            int i = constraintPropertyName.lastIndexOf(".");
            String propertyName;
            if (i > -1) {
                propertyName = constraintPropertyName.substring(i, constraintPropertyName.length());
            }
            else {
                propertyName = constraintPropertyName;
            }
            return new BeanWrapperImpl(target).getPropertyValue(propertyName);
        }
    }

    // For backward compatibility
    public void rejectValue(Object target, Errors errors, String code, String defaultMessage) {
        rejectValueWithDefaultMessage(target, errors, defaultMessage, new String[] {code}, null);
    }

    // For backward compatibility
    public void rejectValue(Object target, Errors errors, String code, Object[] args, String defaultMessage) {
        rejectValueWithDefaultMessage(target, errors, defaultMessage, new String[] {code}, args);
    }

    /**
     * Returns the default message for the given message code in the
     * current locale. Note that the string returned includes any
     * placeholders that the required message has - these must be
     * expanded by the caller if required.
     * @param code The i18n message code to look up.
     * @return The message corresponding to the given code in the
     * current locale.
     */
    protected String getDefaultMessage(String code) {
        try {
            if (messageSource != null) {
                return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
            }

            return ConstrainedProperty.DEFAULT_MESSAGES.get(code);
        }
        catch (Exception e) {
            return ConstrainedProperty.DEFAULT_MESSAGES.get(code);
        }
    }

    protected abstract void processValidate(Object target, Object propertyValue, Errors errors);

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(constraintParameter).toString();
    }

    /**
     * Return whether the constraint is valid for the owning class
     *
     * @return true if it is
     */
    public boolean isValid() {
        return true;
    }
}
