package org.grails.compiler.injection

import grails.compiler.traits.TraitInjector
import spock.lang.Specification

class ParameterizedTraitSpec extends Specification {

    void 'test that generic parameters are populated as expected'() {
        setup:
        def gcl = new GrailsAwareClassLoader()
        def traitInjector = new SampleTraitInjector()
        gcl.traitInjectors = [traitInjector] as TraitInjector[]
        
        when: 'a class is loaded with a trait injector which injects a parameteried trait'
        def widgetClass = gcl.parseClass '''
class Widget {}
'''
        
        then: 'the trait is applied to the class'
        SampleTrait.isAssignableFrom widgetClass
        
        and: 'parameters are replaced with the type which the trait is being applied to'
        widgetClass.methods.find { method ->
            method.name == 'getValue' && method.returnType == widgetClass
        }
    }
}

class SampleTraitInjector implements TraitInjector {

    @Override
    Class getTrait() {
        SampleTrait
    }

    @Override
    boolean shouldInject(URL url) {
        true
    }

    @Override
    String[] getArtefactTypes() {
        []
    }
}

trait SampleTrait<D> {
    D getValue() {}
}
