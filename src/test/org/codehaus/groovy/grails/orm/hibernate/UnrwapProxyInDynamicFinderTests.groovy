package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class UnrwapProxyInDynamicFinderTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Project {

	String         name
	ProjectStatus  projectStatus

}
@Entity
class ProjectStatus {

	String name
	String description

	static getSigned()
	{
		ProjectStatus.findByName("signed")
	}

}
''')
    }


    void testReturnNonProxiedInstanceInFinder() {
        def Project = ga.getDomainClass("Project").clazz
        def ProjectStatus = ga.getDomainClass("ProjectStatus").clazz

        def status = ProjectStatus.newInstance(name:"signed", description:"foo")

        assert status.save(flush:true) : "should have saved"
        assert Project.newInstance(name:"foo", projectStatus:status).save(flush:true) : "should have saved"

        session.clear()

        def project = Project.get(1)

        assertEquals "signed", project.projectStatus.name

        assert !(ProjectStatus.signed instanceof org.hibernate.proxy.HibernateProxy) : "Should not return proxy from finder!"

        assert !(ProjectStatus.get(status.id) instanceof org.hibernate.proxy.HibernateProxy) : "Should not return proxy from finder!"

        assert !(ProjectStatus.read(status.id) instanceof org.hibernate.proxy.HibernateProxy) : "Should not return proxy from finder!"

        assert !(ProjectStatus.find("from ProjectStatus as p where p.name='signed'") instanceof org.hibernate.proxy.HibernateProxy) : "Should not return proxy from finder!"

        assert !(ProjectStatus.findWhere(name:'signed') instanceof org.hibernate.proxy.HibernateProxy) : "Should not return proxy from finder!"

        def c = ProjectStatus.createCriteria()
        def result = c.get {
            eq 'name', 'signed'
        }

        assert !(result instanceof org.hibernate.proxy.HibernateProxy) : "Should not return proxy from criteria!"
    }
}