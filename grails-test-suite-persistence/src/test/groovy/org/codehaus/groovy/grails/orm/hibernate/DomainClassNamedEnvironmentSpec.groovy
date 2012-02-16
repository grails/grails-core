package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 1/25/12
 * Time: 2:33 PM
 * To change this template use File | Settings | File Templates.
 */
class DomainClassNamedEnvironmentSpec extends GormSpec{
    @Override
    List getDomainClasses() {
        [Environment]
    }
    
    void "Test that a domain class named environment doesn't conflict with Spring's environment bean"() {
        when:"A service is called that injects the environment"
            def environmentService = applicationContext.getBean(EnvironmentService)
        
        then:"The service is usable"
            environmentService.listEnvironments().size() == 0
    }

    @Override
    protected void initializeApplication() {
        grailsApplication.initialise()
        domainClasses?.each { dc -> grailsApplication.addArtefact 'Domain', dc }
        grailsApplication.addArtefact("Service", EnvironmentService)
        grailsApplication.setApplicationContext(applicationContext)
        parentCtx.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication)

    }


}
@Entity
class Environment {
    String name
}

class EnvironmentService {
    Environment environment
    org.springframework.core.env.Environment springEnvironment
    List listEnvironments() {
        springEnvironment != null
        Environment.list()
    }
}
