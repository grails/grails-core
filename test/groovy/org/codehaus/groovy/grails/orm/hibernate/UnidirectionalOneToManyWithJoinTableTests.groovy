package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 21, 2009
 */

public class UnidirectionalOneToManyWithJoinTableTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Employee {
    static hasMany = [projects: Project]
    static mapping = { projects joinTable: [name: 'EMP_PROJ', column: 'PROJECT_ID', key: 'EMPLOYEE_ID'] }
}

@Entity
class Project { static belongsTo = Employee }
''')
    }


    void testUnidirectionalOneToManyWithExplicityJoinTable() {
        def conn = session.connection()

        // will throw an exception if join table incorrectly mapped
        conn.prepareStatement("SELECT PROJECT_ID, EMPLOYEE_ID FROM EMP_PROJ").executeQuery()
        
    }
}