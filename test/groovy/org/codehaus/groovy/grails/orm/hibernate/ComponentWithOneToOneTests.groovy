/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 19, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class ComponentWithOneToOneTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class Unit {
    Long id
    Long version

    String name
    String abbreviation
}

class Measurement {
    Long id
    Long version

    Unit unit
    BigDecimal value
    Boolean approximation
}

class BatchAction {
    Long id
    Long version

    Measurement sample
    Measurement sample2
    String name
    static embedded = ['sample','sample2']
}
'''
    }

    void testEmbeddedComponentWithOne2One() {
        def unitClass = ga.getDomainClass("Unit").clazz
        def mClass = ga.getDomainClass("Measurement").clazz
        def bClass = ga.getDomainClass("BatchAction").clazz

        def u = unitClass.newInstance(name:"metres",abbreviation:"m" )
        def u2 = unitClass.newInstance(name:"centimetres",abbreviation:"cm" )

        u.save()
        u2.save()
        
        def m1 = mClass.newInstance(value:1.1, unit:u, approximation:true)
        def m2 = mClass.newInstance(value:2.4, unit:u2, approximation:false)

        m1.save()
        m2.save()
        
        def action = bClass.newInstance(sample:m1, sample2:m2, name:"test")

        action.save()
        session.flush()
        session.clear()

        action = bClass.get(1)
        assert action

        assertEquals 1.1, action.sample.value
        assertEquals "metres", action.sample.unit.name
        assertEquals 2.4, action.sample2.value
        assertEquals "centimetres", action.sample2.unit.name
    }

}