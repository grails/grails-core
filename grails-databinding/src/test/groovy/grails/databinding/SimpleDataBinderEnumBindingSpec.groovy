package grails.databinding

import grails.databinding.SimpleDataBinder;
import grails.databinding.SimpleMapDataBindingSource;
import spock.lang.Issue
import spock.lang.Specification

class SimpleDataBinderEnumBindingSpec extends Specification {
    @Issue('GRAILS-10979')
    void 'Test binding to a List of enum'() {
        given:
        def binder = new SimpleDataBinder()
        def holder = new HatSizeHolder()
        
        when:
        binder.bind holder, ['sizes[0]': 'LARGE', 'sizes[1]': 'SMALL'] as SimpleMapDataBindingSource
        
        then:
        holder.sizes?.size() == 2
        holder.sizes[0] == HatSize.LARGE
        holder.sizes[1] == HatSize.SMALL
    }

}

enum HatSize {
    SMALL, MEDIUM, LARGE
}

class HatSizeHolder {
    List<HatSize> sizes
}
