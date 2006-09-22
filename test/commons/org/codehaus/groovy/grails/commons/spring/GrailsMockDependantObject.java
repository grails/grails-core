package org.codehaus.groovy.grails.commons.spring;

import org.hibernate.SessionFactory;

public class GrailsMockDependantObject {

	SessionFactory sessionFactory;

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}
