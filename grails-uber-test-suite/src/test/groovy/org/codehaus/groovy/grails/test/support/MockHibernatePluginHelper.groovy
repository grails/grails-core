package org.codehaus.groovy.grails.test.support

import org.codehaus.groovy.grails.plugins.GrailsPlugin

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 22/02/2011
 * Time: 12:40
 * To change this template use File | Settings | File Templates.
 */
class MockHibernatePluginHelper {
    public static GrailsPlugin FAKE_HIBERNATE_PLUGIN = [getName: { -> 'hibernate' }] as GrailsPlugin
}
