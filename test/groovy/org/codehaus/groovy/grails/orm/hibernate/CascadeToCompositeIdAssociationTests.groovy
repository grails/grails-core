package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 29, 2008
 */
class CascadeToCompositeIdAssociationTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''

class Trade {
    Long id
    Long version
    Set segments
    static hasMany = [segments:Segment]

}

class Segment{
    Long id
    Long version
    Set products
    static hasMany = [products:Product]
}

class Product implements Serializable{
    Long id
    Long version

    Country country
    Segment segment

    static mapping = {
        id composite:['country','segment']
    }

}
class Country implements Serializable{
    Long id
    Long version
    String name
}
''')
    }


    void testCascadeToCompositeIdEntity() {
        def tradeClass = ga.getDomainClass("Trade").clazz
        def segmentClass = ga.getDomainClass("Segment").clazz
        def productClass = ga.getDomainClass("Product").clazz
        def countryClass = ga.getDomainClass("Country").clazz

        def trade = tradeClass.newInstance()
        def segment = segmentClass.newInstance()

        def product = productClass.newInstance()
        def country = countryClass.newInstance(name:"UK")
        assert country.save(flush:true)
        product.country = country
        segment.addToProducts(product)
        trade.addToSegments(segment)

        assert trade.save(flush:true)

        session.clear()

        assertEquals 1, tradeClass.count()
        assertEquals 1, segmentClass.count()
        assertEquals 1, productClass.count()
        assertEquals 1, countryClass.count()

    }
}