package org.codehaus.groovy.grails.orm.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.classic.Session;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;

import javax.naming.NamingException;
import javax.naming.Reference;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/**
 * Dummy SessionFactory implementation useful for unit testing
 * @author Dmitriy Kopylenko
 * @since 0.5
 */
public class SessionFactoryAdapter implements SessionFactory {
	
	public Reference getReference() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() throws HibernateException {
		// TODO Auto-generated method stub
		
	}

	public void evict(Class arg0, Serializable arg1) throws HibernateException {
		// TODO Auto-generated method stub
		
	}

	public void evict(Class arg0) throws HibernateException {
		// TODO Auto-generated method stub
		
	}

	public void evictCollection(String arg0, Serializable arg1)
			throws HibernateException {
		// TODO Auto-generated method stub
		
	}

	public void evictCollection(String arg0) throws HibernateException {
		// TODO Auto-generated method stub
		
	}

	public void evictEntity(String arg0, Serializable arg1)
			throws HibernateException {
		// TODO Auto-generated method stub
		
	}

	public void evictEntity(String arg0) throws HibernateException {
		// TODO Auto-generated method stub
		
	}

	public void evictQueries() throws HibernateException {
		// TODO Auto-generated method stub
		
	}

	public void evictQueries(String arg0) throws HibernateException {
		// TODO Auto-generated method stub
		
	}

	public Map getAllClassMetadata() throws HibernateException {
		// TODO Auto-generated method stub
		return null;
	}

	public Map getAllCollectionMetadata() throws HibernateException {
		// TODO Auto-generated method stub
		return null;
	}

	public ClassMetadata getClassMetadata(Class arg0) throws HibernateException {
		// TODO Auto-generated method stub
		return null;
	}

	public ClassMetadata getClassMetadata(String arg0)
			throws HibernateException {
		// TODO Auto-generated method stub
		return null;
	}

	public CollectionMetadata getCollectionMetadata(String arg0)
			throws HibernateException {
		// TODO Auto-generated method stub
		return null;
	}

	public Session getCurrentSession() throws HibernateException {
		// TODO Auto-generated method stub
		return null;
	}

	public Set getDefinedFilterNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public FilterDefinition getFilterDefinition(String arg0)
			throws HibernateException {
		// TODO Auto-generated method stub
		return null;
	}

	public Statistics getStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	public Session openSession() throws HibernateException {
		// TODO Auto-generated method stub
		return null;
	}

	public Session openSession(Connection arg0, Interceptor arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Session openSession(Connection arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Session openSession(Interceptor arg0) throws HibernateException {
		// TODO Auto-generated method stub
		return null;
	}

	public StatelessSession openStatelessSession() {
		// TODO Auto-generated method stub
		return null;
	}

	public StatelessSession openStatelessSession(Connection arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
