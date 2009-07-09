/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 19, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class CompositeIdWithOneToManyTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class Left {
    Long id
    Long version
    Set centers
    static hasMany = [centers:Center]
}
class Center implements Serializable {
    Long id
    Long version
    Left left
    String foo
    static belongsTo = [ left:Left ]
    static mapping = {
        id composite:['left', 'foo']
    }
}

''')
    }


    void testCompositeIdWithOneToMany() {
        def leftClass = ga.getDomainClass("Left").clazz
        def centerClass = ga.getDomainClass("Center").clazz

        def left = leftClass.newInstance()

        left
                .addToCenters(centerClass.newInstance(foo:"bar1"))
                .addToCenters(centerClass.newInstance(foo:"bar2"))

        left.save()

        session.flush()
        session.clear()

        left = leftClass.get(1)

        assert left
        assertEquals 2, left.centers.size()

        def c1  = centerClass.get(centerClass.newInstance(foo:"bar1", left:left))

        assert c1
        assertEquals "bar1",c1.foo
    }

}