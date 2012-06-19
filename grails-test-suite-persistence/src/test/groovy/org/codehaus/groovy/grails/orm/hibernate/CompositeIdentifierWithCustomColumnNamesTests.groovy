package org.codehaus.groovy.grails.orm.hibernate

class CompositeIdentifierWithCustomColumnNamesTests extends AbstractGrailsHibernateTests {

    @Override
    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class IntervalType {
    String name
}

@Entity
class IntervalQuantity implements Serializable {

    IntervalType intervalType
    int quantity
    String description

    static mapping = {
        table   'mail_interval_quantity'
        version false
        id composite:[ 'intervalType', 'quantity' ]
        columns {
            intervalType column:'message_code'
            quantity     column:'recipient_code'
            description  column:'type'
        }
    }
}

@Entity
class DeliveryInstruction {

    String description
    IntervalType intervalType
    IntervalQuantity intervalQuantity

    static mapping = {
        table   'mail_delivery_instruction'
        version false
        id      generator:'sequence', params:[sequence:'DELIVERY_INSTR_SEQ']
        columns {
            id               column:'code'
            intervalType     column:'interval_type_code'
            intervalQuantity {
                column name:'interval_type'
                column name:'interval_quantity'
            }
            description      column:'description'
        }
    }
}
''')
    }


    void testCompositeIdentifierWithCustomColumnNames() {
        def DeliveryInstruction = ga.getDomainClass("DeliveryInstruction").clazz
        def IntervalQuantity = ga.getDomainClass("IntervalQuantity").clazz
        def IntervalType = ga.getDomainClass("IntervalType").clazz

        def it = IntervalType.newInstance(name:"Long").save()
        assert it != null

        def iq = IntervalQuantity.newInstance(intervalType:it, quantity:5, description:"Goods").save()
        assert iq != null

        def di = DeliveryInstruction.newInstance(intervalType:it, intervalQuantity:iq, description:"Back door").save(flush:true)
        assert di != null

        session.clear()

        di = DeliveryInstruction.list()[0]

        assert di != null
        assert di.description == "Back door"
        assert di.intervalType != null
        assert di.intervalType.name == "Long"
        assert di.intervalQuantity != null
        assert di.intervalQuantity.quantity == 5
        assert di.intervalQuantity.intervalType.name == "Long"

        def rs = session.connection().createStatement().executeQuery('select interval_type, interval_quantity from mail_delivery_instruction')
        assert rs.next()
    }
}
