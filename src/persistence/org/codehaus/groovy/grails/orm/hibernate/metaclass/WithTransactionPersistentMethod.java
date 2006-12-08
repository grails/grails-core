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

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;

import java.util.regex.Pattern;

import org.codehaus.groovy.grails.orm.support.TransactionManagerAware;
import org.hibernate.SessionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public class WithTransactionPersistentMethod extends
		AbstractStaticPersistentMethod  implements TransactionManagerAware {

	private static final Pattern METHOD_PATTERN = Pattern.compile("^withTransaction$");
	private static final String METHOD_SIGNATURE = "withTransaction";
	private TransactionTemplate transactionTemplate;

	public WithTransactionPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader) {
		super(sessionFactory, classLoader, METHOD_PATTERN);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractStaticPersistentMethod#doInvokeInternal(java.lang.Class, java.lang.String, java.lang.Object[])
	 */
	protected Object doInvokeInternal(Class clazz, String methodName,
			Object[] arguments) {
		if(arguments.length == 0) 
			throw new MissingMethodException(METHOD_SIGNATURE, clazz,arguments);
		if(!(arguments[0] instanceof Closure))
			throw new MissingMethodException(METHOD_SIGNATURE, clazz,arguments);
		
		final Closure callable = (Closure)arguments[0];
		
		if(transactionTemplate.getTransactionManager() == null) 
			throw new IllegalStateException("Cannot use method [withTransaction] without a PlatformTransactionManager instance");
		
		return transactionTemplate.execute(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				return callable.call(status);
			}
			
		});
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

}
