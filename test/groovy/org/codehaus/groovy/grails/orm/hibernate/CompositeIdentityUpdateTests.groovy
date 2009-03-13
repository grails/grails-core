package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Mar 13, 2008
 */
class CompositeIdentityUpdateTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class T implements Serializable {
    Long id
    Long version
    String x
    String y
    String name
    static mapping = {
        id composite:['x', 'y']
    }
}
'''
    }


    void testUpdateObjectWithCompositeId() {
        def tClass = ga.getDomainClass("T").clazz


        def t = tClass.newInstance(x:"1", y:"2", name:"John")

        assert t.save(flush:true)

        session.clear()

        t = tClass.get(tClass.newInstance(x:"1", y:"2"))

        assert t

        t.name = "Fred"

        t.save(flush:true)

        session.clear()


        t = tClass.get(tClass.newInstance(x:"1", y:"2"))

        assertEquals "Fred", t.name


    }

}