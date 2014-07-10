package org.grails.web.servlet.mvc;

import grails.core.support.proxy.EntityProxyHandler;

public class MockHibernateProxyHandler implements EntityProxyHandler {
    public boolean isProxy(Object o) {
        return false;
    }

    public Object unwrapIfProxy(Object instance) {
        return null;
    }

    public boolean isInitialized(Object o) {
        return false;
    }

    public void initialize(Object o) {
    }

    public boolean isInitialized(Object obj, String associationName) {
        return false;
    }

    public Object getProxyIdentifier(Object o) {
        return null;
    }

    public Class<?> getProxiedClass(Object o) {
        return null;
    }
}
