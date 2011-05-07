package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.hibernate.SessionFactory;

public class FindOrCreateByPersistentMethod extends AbstractFindByPersistentMethod {


    private static final String METHOD_PATTERN = "(findOrCreateBy)([A-Z]\\w*)";

    public FindOrCreateByPersistentMethod(GrailsApplication application,SessionFactory sessionFactory, ClassLoader classLoader) {
        this(application,sessionFactory, classLoader, METHOD_PATTERN);
    }

    public FindOrCreateByPersistentMethod(GrailsApplication application,SessionFactory sessionFactory, ClassLoader classLoader, String pattern) {
    	super(application,sessionFactory, classLoader, Pattern.compile( pattern ), OPERATORS);
    }
    
    @Override
    protected Object doInvokeInternalWithExpressions(Class clazz,
    		String methodName, Object[] arguments, List expressions,
    		String operatorInUse, Closure additionalCriteria) {
		if(OPERATOR_OR.equals(operatorInUse)) {
            throw new UnsupportedOperationException(
            "'Or' expressions are not allowed in findOrCreateBy queries.");
    	}
    	Object result = super.doInvokeInternalWithExpressions(clazz, methodName, arguments,
    			expressions, operatorInUse, additionalCriteria);
    	
    	if(result == null) {
    		Map m = new HashMap();
    		for(Object o : expressions) {
    			GrailsMethodExpression gme = (GrailsMethodExpression) o;
    			m.put(gme.getPropertyName(), gme.getArguments()[0]);
    		}
            MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(clazz);
            result = metaClass.invokeConstructor(new Object[]{m});
            if(shouldSaveOnCreate()) {
            	metaClass.invokeMethod(result, "save", null);
            }
    	}
    	
    	return result;
    }

	protected boolean shouldSaveOnCreate() {
		return false;
	}
}
