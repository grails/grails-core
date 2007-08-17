package org.codehaus.groovy.grails.validation;

import org.codehaus.groovy.grails.commons.*;

/**
 * Note there are more tests for DefaultGrailsDomainClass in test/persistence written in Java
 */
class NullableConstraintTests extends GroovyTestCase {

	def gcl

	void setUp() {
		gcl = new GroovyClassLoader()
	}

    void tearDown() {
        gcl = null
    }

    void testNullableConstraint() {
        gcl.parseClass("""
class Project {
    Long id
    Long version
    ProjectStatus status   // nullable set to false
    ProjectInfo info       // nullable set to true
    ProjectVersion number // nullable not set
    static constraints = {
        status(nullable:false)
        info(nullable:true)
    }
}
class ProjectStatus {
    Long id
    Long version
    String desc
}
class ProjectInfo {
    Long id
    Long version
    String blah
}
class ProjectVersion {
    Long id
    Long version
    Double number
}
        """)


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
}
