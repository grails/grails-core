package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.springframework.util.ReflectionUtils;

public class BeforeValidateHelper {
    public static final String BEFORE_VALIDATE = "beforeValidate";

    public void invokeBeforeValidate(final Object target, final List<?> validatedFieldsList) {
		Class<?> domainClass= target.getClass();
		Method method = null;
        if(validatedFieldsList == null) {
        	// prefer the no-arg version of beforeValidate() if validatedFieldsList is null...
        	method = ReflectionUtils.findMethod(domainClass, BEFORE_VALIDATE);
        	if(method == null) {
        		method = ReflectionUtils.findMethod(domainClass, BEFORE_VALIDATE, List.class);
        	}
        } else {
        	// prefer the list-arg version of beforeValidate() if validatedFieldsList is not null...
        	method = ReflectionUtils.findMethod(domainClass, BEFORE_VALIDATE, List.class);
        	if(method == null) {
        		method = ReflectionUtils.findMethod(domainClass, BEFORE_VALIDATE);
        	}
        }
		if (method != null) {
			ReflectionUtils.makeAccessible(method);
			Class<?>[] parameterTypes = method.getParameterTypes();
			if(parameterTypes.length == 1) {
    			ReflectionUtils.invokeMethod(method, target, validatedFieldsList);
			} else {
    			ReflectionUtils.invokeMethod(method, target);
			}
		}
	}
}
