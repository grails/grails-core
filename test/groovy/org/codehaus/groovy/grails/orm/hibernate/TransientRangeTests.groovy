package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class TransientRangeTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class AreaType implements Serializable {

    static transients = ["areaRange"]

    String name
    Integer rangeFrom
    Integer rangeTo


    static constraints = {
        name(blank:false, unique:true)

    }

    String toString() {
        return name
    }

    Range getAreaRange() {
        return rangeFrom&&rangeTo ? rangeFrom..rangeTo : 0..0
    }

    void setAreaRange(Range range) {
        this.rangeFrom = range.first()
        this.rangeTo = range.last()
    }
}
''')
    }


    void testTransientRange() {
        def AreaType = ga.getDomainClass("AreaType").clazz
        def area = AreaType.newInstance(name:"testArea", areaRange:1..10)
		assert true, area.save()
    }

}