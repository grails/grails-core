package org.codehaus.groovy.grails.plugins;

import org.codehaus.groovy.grails.plugins.web.AbstractGrailsPluginTests
import org.codehaus.groovy.grails.compiler.injection.DefaultGrailsDomainClassInjector
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader


class DomainClassGrailsPluginTests extends AbstractGrailsPluginTests {
    void onSetUp() {
        gcl = new GrailsAwareClassLoader(gcl)
        def injector = new AlwaysInjector()
        gcl.setClassInjectors([injector] as ClassInjector[]);

        def fs = File.separator
        gcl.parseClass(
                """class Test{ }""", "myapp${fs}grails-app${fs}domain${fs}Test.groovy")

        gcl.parseClass("""abstract class Parent {
  String name
} """, "myapp${fs}grails-app${fs}domai${fs}Parent.groovy")

        gcl.parseClass("""class Child extends Parent {
   Date someDate
} """, "myapp${fs}grails-app${fs}domain${fs}Child.groovy")

        gcl.parseClass("""class Parent2 {
   Date someDate
    String toString(){
        return 'my toString'
    }
} """, "myapp${fs}grails-app${fs}domain${fs}Parent2.groovy")

        gcl.parseClass("""class Child2 extends Parent2 {
   String someField
  String toString(){
        return 'my other toString'
    }

} """, "myapp${fs}grails-app${fs}domain${fs}Child2.groovy")

        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
    }

    void testDomainClassesPlugin() {
        assert appCtx.containsBean("TestDomainClass")
        def instance = appCtx.getBean("TestDomainClass").newInstance()
        assertNull instance.id
        assertNull instance.version
    }

    void testToString() {
        def instance = appCtx.getBean("ChildDomainClass").newInstance()
        instance.id = 1
        assertEquals('Child : 1', instance.toString())

        instance = appCtx.getBean("TestDomainClass").newInstance()
        instance.id = 2
        assertEquals('Test : 2', instance.toString())

        instance = appCtx.getBean("Parent2DomainClass").newInstance()
        instance.id = 3
        assertEquals('my toString', instance.toString())

        instance = appCtx.getBean("Child2DomainClass").newInstance()
        instance.id = 4
        assertEquals('my other toString', instance.toString())
    }
}

class AlwaysInjector extends DefaultGrailsDomainClassInjector {

    public boolean shouldInject(URL url) {
        return true
    }


}