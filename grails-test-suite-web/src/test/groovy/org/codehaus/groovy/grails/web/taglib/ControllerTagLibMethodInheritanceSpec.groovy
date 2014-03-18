package org.codehaus.groovy.grails.web.taglib

import grails.artefact.Artefact
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Issue
import spock.lang.Specification

@TestFor(FirstTagLib)
@Mock([SecondTagLib])
class ControllerTagLibMethodInheritanceSpec extends Specification {

    @Issue('GRAILS-10031')
    void 'Test calling an inherited tag which invokes a method overridden in the subclass'() {
        when:
        def content = applyTemplate('${second.myTag()}')

        then:
        content == 'SecondTagLib.doSomething()'
    }
}


@Artefact("TagLibrary")
class FirstTagLib {

    static namespace = 'first'
    
    protected String doSomething() {
        'FirstTagLib.doSomething()'
    }

    def myTag = { attrs ->
        out << doSomething()
    }
}

@Artefact("TagLibrary")
class SecondTagLib extends FirstTagLib {
    static namespace = 'second'
    
    protected String doSomething() {
        'SecondTagLib.doSomething()'
    }
}
