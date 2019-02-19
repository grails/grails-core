package grails.test.mixin

import grails.databinding.SimpleMapDataBindingSource;
import grails.databinding.converters.ValueConverter
import org.grails.plugins.databinding.DataBindingGrailsPlugin;
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.plugins.databinding.AbstractDataBindingGrailsPlugin
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

/**
 * @author Lari Hotari
 */
class SpyBeanSpec extends Specification implements GrailsUnitTest {

    def myAddressValueConverter=Spy(MyAddressValueConverter)

    Closure doWithSpring() {{ ->
        def plugin = new DataBindingGrailsPlugin()
        plugin.grailsApplication = delegate.application
        def otherClosure= plugin.doWithSpring().clone()
        otherClosure.delegate=delegate
        otherClosure.call()
        myAddressValueConverter(InstanceFactoryBean, myAddressValueConverter, MyAddressValueConverter)
    }}

    def "it's possible to use Spy instances as beans as well"() {
        given:
        def binder=grailsApplication.mainContext.getBean("grailsWebDataBinder")
        def person=new MyPerson()
        when:
        binder.bind person, [name:'Lari', address:'Espoo,Finland'] as SimpleMapDataBindingSource
        then:
        1 * myAddressValueConverter.canConvert('Espoo,Finland')
        1 * myAddressValueConverter.convert('Espoo,Finland')
        0 * myAddressValueConverter._
        person.address.city=='Espoo'
        person.address.country=='Finland'
    }
}

class MyPerson {
    String name
    MyAddress address
}

class MyAddress {
    String city
    String country
}

class MyAddressValueConverter implements ValueConverter {
    boolean canConvert(value) {
        value instanceof String
    }

    def convert(value) {
        def pieces = value.split(',')
        new MyAddress(city: pieces[0], country: pieces[1])
    }

    Class<?> getTargetType() {
        MyAddress
    }
}