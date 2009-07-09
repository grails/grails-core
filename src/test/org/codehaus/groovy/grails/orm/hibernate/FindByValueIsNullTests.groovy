package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 22, 2009
 */

public class FindByValueIsNullTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class FindByValueIsNull {
    String name
    Integer age
    FindByValueIsNullB bee
    static belongsTo = [bee:FindByValueIsNullB]
    static constraints = {
        age nullable:true
        bee nullable:true
    }
}
@Entity
class FindByValueIsNullB {
    String name
    FindByValueIsNull aye

    static constraints = {
        aye nullable:true
    }
}
''')
    }

    void testFindByIsNull() {
        if(notYetImplemented(this)) return
        // test for GRAILS-4601
        
        def domainClass = ga.getDomainClass("FindByValueIsNull").clazz

        assertNotNull "should have saved domain",domainClass.newInstance(name:"Bob", age:11).save()
        assertNotNull "should have saved domain",domainClass.newInstance(name:"Fred").save()


        session.flush()
        session.clear()

        def results = domainClass.findAllByAgeIsNull()

        assertNotNull "should have returned results", results

        assertEquals 1, results.size()

        assertEquals "Fred", results[0].name

        session.clear()

        results = domainClass.findAllByBeeIsNull()

        assertNotNull "should have returned results", results

        assertEquals 2, results.size()


    }


}