/* Copyright 2007 the original author or authors.
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


import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.beans.SimpleTypeConverter;

import groovy.lang.GroovyObject;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * The "getAll" static persistent method for Grails domain classes. This method
 * takes a list of ids and returns the list of instances with provided ids in
 * the same order.
 * 
 * eg. Account.getAll(2,4,1) or Account.getAll([2,4,1])
 * 
 * When called without params this method returns list of all instances of the
 * class.
 * 
 * @author Sergey Nebolsin
 */
public class GetAllPersistentMethod
		extends AbstractStaticPersistentMethod {

	private static final Pattern	METHOD_PATTERN		= Pattern.compile("^getAll$");
	public static final String		METHOD_SIGNATURE	= "getAll";
	private GrailsApplication		application;
    private SimpleTypeConverter typeConverter = new SimpleTypeConverter();

    public GetAllPersistentMethod(GrailsApplication application,
			SessionFactory sessionFactory, ClassLoader classLoader) {
		super(sessionFactory, classLoader, METHOD_PATTERN);
		this.application = application;
	}

	protected Object doInvokeInternal(final Class clazz, String methodName, Object[] arguments) {

		// if there are no arguments list all
		if (arguments == null || arguments.length == 0) {
			return super.getHibernateTemplate().loadAll(clazz);
		}

		if (arguments.length == 1 && List.class.isAssignableFrom(arguments[0].getClass())) {
			arguments = ((List) arguments[0]).toArray();
		}

		List result = new ArrayList();

        final GrailsDomainClass domainClass = (GrailsDomainClass) this.application.getArtefact(
                DomainClassArtefactHandler.TYPE, clazz.getName());

		if (domainClass != null) {
			Class identityType = domainClass.getIdentifier().getType();
			final List args = new ArrayList();

			// convert arguments to required identifier type
			for (int i = 0; i < arguments.length; i++) {
				if (!identityType.isAssignableFrom(arguments[i].getClass())) {
                    if(arguments[i] instanceof Number && Long.class.equals(identityType)) {
                        args.add(DefaultGroovyMethods.toLong((Number)arguments[i]));
                    }
                    else {
                        args.add(typeConverter.convertIfNecessary(arguments[i], identityType));
                    }
				} else {
					args.add(arguments[i]);
				}
			}
			
			result = super.getHibernateTemplate().executeFind(new HibernateCallback() {
				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					Criteria c = session.createCriteria(clazz);
					c.add(Restrictions.in(domainClass.getIdentifier().getName(), args));
					return c.list();
				}
			});
			
			// At this stage we have unsorted list of result objects and we want to order it 
			// corresponding to original ids order
			
			Map idMap = new HashMap();
			for( Iterator it = result.iterator();it.hasNext();) {
				GroovyObject obj = (GroovyObject) it.next();
				idMap.put(obj.getProperty(domainClass.getIdentifier().getName()), obj);
			}
			
			result.clear();
			
			for( Iterator it = args.iterator(); it.hasNext(); ){
				Object identifier = it.next();
				result.add( idMap.get(identifier) );
			}
		}

		return result;
	}

}