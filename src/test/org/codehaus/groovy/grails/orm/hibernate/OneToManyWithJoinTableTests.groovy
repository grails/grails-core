/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 4, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class OneToManyWithJoinTableTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class Thing {
    Long id
    Long version
    String name

}
class ThingGroup {
    Long id
    Long version
    Set things
    Set moreThings
    static hasMany = [things: Thing, moreThings:Thing]
    static mapping = {
        columns {
            things joinTable:true
            moreThings joinTable:'more_things'
        }
    }
    String name
}
        ''')
    }


    void testOneToManyJoinTableMapping() {
        def groupClass = ga.getDomainClass("ThingGroup")
        def thingClass = ga.getDomainClass("Thing")

        def g = groupClass.newInstance()

        g.name = "Group 1"
        def t1 = thingClass.newInstance()
        t1.name = "Bob"
        g.addToThings(t1)
        g.save()

        session.flush()
        session.clear()

        g = groupClass.clazz.get(1)

        def t = thingClass.newInstance()
        t.name = "Fred"
        g.addToThings(t)
        g.save()

        session.flush()
        session.clear()

        g = groupClass.clazz.get(1)
        assertEquals 2, g.things.size()                
    }

}