package org.codehaus.groovy.grails.orm

import org.codehaus.groovy.grails.web.binding.GrailsWebDataBinder
import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.databinding.converters.AbstractStructuredBindingEditor

import spock.lang.Specification

class GrailsWebDataBindingStructuredEditorSpec extends Specification {
    
    void 'Test structured editor'() {
        given: 'A binder with a structured editor registered'
        def binder = new GrailsWebDataBinder()
        binder.setStructuredBindingEditors(new StucturedEmployeeEditor())
        def company = new Company()
        
        when: 'binding happens'
        binder.bind company, [president: 'struct', president_firstName: 'Tim', president_lastName: 'Cook',
                              vicePresident: 'struct', vicePresident_firstName: 'Eddy', vicePresident_lastName: 'Cue'] as SimpleMapDataBindingSource
                          
        then: 'the expected values were bound'
        'Tim' == company.president.firstName
        'Cook' == company.president.lastName
        'Eddy' == company.vicePresident.firstName
        'Cue' == company.vicePresident.lastName                    
    }
}

class Company {
    Employee president
    Employee vicePresident
}

class Employee {
    String firstName
    String lastName
}

class StucturedEmployeeEditor extends AbstractStructuredBindingEditor<Employee> {

    @Override
    public Employee getPropertyValue(Object obj, String propertyName, DataBindingSource bindingSource) {
        def employee = new Employee()
        employee.firstName = bindingSource.getPropertyValue(propertyName + '_firstName')
        employee.lastName = bindingSource.getPropertyValue(propertyName + '_lastName')
        employee
    }

    @Override
    Class<? extends Employee> getTargetType() {
        Employee
    }
    
    
}