package grails.test.mixin

import grails.databinding.SimpleMapDataBindingSource;
import grails.databinding.converters.ValueConverter;
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.runtime.FreshRuntime

import org.codehaus.groovy.grails.commons.InstanceFactoryBean
import org.codehaus.groovy.grails.plugins.databinding.DataBindingGrailsPlugin
import org.junit.ClassRule

import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Lari Hotari
 */
@FreshRuntime
@TestMixin(GrailsUnitTestMixin)
class SpyBeanSpec extends Specification {
    def myAddressValueConverter=Spy(MyAddressValueConverter)
    def doWithSpring = {
        def otherClosure=new DataBindingGrailsPlugin().doWithSpring.clone()
        otherClosure.delegate=delegate
        otherClosure.call()
        myAddressValueConverter(InstanceFactoryBean, myAddressValueConverter, MyAddressValueConverter)
    }
    
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