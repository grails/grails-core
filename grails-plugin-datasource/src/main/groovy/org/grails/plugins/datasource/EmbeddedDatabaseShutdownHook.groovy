package org.grails.plugins.datasource

import groovy.sql.Sql
import groovy.transform.CompileStatic

import java.sql.Connection

import javax.sql.DataSource

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.grails.core.lifecycle.ShutdownOperations
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.SmartLifecycle

@CompileStatic
class EmbeddedDatabaseShutdownHook implements SmartLifecycle, ApplicationContextAware {
    private static final Log log=LogFactory.getLog(this)
    private boolean running
    private ApplicationContext applicationContext
    private List<String> embeddedDatabaseBeanNames

    @Override
    public void start() {
        embeddedDatabaseBeanNames = []
        applicationContext.getBeansOfType(DataSource).each { String beanName, DataSource dataSource ->
            if(isEmbeddedH2orHsqldb(dataSource)) {
                embeddedDatabaseBeanNames.add(beanName)
            }
        }
        running = true
    }

    @Override
    public void stop() {
        embeddedDatabaseBeanNames?.each { String beanName ->
            shutdownEmbeddedDatabase(applicationContext.getBean(beanName, DataSource))
        }
        embeddedDatabaseBeanNames = []
        running = false
    }

    @Override
    public boolean isRunning() {
        return running
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE
    }

    @Override
    public boolean isAutoStartup() {
        return true
    }

    @Override
    public void stop(Runnable callback) {
        stop()
        callback.run()
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext
    }
    
    protected boolean isEmbeddedH2orHsqldb(DataSource dataSource) {
        MetaProperty urlProperty = dataSource.hasProperty("url") 
        if (urlProperty) {
            String url = urlProperty.getProperty(dataSource)
            if(url && (url.startsWith('jdbc:h2:') || url.startsWith('jdbc:hsqldb:'))) {
                // don't shutdown remote servers
                if(!(url.startsWith('jdbc:hsqldb:h') || url.startsWith('jdbc:h2:tcp:') || url.startsWith('jdbc:h2:ssl:'))) {
                    return true
                }
            }
        }
        return false
    }

    protected shutdownEmbeddedDatabase(DataSource dataSource) {
        try {
            addShutdownOperation(dataSource.getConnection())
        } catch (e) {
            log.error "Error shutting down datasource", e
        }
    }

    protected addShutdownOperation(Connection connection) {
        // delay the operation until Grails Application is stopping and shutdown hooks are called
        ShutdownOperations.addOperation {
            try {
                Sql sql = new Sql(connection)
                sql.executeUpdate('SHUTDOWN')
            } catch (e) {
                // already closed, ignore
            } finally {
                try { connection?.close() } catch (ignored) {}
            }
        } as Runnable
    }
}
