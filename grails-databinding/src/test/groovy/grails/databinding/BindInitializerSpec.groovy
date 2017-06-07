package grails.databinding

import spock.lang.Specification


class BindInitializerSpec extends Specification {
    
    void 'Test BindInitializer for specific property'() {
        given:
            def binder = new SimpleDataBinder()
            def obj = new ClassWithBindInitializerOnProperty()
        when:
            binder.bind(obj, new SimpleMapDataBindingSource(['association': [valueBound:'valueBound']]))

        then:
            obj.association.valueBound == 'valueBound'
            obj.association.valueInitialized == 'valueInitialized'
    }


    static class ReferencedClass{
        String valueInitialized
        String valueBound
    }
    class ClassWithBindInitializerOnProperty {
        @BindInitializer({
            obj -> 
                new ReferencedClass(valueInitialized:'valueInitialized')
        })
        ReferencedClass association
    }
}
