package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 13, 2008
 */
class EventsTriggeringTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''

class EventsTriggering {

    Long id
    Long version

    String name

    def eventData = [:]
    static optionals = ['eventData']


    def beforeInsert = {
        eventData['beforeInsert'] = true
    }

    def afterInsert = {
        eventData['afterInsert'] = true
    }

    def beforeUpdate = {
        eventData['beforeUpdate'] = true
    }

    def afterUpdate = {
        eventData['afterUpdate'] = true
    }




    def beforeLoad = {
        eventData['beforeLoad'] = true
    }

    def afterLoad = {
        eventData['afterLoad'] = true
    }

    def beforeDelete = {
        eventData['beforeDelete'] = true
    }

    def afterDelete = {
        eventData['afterDelete'] = true
    }

}
''')
    }



    void testEvents() {
        def testClass = ga.getDomainClass("EventsTriggering").clazz


        def test = testClass.newInstance(name:"Foo")

        def testData = test.eventData


        assert !testData.beforeInsert
        assert !testData.afterInsert
        assert !testData.afterUpdate
        assert !testData.beforeUpdate
        assert !testData.beforeDelete
        assert !testData.afterDelete
        assert !testData.beforeLoad
        assert !testData.afterLoad


        assert test.save(flush:true)

        assert session.contains(test)
        assert testData.beforeInsert
        assert testData.afterInsert
        assert !testData.afterUpdate
        assert !testData.beforeUpdate
        assert !testData.beforeDelete
        assert !testData.afterDelete
        assert !testData.beforeLoad
        assert !testData.afterLoad

        test.name = "Bar"
        assert test.save(flush:true)

        assert testData.beforeInsert
        assert testData.afterInsert
        assert testData.afterUpdate
        assert testData.beforeUpdate
        assert !testData.beforeDelete
        assert !testData.afterDelete
        assert !testData.beforeLoad
        assert !testData.afterLoad


        session.clear()

        test = testClass.get(1)
        testData = test.eventData

        assert !testData.beforeInsert
        assert !testData.afterInsert
        assert !testData.afterUpdate
        assert !testData.beforeUpdate
        assert !testData.beforeDelete
        assert !testData.afterDelete
        assert testData.beforeLoad
        assert testData.afterLoad

        test.delete(flush:true)

        assert !testData.beforeInsert
        assert !testData.afterInsert
        assert !testData.afterUpdate
        assert !testData.beforeUpdate
        assert testData.beforeDelete
        assert testData.afterDelete
        assert testData.beforeLoad
        assert testData.afterLoad        
    }
}