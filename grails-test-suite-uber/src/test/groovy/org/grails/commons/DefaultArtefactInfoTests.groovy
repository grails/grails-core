package org.grails.commons

import grails.core.DefaultArtefactInfo
import org.grails.core.DefaultGrailsControllerClass
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class DefaultArtefactInfoTests {

    @Test
    void testAddGrailsClass() {
        def info = new DefaultArtefactInfo()

        def gcl = new GroovyClassLoader()

        info.updateComplete()

        assertEquals 0, info.classes.size()
        assertEquals 0, info.classesByName.size()
        assertEquals 0, info.grailsClasses.size()
        assertEquals 0, info.grailsClassesByName.size()
        assertEquals 0, info.grailsClassesArray.size()

        def c1 = gcl.parseClass('''
class FooController {}
''')
        def controllerClass1 = new DefaultGrailsControllerClass(c1)
        info.addGrailsClass(controllerClass1)

        info.updateComplete()

        assertEquals 1, info.classes.size()
        assertEquals 1, info.classesByName.size()
        assertEquals 1, info.grailsClasses.size()
        assertEquals 1, info.grailsClassesByName.size()
        assertEquals 1, info.grailsClassesArray.size()

        def c2 = gcl.parseClass('''
class BarController {}
''')
        def controllerClass2 = new DefaultGrailsControllerClass(c2)
        info.addGrailsClass(controllerClass2)

        info.addGrailsClass(controllerClass2)

        info.updateComplete()

        assertEquals 2, info.classes.size()
        assertEquals 2, info.classesByName.size()
        assertEquals 2, info.grailsClasses.size()
        assertEquals 2, info.grailsClassesByName.size()
        assertEquals 2, info.grailsClassesArray.size()

        // test class of same name
        def c3 = gcl.parseClass('''
class FooController {}
''')
        def controllerClass3 = new DefaultGrailsControllerClass(c3)
        info.addGrailsClass(controllerClass3)

        info.updateComplete()

        assertEquals 2, info.classes.size()
        assertEquals 2, info.classesByName.size()
        assertEquals 2, info.grailsClasses.size()
        assertEquals 2, info.grailsClassesByName.size()
        assertEquals 2, info.grailsClassesArray.size()
    }
}
