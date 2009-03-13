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

class CascadeToCompositeIdAssociationTrade {
    Long id
    Long version
    Set segments
    static hasMany = [segments:CascadeToCompositeIdAssociationSegment]

}

class CascadeToCompositeIdAssociationSegment{
    Long id
    Long version
    Set products
    static hasMany = [products:CascadeToCompositeIdAssociationProduct]
}

class CascadeToCompositeIdAssociationProduct implements Serializable{
    Long id
    Long version

    CascadeToCompositeIdAssociationCountry country
    CascadeToCompositeIdAssociationSegment segment

    static mapping = {
        id composite:['country','segment']
    }

}
class CascadeToCompositeIdAssociationCountry implements Serializable{
    Long id
    Long version
    String name
}
''')
    }


    void testCascadeToCompositeIdEntity() {
        def tradeClass = ga.getDomainClass("CascadeToCompositeIdAssociationTrade").clazz
        def segmentClass = ga.getDomainClass("CascadeToCompositeIdAssociationSegment").clazz
        def productClass = ga.getDomainClass("CascadeToCompositeIdAssociationProduct").clazz
        def countryClass = ga.getDomainClass("CascadeToCompositeIdAssociationCountry").clazz

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