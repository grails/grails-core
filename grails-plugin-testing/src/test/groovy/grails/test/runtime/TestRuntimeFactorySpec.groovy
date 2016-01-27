package grails.test.runtime

import grails.test.mixin.UseTestPlugin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.support.MixinInstance
import grails.test.mixin.support.TestMixinRuntimeSupport
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import grails.test.mixin.webflow.WebFlowUnitTestMixin
import spock.lang.Specification
import spock.lang.Unroll

class TestRuntimeFactorySpec extends Specification {
    @Unroll
    def "should instantiate correct plugins and features for #testClass.simpleName"() {
        when:
            def testRuntime = TestRuntimeFactory.getRuntimeForTestClass(testClass)
        then:
            testRuntime.features == features as Set
            testRuntime.plugins.collect {
                it.class.simpleName
            } as Set  == plugins as Set
        where:
            testClass | features | plugins
            SampleGrailsTestClass | ['coreBeans','grailsApplication'] | ['CoreBeansTestPlugin','GrailsApplicationTestPlugin','MetaClassCleanerTestPlugin']
            SampleDomainTestClass | ['coreBeans','domainClass','grailsApplication'] | ['CoreBeansTestPlugin','DomainClassTestPlugin','GrailsApplicationTestPlugin','MetaClassCleanerTestPlugin']
            SampleServiceTestClass | ['coreBeans','domainClass','grailsApplication'] | ['CoreBeansTestPlugin','DomainClassTestPlugin','GrailsApplicationTestPlugin','MetaClassCleanerTestPlugin']
            SampleControllerTestClass | ['coreBeans','controller','grailsApplication'] | ['CoreBeansTestPlugin','ControllerTestPlugin','GrailsApplicationTestPlugin','MetaClassCleanerTestPlugin']
            SampleGroovyPageTestClass | ['coreBeans','controller','groovyPage','grailsApplication'] | ['CoreBeansTestPlugin','ControllerTestPlugin','GrailsApplicationTestPlugin','MetaClassCleanerTestPlugin','GroovyPageTestPlugin']
            SampleWebflowTestClass | ['coreBeans','controller','webFlow','grailsApplication'] | ['CoreBeansTestPlugin','ControllerTestPlugin','GrailsApplicationTestPlugin','MetaClassCleanerTestPlugin','WebFlowTestPlugin']
            SampleDomainWithCustomPluginProvidingFeatureTestClass | ['coreBeans','domainClass','grailsApplication'] | ['CoreBeansTestPlugin','SampleDomainClassTestPlugin','GrailsApplicationTestPlugin','MetaClassCleanerTestPlugin']
            SampleDomainWithCustomPluginRegisteredByRegistrarTestClass | ['coreBeans','domainClass','grailsApplication'] | ['CoreBeansTestPlugin','SampleDomainClassTestPlugin','GrailsApplicationTestPlugin','MetaClassCleanerTestPlugin']
    }

    @Unroll
    def "should throw exception when feature is missing for #testClass.simpleName"() {
        when:
            def testRuntime = TestRuntimeFactory.getRuntimeForTestClass(testClass)
        then:
            TestRuntimeFactoryException e = thrown()
            e.message == "No plugin available for feature $missingFeature"
        where:
            testClass | missingFeature
            SampleTestWithMissingFeatureTestClass | 'myMissingFeature'
    }
}

// emulate test classes that have an instance of a test mixin target class
// the instance gets annotated with @MixinInstance by the TestMixinTransform

class SampleGrailsTestClass {
    @MixinInstance
    private static GrailsUnitTestMixin mixinInstance = new GrailsUnitTestMixin()
}
class SampleDomainTestClass {
    @MixinInstance
    private static DomainClassUnitTestMixin mixinInstance = new DomainClassUnitTestMixin()
}
class SampleServiceTestClass {
    @MixinInstance
    private static ServiceUnitTestMixin mixinInstance = new ServiceUnitTestMixin()
}
class SampleControllerTestClass {
    @MixinInstance
    private static ControllerUnitTestMixin mixinInstance = new ControllerUnitTestMixin()
}
class SampleGroovyPageTestClass {
    @MixinInstance
    private static GroovyPageUnitTestMixin mixinInstance = new GroovyPageUnitTestMixin()
}
class SampleWebflowTestClass {
    @MixinInstance
    private static WebFlowUnitTestMixin mixinInstance = new WebFlowUnitTestMixin()
}
@UseTestPlugin(SampleDomainClassTestPlugin)
class SampleDomainWithCustomPluginProvidingFeatureTestClass {
    @MixinInstance
    private static DomainClassUnitTestMixin domainClassMixinInstance = new DomainClassUnitTestMixin()
}
class SampleBaseTestPlugin implements TestPlugin {
    String[] requiredFeatures
    String[] providedFeatures
    int ordinal=1

    void onTestEvent(TestEvent event) {

    }

    void close(TestRuntime runtime) {

    }
}
class SampleDomainClassTestPlugin extends SampleBaseTestPlugin {
    SampleDomainClassTestPlugin() {
        requiredFeatures = ['grailsApplication']
        providedFeatures = ['domainClass']
    }
}
class SampleTestMixinWithRegistrar extends TestMixinRuntimeSupport implements TestPluginRegistrar {
    SampleTestMixinWithRegistrar() {
        super(['coreBeans','domainClass','grailsApplication'])
    }

    Iterable<TestPluginUsage> getTestPluginUsages() {
        [new TestPluginUsage(pluginClasses:[SampleDomainClassTestPlugin], requestActivation:true)]
    }
}
class SampleDomainWithCustomPluginRegisteredByRegistrarTestClass {
    @MixinInstance
    private static SampleTestMixinWithRegistrar mixinInstance = new SampleTestMixinWithRegistrar()
}
class SampleTestMixinWithMissingFeature extends TestMixinRuntimeSupport {
    SampleTestMixinWithMissingFeature() {
        super(['coreBeans','grailsApplication','myMissingFeature'])
    }
}
class SampleTestWithMissingFeatureTestClass {
    @MixinInstance
    private static SampleTestMixinWithMissingFeature mixinInstance = new SampleTestMixinWithMissingFeature()
}
