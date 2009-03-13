/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 14, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class ListReorderingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class Bar {

    Long id
    Long version
    String name
    Foo foo
    static belongsTo = Foo
}
class Foo {
    Long id
    Long version
    String name

    List bars
    static hasMany = [bars : Bar]
}

''')
    }



    void testReorderList() {
        def fooClass = ga.getDomainClass("Foo").clazz
        def foo = fooClass.newInstance(name:"foo")
                            .addToBars(name:"bar1")
                            .addToBars(name:"bar2")

        assertEquals foo,foo.bars[0].foo
        assertEquals foo,foo.bars[1].foo


        foo.save()

        session.flush()
        session.clear()

        foo = fooClass.get(1)


        assert foo
        assertEquals 2, foo.bars.size()
        assertEquals "bar1", foo.bars[0].name
        assertEquals "bar2", foo.bars[1].name

        def tmp = foo.bars[0]
        foo.bars[0] = foo.bars[1]
        foo.bars[1] = tmp

        session.flush()
        session.clear()

        foo = fooClass.get(1)
        assertEquals 2, foo.bars.size()

        assertEquals "bar2", foo.bars[0].name
        assertEquals "bar1", foo.bars[1].name

    }

}