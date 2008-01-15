/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Dec 4, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class BidirectionalMapOneToManyTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass '''
class StockLocation
{

   Long id
   Long version
  Map stockpiles

  static hasMany = [stockpiles:Stockpile]

}

class Stockpile
{
   Long id
   Long version
  String product
  Float quantity
  StockLocation stockLocation

    static constraints = {
        stockLocation(nullable:true)
    }
}
'''
    }


    void testModel() {
        def locClass = ga.getDomainClass("StockLocation")
        def spClass = ga.getDomainClass("Stockpile")

        assert locClass.getPropertyByName("stockpiles").association
        assert locClass.getPropertyByName("stockpiles").bidirectional
        assert locClass.getPropertyByName("stockpiles").oneToMany


        assert spClass.getPropertyByName("stockLocation").association
        assert spClass.getPropertyByName("stockLocation").bidirectional
        assert spClass.getPropertyByName("stockLocation").manyToOne
    }


    void testUpdateBidiMap() {
        def locClass = ga.getDomainClass("StockLocation").clazz
        def spClass = ga.getDomainClass("Stockpile").clazz

        def sl = locClass.newInstance()

         sl.stockpiles = [one: spClass.newInstance(product:"MacBook", quantity:1.1 as Float)]

         assert sl.save(flush:true)

         session.clear()

         sl = locClass.get(1)

         assert sl

         assertEquals 1, sl.stockpiles.size()

         sl.stockpiles.two = spClass.newInstance(product:"MacBook Pro", quantity:2.3 as Float)

         sl.save(flush:true)

         session.clear()

         sl = locClass.get(1)

         assert sl

         assertEquals 2, sl.stockpiles.size()
    }
}