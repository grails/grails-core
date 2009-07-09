/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Jul 13, 2007
 * Time: 9:24:42 AM
 * 
 */

package org.codehaus.groovy.grails.orm.hibernate

class ClassHeirarchyInheritanceTests extends AbstractGrailsHibernateTests {
    void testPolymorphicQuery() {
        def carClass = ga.getDomainClass("Car").clazz
        def alpha = ga.getDomainClass("Alpha").newInstance()
        def fiatClass = ga.getDomainClass("Fiat")
        def fiat = fiatClass.newInstance()
        def ferrari = ga.getDomainClass("Ferrari").newInstance()

        fiat.type = "cheap"
        alpha.type = "luxury"
        ferrari.type = "luxury"

        fiat.save()
        alpha.save()
        ferrari.save()

        def cars = carClass.findAll("from Car as c where c.type='luxury'")
        assertEquals 2, cars.size()

        def fiats = fiatClass.clazz.list()

        assertEquals 1, fiats.size()
    }

    void onTearDown() {}
    
    void onSetUp() {
        gcl.parseClass('''
class Car { Long id;Long version;String type;}
class Alpha extends Car { }
class Fiat extends Car { }
class Ferrari extends Car { }
        ''')
    }
}