package org.grails.databinding

import grails.databinding.initializers.ValueInitializer
import groovy.transform.CompileStatic

@CompileStatic
class ClosureValueInitializer implements ValueInitializer {

    Closure initializerClosure
    Class targetType
    

    @Override
    Object initialize() {
        initializerClosure.call()
    }
}