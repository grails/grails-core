package org.codehaus.groovy.grails.orm.hibernate

/**
 * Tests the isDirty and getPersistentValue methods.
 *
 * @author Burt Beckwith
 */
class DirtyTests extends AbstractGrailsHibernateTests {

    void testIsDirty() {
        def Dirt = ga.getDomainClass('Dirt').clazz

        def d = Dirt.newInstance(pr1: 'pr1', pr2: new Date(), pr3: 123)
        d.save(flush: true, failOnError: true)
        session.clear()

        d = Dirt.get(d.id)
        assertNotNull d

        assertFalse d.isDirty('pr1')
        assertFalse d.isDirty('pr2')
        assertFalse d.isDirty('pr3')
        assertFalse d.isDirty()

        d.pr1 = d.pr1.reverse()
        assertTrue d.isDirty('pr1')
        assertFalse d.isDirty('pr2')
        assertFalse d.isDirty('pr3')
        assertTrue d.isDirty()

        d.pr1 = d.pr1.reverse()
        d.pr2++
        assertFalse d.isDirty('pr1')
        assertTrue d.isDirty('pr2')
        assertFalse d.isDirty('pr3')
        assertTrue d.isDirty()

        d.pr2--
        d.pr3++
        assertFalse d.isDirty('pr1')
        assertFalse d.isDirty('pr2')
        assertTrue d.isDirty('pr3')
        assertTrue d.isDirty()
    }

    void testGetDirtyPropertyNames() {
        def Dirt = ga.getDomainClass('Dirt').clazz

        def d = Dirt.newInstance(pr1: 'pr1', pr2: new Date(), pr3: 123)
        d.save(flush: true, failOnError: true)
        session.clear()

        d = Dirt.get(d.id)
        assertNotNull d

        assertFalse d.isDirty()
        assertEquals 0, d.dirtyPropertyNames.size()

        d.pr1 = d.pr1.reverse()
        assertTrue d.isDirty()
        assertEquals(['pr1'], d.dirtyPropertyNames)

        d.pr2++
        assertTrue d.isDirty()
        assertEquals(['pr1', 'pr2'], d.dirtyPropertyNames.sort())

        d.pr3++
        assertTrue d.isDirty()
        assertEquals(['pr1', 'pr2', 'pr3'], d.dirtyPropertyNames.sort())
    }

    void testGetPersistentValue() {
        def Dirt = ga.getDomainClass('Dirt').clazz

        String pr1 = 'pr1'
        Date pr2 = new Date()
        int pr3 = 123

        def d = Dirt.newInstance(pr1: pr1, pr2: pr2, pr3: pr3)
        d.save(flush: true, failOnError: true)
        session.clear()

        d = Dirt.get(d.id)
        assertNotNull d

        d.pr1.reverse()
        d.pr2++
        d.pr3++

        assertEquals pr1, d.getPersistentValue('pr1')
        assertEquals pr2.time, d.getPersistentValue('pr2').time
        assertEquals pr3, d.getPersistentValue('pr3')
    }

    void testGetPersistentValueRead() {
        def Dirt = ga.getDomainClass('Dirt').clazz

        String pr1 = 'pr1'
        Date pr2 = new Date()
        int pr3 = 123

        def d = Dirt.newInstance(pr1: pr1, pr2: pr2, pr3: pr3)
        d.save(flush: true, failOnError: true)
        session.clear()

        d = Dirt.read(d.id)
        assertNotNull d

        d.pr1.reverse()
        d.pr2++
        d.pr3++

        assertNull d.getPersistentValue('pr1')
        assertNull d.getPersistentValue('pr2')
        assertNull d.getPersistentValue('pr3')
    }

    void testNewInstances() {
        def Dirt = ga.getDomainClass('DirtWithValidator').clazz

        def d = Dirt.newInstance(pr1: 'pr1', pr2: new Date(), pr3: 123)
        assertFalse d.isDirty()
        assertFalse d.isDirty('pr1')
        assertFalse d.isDirty('pr2')
        assertFalse d.isDirty('pr3')
        assertEquals 0, d.getDirtyPropertyNames().size()

        d.save(flush: true, failOnError: true)
        session.clear()

        assertNotNull Dirt.get(d.id)
    }

    protected void onSetUp() {
        gcl.parseClass("""
import grails.persistence.*

@Entity
class Dirt {

    String pr1
    Date pr2
    Integer pr3
}

@Entity
class DirtWithValidator {

    String pr1
    Date pr2
    Integer pr3

    static constraints = {
        pr1(validator: { val, obj -> if (obj.isDirty('pr3')) {} })
    }
}
""")
    }
}
