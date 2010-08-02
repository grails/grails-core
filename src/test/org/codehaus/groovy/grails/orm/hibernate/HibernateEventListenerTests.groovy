package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.event.PostDeleteEvent
import org.hibernate.event.PostDeleteEventListener
import org.hibernate.event.PostInsertEvent
import org.hibernate.event.PostInsertEventListener

import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin

/**
 * @author Burt Beckwith
 */
class HibernateEventListenerTests extends AbstractGrailsHibernateTests {

    private plugin

    protected void afterPluginInitialization() {
        plugin = new DefaultGrailsPlugin(EventListenerGrailsPlugin, ga)
        mockManager.registerMockPlugin plugin
        plugin.manager = mockManager
    }

    protected void doWithRuntimeConfiguration(dependentPlugins, springConfig) {
        super.doWithRuntimeConfiguration dependentPlugins, springConfig
        plugin.doWithRuntimeConfiguration springConfig
    }

    void testDoRuntimeConfiguration() {
        def eventListeners = appCtx.sessionFactory.eventListeners
        assertTrue eventListeners.postInsertEventListeners.any { it instanceof TestAuditListener }
        assertTrue eventListeners.postDeleteEventListeners.any { it instanceof TestAuditListener }
        assertFalse eventListeners.postUpdateEventListeners.any { it instanceof TestAuditListener }
    }
}

class EventListenerGrailsPlugin {
    def version = 1
    def doWithSpring = {
        testAuditListener(TestAuditListener)
        hibernateEventListeners(HibernateEventListeners) {
            listenerMap = ['post-insert': testAuditListener,
                           'post-delete': testAuditListener]
        }
    }
}

class TestAuditListener implements PostInsertEventListener, PostDeleteEventListener {
    void onPostInsert(PostInsertEvent event) {}
    void onPostDelete(PostDeleteEvent event) {}
}
