/* Copyright (C) 2011 SpringSource
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
package org.codehaus.groovy.grails.orm.hibernate;

import org.hibernate.SessionFactory;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.model.MappingContext;

import java.util.Map;

/**
 * Datastore implementation that uses a Hibernate SessionFactory underneath
 * 
 * @author Graeme Rocher
 * @since 1.0
 * 
 */
public class HibernateDatastore extends AbstractDatastore {

	private SessionFactory sessionFactory;
	
	
	public HibernateDatastore(MappingContext mappingContext,
			SessionFactory sessionFactory) {
		super(mappingContext);
		this.sessionFactory = sessionFactory;
		super.initializeConverters(mappingContext);
	}
	
	

	/**
	 * @return The Hibernate {@link SessionFactory} being used by this datastore instance
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}




	@Override
	protected Session createSession(Map<String, String> connectionDetails) {
		return new HibernateSession(this, this.sessionFactory);
	}

}
