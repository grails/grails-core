package org.codehaus.groovy.grails.commons

class DefaultGrailsTagLibClassTests extends GroovyTestCase {

    void testNotAllPropertiesAreTreatedAsTags() {
        def tagLibClass = new DefaultGrailsTagLibClass(SomeTagLib)
        def tagNames = tagLibClass.tagNames
        assertEquals 2, tagNames.size()
        assertTrue 'tagOne' in tagNames
        assertTrue 'tagTwo' in tagNames
    }
}


class SomeTagLib {
    def someProperty
    def someOtherProperty
    def tagOne = { args ->}
    def tagTwo = { args ->}
}