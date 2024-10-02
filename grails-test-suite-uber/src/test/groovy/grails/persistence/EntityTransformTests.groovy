package grails.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class EntityTransformTests {

    @Delegate protected GroovyShell shell

    @BeforeEach
    void setup() {
        shell = createNewShell()
    }


    protected GroovyShell createNewShell() {
        return new GroovyShell()
    }


    // test for http://jira.codehaus.org/browse/GRAILS-5238
    void testGRAILS_5238() {
        def p = evaluate('''
import grails.persistence.*

@Entity
class Permission {
    String permission

    static belongsTo = [ user: User ]

    void setOwner(User owner) {
        this.user = owner
    }

    User getOwner() {
        return user
    }
}

@Entity
class User {
    String username
}

u = new User(username:"bob")
p = new Permission(user:u, permission:"uber")
''')

        assertEquals "User", p.user.class.name
        assertEquals "User", p.class.methods.find { it.name == 'getUser' }.returnType.name
    }

    void testDefaultConstructorBehaviourNotOverriden() {
        def entity = evaluate("""
          import grails.persistence.*
          @Entity
          class EntityTransformTest {

                boolean enabled
                int cash
                EntityTransformTest() {
                    enabled = true
                    cash = 30
                }
          }
          new EntityTransformTest()
        """)

        assert entity != null
        assert entity.enabled
        assert entity.cash == 30

    }

    void testConstructorBehaviourNotOverriden() {
        def entity = evaluate("""
          import grails.persistence.*
          @Entity
          class EntityTransformTest2 {

                boolean enabled
                int cash
                EntityTransformTest2() {
                    enabled = true
                    cash = 30
                }
          }
          new EntityTransformTest2(cash: 42)
        """)

        assert entity != null
        assert entity.enabled
        assert entity.cash == 42
    }

    void testAnnotatedEntity() {
        def entity = evaluate("""
          import grails.persistence.*
          @Entity
          class EntityTransformTest3 {

               static belongsTo = [one:EntityTransformTest3]
               static hasMany = [many:EntityTransformTest3]

               static constraints = {
                    id bindable:true
               }
          }
          new EntityTransformTest3()
        """)

        assertNull entity.id
        assertNull entity.version

        entity.many = new HashSet()
        assertEquals 0, (int) entity.many.size()

        entity.one = entity.class.getDeclaredConstructor().newInstance()

        assertNotNull entity.one
    }

    @Test
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
