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

import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.metaclass.AbstractStaticMethodInvocation;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.Assert;

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
	private ClassLoader classLoader = null;
	private HibernateTemplate hibernateTemplate;

    public AbstractStaticPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader, Pattern pattern) {
		super();
		setPattern(pattern);
		this.classLoader = classLoader;
        Assert.notNull(sessionFactory, "Session factory is required!");
        hibernateTemplate=new HibernateTemplate(sessionFactory);
        hibernateTemplate.setCacheQueries(GrailsHibernateUtil.isCacheQueriesByDefault());
    }

    protected HibernateTemplate getHibernateTemplate() {
    	return hibernateTemplate;
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

}
