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

import org.codehaus.groovy.grails.commons.metaclass.AbstractStaticMethodInvocation;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Abstract base class for static persistent methods
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 * 
 * @since Aug 8, 2005
 */
public abstract class AbstractStaticPersistentMethod extends
		AbstractStaticMethodInvocation {

	private SessionFactory sessionFactory = null;
	private ClassLoader classLoader = null;
    private SimpleTypeConverter converter = new SimpleTypeConverter();

    protected static final String ARGUMENT_MAX = "max";
	protected static final String ARGUMENT_OFFSET = "offset";
	protected static final String ARGUMENT_ORDER = "order";
	protected static final String ARGUMENT_SORT = "sort";
	protected static final String ORDER_DESC = "desc";
	protected static final String ORDER_ASC = "asc";
	protected static final String ARGUMENT_FETCH = "fetch";
    protected static final String ARGUMENT_IGNORE_CASE = "ignoreCase";

    public AbstractStaticPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader, Pattern pattern) {
		super();
		setPattern(pattern);
		this.sessionFactory = sessionFactory;
		this.classLoader = classLoader;
	}

	protected HibernateTemplate getHibernateTemplate() {
		Assert.notNull(sessionFactory, "Session factory is required!");
		return new HibernateTemplate(this.sessionFactory);
	}
	
	public Object invoke(Class clazz, String methodName, Object[] arguments) {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();    
		try {
			Thread.currentThread().setContextClassLoader(this.classLoader);
			return doInvokeInternal(clazz, methodName, arguments);			
		}   
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);      			
		}
	}

	protected abstract Object doInvokeInternal(Class clazz, String methodName, Object[] arguments);
	
	protected void populateArgumentsForCriteria(Criteria c, Map argMap) {
		
		Integer maxParam = null;
		Integer offsetParam = null;
		if(argMap.containsKey(ARGUMENT_MAX)) {			
			maxParam = (Integer)converter.convertIfNecessary(argMap.get(ARGUMENT_MAX),Integer.class);
		}
		if(argMap.containsKey(ARGUMENT_OFFSET)) {
			offsetParam = (Integer)converter.convertIfNecessary(argMap.get(ARGUMENT_OFFSET),Integer.class);
		}
		String orderParam = (String)argMap.get(ARGUMENT_ORDER);
		Object fetchObj = argMap.get(ARGUMENT_FETCH);
		if(fetchObj instanceof Map) {
			Map fetch = (Map)fetchObj;
			for (Iterator i = fetch.keySet().iterator(); i.hasNext();) {
				String associationName = (String) i.next();
				c.setFetchMode(associationName, getFetchMode(fetch.get(i)));
			}
		}
		
		final String sort = (String)argMap.get(ARGUMENT_SORT);
		final String order = ORDER_DESC.equalsIgnoreCase(orderParam) ? ORDER_DESC : ORDER_ASC;
		final int max = maxParam == null ? -1 : maxParam.intValue();
		final int offset = offsetParam == null ? -1 : offsetParam.intValue();
		if(max > -1)
			c.setMaxResults(max);
		if(offset > -1)
			c.setFirstResult(offset);
		if(sort != null) {
            boolean ignoreCase = true;
            Object caseArg = argMap.get(ARGUMENT_IGNORE_CASE);
            if(caseArg instanceof Boolean) {
                ignoreCase = ((Boolean)caseArg).booleanValue();
            }
            if(ORDER_DESC.equals(order)) {
				c.addOrder( ignoreCase ? Order.desc(sort).ignoreCase() : Order.desc(sort));
			}
			else {
				c.addOrder( ignoreCase ? Order.asc(sort).ignoreCase() : Order.asc(sort) );
			}
		}		
	}

	/**
	 * Will retrieve the fetch mode for the specified instance other wise return the 
	 * default FetchMode
	 * 
	 * @param object The object, converted to a string
	 * @return The FetchMode
	 */
	protected FetchMode getFetchMode(Object object) {
		String name = object != null ? object.toString() : "default";
		if(name.equals(FetchMode.JOIN.toString()) || name.equals("eager")) {
			return FetchMode.JOIN;
		}
		else if(name.equals(FetchMode.SELECT.toString()) || name.equals("lazy")) {
			return FetchMode.SELECT;
		}
		return FetchMode.DEFAULT;
	}	
}
