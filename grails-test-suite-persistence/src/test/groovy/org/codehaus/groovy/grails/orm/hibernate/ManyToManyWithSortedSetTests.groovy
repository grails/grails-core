package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 19, 2007
 */
class ManyToManyWithSortedSetTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class Foo {
    Long id
    Long version
    SortedSet bars

    static hasMany = [bars:Bar]
}

class Bar implements Comparable {
    String name
    int sortOrder

    Long id
    Long version
    Set foos
    static belongsTo = Foo
    static hasMany = [foos:Foo]

    int compareTo(that) {
        sortOrder <=> that.sortOrder
    }
}
'''
    }

    void testManyToManyWithSortedSet() {
        def barClass = ga.getDomainClass("Bar").clazz
        def fooClass = ga.getDomainClass("Foo").clazz

        def bar1 = barClass.newInstance(name:'Bar 1', sortOrder:1)
        assertNotNull bar1.save()
        def bar2 = barClass.newInstance(name:'Bar 2', sortOrder:2)
        assertNotNull bar2.save()

        def foo = fooClass.newInstance()
        foo.addToBars(bar2)
        foo.addToBars(bar1)
        assertNotNull foo.save()

        session.flush()

        session.clear()

        foo = fooClass.get(1)
        assertNotNull foo
        assertEquals "Bar 1", foo.bars.first().name
        assertEquals "Bar 2", foo.bars.last().name
    }
}
