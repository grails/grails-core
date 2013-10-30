package org.codehaus.groovy.grails.domain

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Commons

import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent


/**
 * Clears static Grails "instance api" instances from domain classes when 
 * ApplicationContext's ContextClosedEvent is received. 
 * 
 * 
 * @author Lari Hotari
 *
 */
@Commons
class GrailsDomainClassCleaner implements ApplicationListener<ContextClosedEvent>, ApplicationContextAware  {
    protected GrailsApplication grailsApplication
    protected ApplicationContext applicationContext
    
    public GrailsDomainClassCleaner(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    public void onApplicationEvent(ContextClosedEvent event) {
        if(event.applicationContext == this.applicationContext || this.applicationContext == null) {
            clearAllStaticApiInstances()
        }
    }

    protected clearAllStaticApiInstances() {
        for (dc in grailsApplication.domainClasses) {
            clearStaticApiInstances(dc.clazz)
        }
    }

    protected clearStaticApiInstances(Class clazz) {
        clazz.metaClass.getProperties().each { MetaProperty metaProperty ->
            if(Modifier.isStatic(metaProperty.getModifiers()) && metaProperty.name ==~ /^(instance|static).+Api$/) {
                log.info("Clearing static property ${metaProperty.name} in ${clazz.name}")
                try {
                    metaProperty.setProperty(clazz, null)
                } catch (e) {
                    log.warn("Error clearing static property ${metaProperty.name} in ${clazz.name}", e)
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext
    }
}
