package grails.util 

import spock.lang.Specification

class GrailsMetaClassUtilsSpec extends Specification {
    static {
        ExpandoMetaClass.enableGlobally()
    }
        
    def "delegating metaclass shouldn't be replaced"() {
        given:
        def obj = new MySampleClass(name: 'John Doe')
        def dmc = new DelegatingMetaClass(MySampleClass)
        obj.metaClass = dmc
        when:
        MetaClass mc = GrailsMetaClassUtils.getMetaClass(obj)
        then:
        mc instanceof ExpandoMetaClass
        obj.getMetaClass().getAdaptee() == dmc 
    }

}

class MySampleClass {
    String name    
}
