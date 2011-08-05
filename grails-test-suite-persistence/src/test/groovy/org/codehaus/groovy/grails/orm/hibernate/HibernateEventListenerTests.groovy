package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.event.PostDeleteEvent
import org.hibernate.event.PostDeleteEventListener
import org.hibernate.event.PostInsertEvent
import org.hibernate.event.PostInsertEventListener
import org.hibernate.event.SaveOrUpdateEvent
import org.hibernate.event.SaveOrUpdateEventListener
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
        assertTrue eventListeners.saveOrUpdateEventListeners.any { it instanceof TestAuditListener }
        assertFalse eventListeners.postUpdateEventListeners.any { it instanceof TestAuditListener }
    }
}

class EventListenerGrailsPlugin {
    def version = 1
    def doWithSpring = {
        testAuditListener(TestAuditListener)
        hibernateEventListeners(HibernateEventListeners) {
            listenerMap = ['post-insert': testAuditListener,
                           'post-delete': testAuditListener,
                           'save-update': testAuditListener]
        }
    }
}

class TestAuditListener implements PostInsertEventListener, PostDeleteEventListener, SaveOrUpdateEventListener {
    void onPostInsert(PostInsertEvent event) {}
    void onPostDelete(PostDeleteEvent event) {}
    void onSaveOrUpdate(SaveOrUpdateEvent event) {}
}
