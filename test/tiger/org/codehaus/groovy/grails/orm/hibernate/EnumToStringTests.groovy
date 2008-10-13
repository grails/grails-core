package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.codehaus.groovy.grails.plugins.web.AbstractGrailsPluginTests
import org.codehaus.groovy.grails.plugins.AlwaysInjector

class EnumToStringTests extends AbstractGrailsPluginTests {
    void onSetUp() {
        gcl = new GrailsAwareClassLoader(gcl)
        def injector = new AlwaysInjector()
        gcl.setClassInjectors([injector] as ClassInjector[]);
        gcl.parseClass('''enum State {
    ONE,TWO, THRE
}    ''')
    }

    void testEnumToStringIsNotOverridden() {
        def clazz = ga.classLoader.loadClass("State")
        assertEquals('TWO', clazz.TWO.toString())
    }

}

