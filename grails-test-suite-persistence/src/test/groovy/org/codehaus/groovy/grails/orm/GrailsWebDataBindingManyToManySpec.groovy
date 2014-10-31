package org.codehaus.groovy.grails.orm

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin

import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.codehaus.groovy.grails.web.binding.GrailsWebDataBinder
import org.grails.databinding.SimpleMapDataBindingSource

import spock.lang.Issue
import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
@Mock([OwningEnd, ChildEnd])
class GrailsWebDataBindingManyToManySpec extends Specification {

    GrailsWebDataBinder binder

    void setup() {
        binder = grailsApplication.mainContext.getBean(DataBindingUtils.DATA_BINDER_BEAN_NAME)
    }

    @Issue('GRAILS-11638')
    void 'test binding to the owning end'() {
        given:
        def owner = new OwningEnd()
        
        when:
        binder.bind owner,  ['children[0]': [name: 'Child One'], 'children[1]': [name: 'Child Two']] as SimpleMapDataBindingSource
        
        then:
        owner.children.size() == 2
        owner.children[0].owners.size() == 1
        owner.children[0].owners[0].is owner
        owner.children[1].owners.size() == 1
        owner.children[1].owners[0].is owner
    }
    
    @Issue('GRAILS-11638')
    void 'test binding to the child end'() {
        given:
        def child = new ChildEnd()
        
        when:
        binder.bind child,  ['owners[0]': [name: 'Owner One'], 'owners[1]': [name: 'Owner Two']] as SimpleMapDataBindingSource
        
        then:
        child.owners.size() == 2
        child.owners[0].children.size() == 1
        child.owners[0].children[0].is child
        child.owners[1].children.size() == 1
        child.owners[1].children[0].is child
    }
}

@Entity
class OwningEnd {
    String name
    static hasMany = [children: ChildEnd]
}

@Entity
class ChildEnd {
    String name
    static hasMany = [owners: OwningEnd]
    static belongsTo = OwningEnd
}
