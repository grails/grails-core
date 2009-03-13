package org.codehaus.groovy.grails.orm.hibernate
/**
 *
 * test for GRAILS-2734
 *
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Apr 9, 2008
 */
class BidirectionalOneToManyWithInheritanceTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class ConfigurationItem
{
    Long id
    Long version
    ConfigurationItem parent

    Set configurationItems

    static hasMany = [configurationItems: ConfigurationItem]
    static mappedBy = [configurationItems: 'parent']
    static belongsTo = [ConfigurationItem]
    static constraints = {
        parent(nullable: true)
    }

    static mapping = {
        table 'configuration_item'
        columns {
           parent lazy: false, column: 'bom'
        }
    }
}

class Documentation extends ConfigurationItem{
    Long id
    Long version

}
class ChangeRequest extends ConfigurationItem{
    Long id
    Long version

}

''')
    }

    void testBidirectionalOneToManyWithInheritance() {

        def configItemClass = ga.getDomainClass("ConfigurationItem").clazz
        def docClass = ga.getDomainClass("Documentation").clazz
        def changeRequestClass = ga.getDomainClass("ChangeRequest").clazz

        def doc = docClass.newInstance()


        assert doc.addToConfigurationItems(changeRequestClass.newInstance())
                    .addToConfigurationItems(docClass.newInstance())
                    .save(flush:true)

        session.clear()

        doc = docClass.get(1)

        assertEquals 2,doc.configurationItems.size()
        
    }

}