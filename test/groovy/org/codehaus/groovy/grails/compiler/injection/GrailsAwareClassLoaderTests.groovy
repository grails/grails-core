package org.codehaus.groovy.grails.compiler.injection
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 2, 2008
 */
class GrailsAwareClassLoaderTests extends GroovyTestCase{

    GroovyClassLoader gcl

    protected void setUp() {
        gcl = new GrailsAwareClassLoader()
        gcl.classInjectors = [new MockGrailsDomainInjector()] as ClassInjector[]
    }


    void testInjectIds() {
        def clz = gcl.parseClass('''
class IdTest {}
''')

        def obj = clz.newInstance()
        obj.id = 10
        obj.version = 2

        assertEquals 10, obj.id
        assertEquals 2, obj.version
    }

    void testInjectHasManyAsssociation() {
        def clz = gcl.parseClass('''
class IdTest {
    static hasMany = [others:AssocTest]
}
class AssocTest {}
''')

        def obj = clz.newInstance()
        obj.id = 10
        obj.version = 2
        obj.others = [] as Set
        assertEquals 10, obj.id
        assertEquals 2, obj.version
        assertTrue obj.others instanceof Set

    }

    void testInjectBelongsToAssociation()  {
        def clz1 = gcl.parseClass('''
class AssocTest {}
''')
        def clz = gcl.parseClass('''
class IdTest {
    static belongsTo = [other:AssocTest]
}

''')

        def obj = clz.newInstance()
        obj.id = 10
        obj.version = 2
        obj.other = clz1.newInstance()


        assertEquals 10, obj.id
        assertEquals 2, obj.version
        assertTrue clz1.isInstance(obj.other)        
    }


    /* TODO: Failing due to a bug in Groovy ClassNode API, need new Groovy version to fix GRAILS-2449
    void testSubclassProvidedIdWithDifferentType() {
        gcl.parseClass('''
class TheClass {
    String id
    String name
}
class TheSubClass extends TheClass {
    String secondName
}
''')

        def clz = gcl.loadClass("TheClass")
        def subClz = gcl.loadClass("TheSubClass")
        
        def obj = clz.newInstance()
        obj.id = "foo"
        obj.name = "bar"

        assertEquals "foo", obj.id
        assertEquals "bar", obj.name

        def sub  = subClz.newInstance()

        sub.id = "foo"
        sub.name = "bar"
        sub.secondName = "stuff"

        assertEquals "foo", sub.id
        assertEquals "bar", sub.name
        assertEquals "stuff", sub.secondName

    }    */
}

class MockGrailsDomainInjector extends DefaultGrailsDomainClassInjector {
    public boolean shouldInject(URL url) {  true  }

}