package org.codehaus.groovy.grails.commons

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.core.util.ClassPropertyFetcher} instead
 */
@CompileStatic
@Deprecated
class ClassPropertyFetcher extends org.grails.core.util.ClassPropertyFetcher {
    ClassPropertyFetcher(Class<?> clazz, org.grails.core.util.ClassPropertyFetcher.ReferenceInstanceCallback callback) {
        super(clazz, callback)
    }
}
