package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.proxy.HibernateProxy

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class UnwrapProxyInDynamicFinderTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class UnrwapProxyInDynamicFinderProject {

    String name
    UnrwapProxyInDynamicFinderProjectStatus  projectStatus
}

@Entity
class UnrwapProxyInDynamicFinderProjectStatus {

    String name
    String description

    static getSigned() {
        UnrwapProxyInDynamicFinderProjectStatus.findByName("signed")
    }
}
'''
    }

    void testReturnNonProxiedInstanceInFinder() {
        def Project = ga.getDomainClass("UnrwapProxyInDynamicFinderProject").clazz
        def ProjectStatus = ga.getDomainClass("UnrwapProxyInDynamicFinderProjectStatus").clazz

        def status = ProjectStatus.newInstance(name:"signed", description:"foo")

        assertNotNull "should have saved", status.save(flush:true)
        assertNotNull "should have saved", Project.newInstance(name:"foo", projectStatus:status).save(flush:true)

        session.clear()

        def project = Project.get(1)
        assertEquals "signed", project.projectStatus.name
        assertFalse "Should not return proxy from finder!", ProjectStatus.signed instanceof HibernateProxy
        assertFalse "Should not return proxy from finder!", ProjectStatus.get(status.id) instanceof HibernateProxy
        assertFalse "Should not return proxy from finder!", ProjectStatus.read(status.id) instanceof HibernateProxy
        assertFalse "Should not return proxy from finder!",
            ProjectStatus.find("from UnrwapProxyInDynamicFinderProjectStatus as p where p.name='signed'") instanceof HibernateProxy
        assertFalse "Should not return proxy from finder!", ProjectStatus.findWhere(name:'signed') instanceof HibernateProxy

        def c = ProjectStatus.createCriteria()
        def result = c.get {
            eq 'name', 'signed'
        }

        assertFalse "Should not return proxy from criteria!", result instanceof HibernateProxy
    }
}
