package org.codehaus.groovy.grails.test.support

import grails.plugins.GrailsPlugin

/**
 * @author graemerocher
 */
class MockHibernatePluginHelper {
    public static GrailsPlugin FAKE_HIBERNATE_PLUGIN = [getName: { -> 'hibernate' }] as GrailsPlugin
}
