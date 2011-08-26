package org.codehaus.groovy.grails.compiler.injection

import spock.lang.Specification

class GrailsArtefactTransformerSpec extends Specification {
    static testClass
    static gcl

    void setupSpec() {
        gcl = new GrailsAwareClassLoader()
        def transformer = new TestTransformer()
        gcl.classInjectors = [transformer]as ClassInjector[]
    }

    void 'Test properties defined in InstanceApi'() {
        given:
            def testClass = gcl.parseClass('''
            class TestClass {
            }
            ''')

        when:
            def getterMethods = testClass.metaClass.methods.findAll { 'getSomePropertyDefinedInTestInstanceApi' == it.name }
            def setterMethods = testClass.metaClass.methods.findAll { 'setSomePropertyDefinedInTestInstanceApi' == it.name }

        then:
            1 == getterMethods?.size()
            String == getterMethods[0].returnType
            1 == setterMethods?.size()
            1 == setterMethods[0].paramsCount
            String == setterMethods[0].parameterTypes[0].theClass
    }

    void 'Test properties defined in class take precedence over properties defined in InstanceApi'() {
        given:
            def testClass = gcl.parseClass('''
            class TestClass {
                List somePropertyDefinedInTestInstanceApi
            }
            ''')

        when:
           def getterMethods = testClass.metaClass.methods.findAll { 'getSomePropertyDefinedInTestInstanceApi' == it.name }
           def setterMethods = testClass.metaClass.methods.findAll { 'setSomePropertyDefinedInTestInstanceApi' == it.name }

        then:
            1 == getterMethods?.size()
            List == getterMethods[0].returnType
            1 == setterMethods?.size()
            1 == setterMethods[0].paramsCount
            List == setterMethods[0].parameterTypes[0].theClass
    }

    void 'Test that set* and get* methods which are not property accessors are added even if they overload actual accessors'() {
        given:
            def testClass = gcl.parseClass('''
            class TestClass {
                String firstName
            }
            ''')

        when:
           def oneArgSetters = testClass.metaClass.methods.findAll { 'setFirstName' == it.name && it.paramsCount == 1}
           def twoArgSetters = testClass.metaClass.methods.findAll { 'setFirstName' == it.name && it.paramsCount == 2}
           def noArgGetters = testClass.metaClass.methods.findAll { 'getFirstName' == it.name && it.paramsCount == 0 }
           def oneArgGetters = testClass.metaClass.methods.findAll { 'getFirstName' == it.name && it.paramsCount == 1 }

        then:
            1 == oneArgSetters?.size()
            String == oneArgSetters[0].parameterTypes[0].theClass

            1 == twoArgSetters?.size()
            String == twoArgSetters[0].parameterTypes[0].theClass
            String == twoArgSetters[0].parameterTypes[1].theClass

            1 == noArgGetters?.size()
            String == noArgGetters[0].returnType

            1 == oneArgGetters?.size()
            String == oneArgGetters[0].returnType
            String == oneArgGetters[0].parameterTypes[0].theClass
    }
}

class TestTransformer extends AbstractGrailsArtefactTransformer {
    public Class getInstanceImplementation() {
        TestInstanceApi
    }

    public Class getStaticImplementation() {
    }

    boolean shouldInject(URL arg0) {
        true
	}
}

class TestInstanceApi {
    void setFirstName(Object instance, String firstName, String someArgWhichMakesThisNotAPropertySetterMethod) {}
    String getFirstName(Object instance, String someArgumentWhichMakesThisNotAPropertyGetter) {}
    void setSomePropertyDefinedInTestInstanceApi(Object instance, String arg) {}
    String getSomePropertyDefinedInTestInstanceApi(Object instance) { }
}

