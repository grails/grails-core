package org.grails.validation

import grails.core.DefaultGrailsApplication
import grails.web.databinding.DataBindingUtils

 /**
 * Note there are more tests for DefaultGrailsDomainClass in test/persistence written in Java
 */
class NullableConstraintTests extends GroovyTestCase {

    def gcl

    protected void setUp() {
        super.setUp()

        gcl = new GroovyClassLoader()
        gcl.parseClass("""
@grails.persistence.Entity
class Project {
    Long id
    Long version
    ProjectStatus status = new ProjectStatus()  // nullable set to false
    ProjectInfo info = new ProjectInfo()      // nullable set to true
    ProjectVersion number // nullable not set
    String name
    String group

    static constraints = {
        status(nullable:false)
        info(nullable:true)
        name nullable:false
        group nullable:true
    }
}
@grails.persistence.Entity
class ProjectStatus {
    Long id
    Long version
    String desc
}
@grails.persistence.Entity
class ProjectInfo {
    Long id
    Long version
    String blah

    static constraints = {
      blah nullable:true
    }
}
@grails.persistence.Entity
class ProjectVersion {
    Long id
    Long version
    Double number
}
        """)
    }

    void testNullableConstraint() {

        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()

        def project = ga.getDomainClass("Project")

        assertNotNull project

        def constraints = project.constrainedProperties

        assertTrue constraints.status?.hasAppliedConstraint("nullable")
        assertTrue constraints.info?.hasAppliedConstraint("nullable")
        assertTrue constraints.number?.hasAppliedConstraint("nullable")

        assertTrue constraints.info.nullable
        assertFalse constraints.status.nullable
        assertFalse constraints.number.nullable
    }

    void testBindToNullable() {
        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()

        def projectDomain = ga.getDomainClass("Project")
        def projectClass = projectDomain.clazz

        def projectInfoDomain = ga.getDomainClass("ProjectInfo")
        def projectInfoClass = projectInfoDomain.clazz

        projectClass.metaClass.getConstraints = {-> projectDomain.constrainedProperties }
        projectInfoClass.metaClass.getConstraints = { -> projectInfoDomain.constrainedProperties }

        def project = projectClass.newInstance()

        DataBindingUtils.bindObjectToInstance(project, ['info.blah':'', name:'test', group:'' ])

        assertFalse project.errors.hasErrors()
        assertNull "should have bound String property to null with nullable:true",project.group
        assertNull "should have bound nested String property to null with nullable:true", project.info.blah
        assertEquals "test", project.name
    }
}
