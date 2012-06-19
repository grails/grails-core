package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 21, 2009
 */
class UnidirectionalOneToManyWithJoinTableTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class Employee {
    static hasMany = [projects: Project]
    static mapping = { projects joinTable: [name: 'EMP_PROJ', column: 'PROJECT_ID', key: 'EMPLOYEE_ID'] }
}

@Entity
class Project { static belongsTo = Employee }
'''
    }

    void testUnidirectionalOneToManyWithExplicityJoinTable() {
        // will throw an exception if join table incorrectly mapped
        session.connection().prepareStatement("SELECT PROJECT_ID, EMPLOYEE_ID FROM EMP_PROJ").executeQuery()
    }
}
