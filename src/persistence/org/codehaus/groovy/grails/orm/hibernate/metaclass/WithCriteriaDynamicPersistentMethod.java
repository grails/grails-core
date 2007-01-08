/*
 * Copyright 2004-2006 Graeme Rocher
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

import grails.orm.HibernateCriteriaBuilder;
import groovy.lang.Closure;
import groovy.lang.MissingMethodException;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.SessionFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Lets you call criteria inline:
 * 
 *  books = Book.withCriteria {
 *    or {
 *      inList("author.name",
 *             ["Dierk Koenig", "Graeme Rocher"]) 
 *      ilike("title", "Groovy")
 *    }
 * }
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public class WithCriteriaDynamicPersistentMethod extends
		AbstractStaticPersistentMethod {

	private static final Pattern METHOD_PATTERN = Pattern.compile("^withCriteria$");
	private static final String METHOD_SIGNATURE = "withCriteria";

	public WithCriteriaDynamicPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader) {
		super(sessionFactory, classLoader, METHOD_PATTERN);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractStaticPersistentMethod#doInvokeInternal(java.lang.Class, java.lang.String, java.lang.Object[])
	 */
	protected Object doInvokeInternal(Class clazz, String methodName,
			Object[] arguments) {
		if(arguments.length == 0) 
			throw new MissingMethodException(METHOD_SIGNATURE, clazz,arguments);
		
		Object arg1 = arguments[0];
		Object arg2 = arguments.length > 1 ? arguments[1] : null;

		if(!(arg1 instanceof Closure && arg2 == null) && !((arg1 instanceof Map) && (arg2 instanceof Closure)))
			throw new MissingMethodException(METHOD_SIGNATURE, clazz,arguments);
		
		HibernateCriteriaBuilder builder = new HibernateCriteriaBuilder(clazz, getHibernateTemplate().getSessionFactory());
		Closure callable = arg1 instanceof Closure ? (Closure)arg1 : (Closure)arg2;
		
		if(arg1 instanceof Map) {
			BeanWrapper builderBean = new BeanWrapperImpl(builder);
			Map args = (Map)arg1;
			for (Iterator i = args.keySet().iterator(); i.hasNext();) {
				String name = (String) i.next();
				if(builderBean.isWritableProperty(name)) {
					builderBean.setPropertyValue(name, args.get(i));
				}
			}
		}
		
		return builder.invokeMethod("doCall",callable);
	}

}
