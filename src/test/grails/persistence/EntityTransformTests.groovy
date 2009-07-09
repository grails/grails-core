package grails.persistence
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Dec 17, 2008
 */

public class EntityTransformTests extends GroovyShellTestCase{

    void testAnnotatedEntity() {
        def entity = evaluate("""
          import grails.persistence.*
          @Entity
          class EntityTransformTest {

               static belongsTo = [one:EntityTransformTest]
               static hasMany = [many:EntityTransformTest]
          }
          new EntityTransformTest(id:1L, version:2L)
        """)


        assertEquals 1L, entity.id
        assertEquals 2L, entity.version

        entity.many = new HashSet()
        assertEquals 0, entity.many.size()

        entity.one = entity.class.newInstance()

        assertNotNull entity.one
    }

    void testToStringOverrideTests() {
        def entities = evaluate('''

        import grails.persistence.*
        @Entity
        class Personnel {
            String lastName
            String firstName
            String toString() {"${firstName}, ${lastName}"}
        }

        @Entity
        class Approver extends Personnel {

        }
        [new Approver(firstName:"joe", lastName:"bloggs"), new Personnel(firstName:"jack", lastName:"dee") ]
        ''')


        assertEquals "joe, bloggs", entities[0].toString()
        assertEquals "jack, dee", entities[1].toString()
    }

}