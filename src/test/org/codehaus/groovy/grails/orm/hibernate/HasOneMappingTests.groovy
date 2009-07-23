package org.codehaus.groovy.grails.orm.hibernate

import java.sql.Connection

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class HasOneMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class HasOneFace {
    String name
    static hasOne = [nose:HasOneNose]

}
@Entity
class HasOneNose {
    String shape
    HasOneFace face
}
''')
    }

    void testHasOneMapping() {
        def Face = ga.getDomainClass("HasOneFace").clazz
        def Nose = ga.getDomainClass("HasOneNose").clazz

        def f = Face.newInstance(name:"Bob", nose: Nose.newInstance(shape:"round"))

        assertNotNull "entities should be associated",f.nose
        assertNotNull "entities should be associated",f.nose.face
        f.save(flush:true)

        session.clear()

        f = Face.get(1)



        assertNotNull "should have been able to read back nose",f.nose

        // now test table structure
        Connection c = session.connection()
        def r = c.prepareStatement("select * from has_one_face").executeQuery()
        r.next()
        r.getLong("id")
        r.getLong("version")
        r.getString("name")
        shouldFail {
            r.getLong("face_id")
        }

        r = c.prepareStatement("select * from has_one_nose").executeQuery()
        r.next()
        r.getLong("id")
        r.getLong("version")
        r.getString("shape")
        r.getLong("face_id") // association key stored in child

        

        // now test delete
        f.delete(flush:true)

        assertEquals 0, Face.count()
        assertEquals 0, Nose.count()

    }


}