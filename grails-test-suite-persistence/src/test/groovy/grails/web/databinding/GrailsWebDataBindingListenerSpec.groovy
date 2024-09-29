package grails.web.databinding

import grails.databinding.SimpleMapDataBindingSource
import grails.databinding.events.DataBindingListenerAdapter
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import spock.lang.Ignore
import spock.lang.Specification

class GrailsWebDataBindingListenerSpec extends Specification implements GrailsUnitTest {

    DataBindingListenerAdapter dataBindingListenerAdapter = Mock()

    Closure doWithSpring() { { ->
            testWidgetDataBindingListener(InstanceFactoryBean, dataBindingListenerAdapter, DataBindingListenerAdapter)
        }
    }

    @Ignore("Too few invocations")
    void "test that DataBindingListener is added to GrailsWebDataBinder"() {

        given:
        GrailsWebDataBinder binder = grailsApplication.mainContext.getBean(DataBindingUtils.DATA_BINDER_BEAN_NAME)
        TestWidget testWidget = new TestWidget()

        when:
        binder.bind(testWidget, ["name": "Clock"] as SimpleMapDataBindingSource)

        then:
        1 * dataBindingListenerAdapter.supports(TestWidget) >> true
        1 * dataBindingListenerAdapter.beforeBinding(testWidget, _)
        1 * dataBindingListenerAdapter.afterBinding(testWidget, _)
    }

    static class TestWidget {
        String name
    }
}

