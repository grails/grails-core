package org.grails.compiler.injection

import grails.artefact.Enhanced
import grails.persistence.PersistenceMethod
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import grails.compiler.ast.ClassInjector
import spock.lang.Specification

class GrailsArtefactTransformerSpec extends Specification {
    static testClass
    static gcl

    void setupSpec() {
        gcl = new GrailsAwareClassLoader()
        def transformer = new TestTransformer()
        gcl.classInjectors = [transformer]as ClassInjector[]
        System.setProperty("grails.version", "3.0.0")
    }

    void cleanupSpec() {
        System.setProperty("grails.version", "")
    }

    void "Test that a marker annotation can be added to weaved methods"() {
         given:"An enhanced class"
            def theClass = gcl.parseClass('''
                class AnnotatedClass{}
    ''')

        when:"The marker annotation on added methods retrieved"
            def theMethod = theClass.getDeclaredMethod("getSomePropertyDefinedInTestInstanceApi")
            final theAnnotation = theMethod.getAnnotation(PersistenceMethod)

        then:"The annotation is present"
            theAnnotation != null

    }

    void 'Test instance property is available in all classes in the hierarcy'() {
        given:
            def parentClass = gcl.parseClass('''
            class ParentClass {}
''')
            def firstChildClass = gcl.parseClass('''
            class FirstChildClass extends ParentClass {}
''')
            def secondChildClass = gcl.parseClass('''
            class SecondChildClass extends FirstChildClass {}
''')

        when:
            def parentInstanceFields = parentClass.declaredFields.findAll { 'instanceTestInstanceApi' == it.name && it.declaringClass == parentClass}
            def firstChildInstanceFields = firstChildClass.declaredFields.findAll { 'instanceTestInstanceApi' == it.name && it.declaringClass == firstChildClass}
            def secondChildInstanceFields = secondChildClass.declaredFields.findAll { 'instanceTestInstanceApi' == it.name && it.declaringClass == secondChildClass}

        then:
            1 == parentInstanceFields?.size()
            1 == firstChildInstanceFields?.size()
            1 == secondChildInstanceFields?.size()
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
            class TestClass2 {
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

    void 'Test version attribute on @Enhanced'() {
        given:
            def testClass = gcl.parseClass('''
            class TestClass3 {
                String firstName
            }
            ''')

        when:
           def enhancedAnnotation = testClass.getAnnotation(Enhanced)
           def version = enhancedAnnotation.version()

        then:
            version == "3.0.0"
    }

    void 'Test mixins attribute on @Enhanced'() {
        given:
        def testClass = gcl.parseClass('''
            @Mixin(Date)
            class TestClass4 {
                String firstName
            }
            ''')

        when:
            Enhanced enhancedAnnotation = testClass.getAnnotation(Enhanced)
            def version = enhancedAnnotation.version()

        then:
            version == "3.0.0"
            enhancedAnnotation.mixins() == [Date] as Class[]
    }
}

class TestTransformer extends AbstractGrailsArtefactTransformer {
    Class getInstanceImplementation() { TestInstanceApi }

    Class getStaticImplementation() {}

    boolean shouldInject(URL arg0) { true }

    protected boolean requiresAutowiring() { false }

    @Override
    protected String getArtefactType() {
        "*"
    }

    @Override
    protected AnnotationNode getMarkerAnnotation() {
        return new AnnotationNode(new ClassNode(PersistenceMethod).getPlainNodeReference())
    }
}

class TestInstanceApi {
    void setFirstName(Object instance, String firstName, String someArgWhichMakesThisNotAPropertySetterMethod) {}
    String getFirstName(Object instance, String someArgumentWhichMakesThisNotAPropertyGetter) {}
    void setSomePropertyDefinedInTestInstanceApi(Object instance, String arg) {}
    String getSomePropertyDefinedInTestInstanceApi(Object instance) { }
}
