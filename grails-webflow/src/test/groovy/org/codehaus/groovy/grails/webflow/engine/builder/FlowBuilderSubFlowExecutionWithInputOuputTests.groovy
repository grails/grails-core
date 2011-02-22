package org.codehaus.groovy.grails.webflow.engine.builder;


import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests
import org.springframework.webflow.engine.FlowInputMappingException

class FlowBuilderSubFlowExecutionWithInputOuputTests extends AbstractGrailsTagAwareFlowExecutionTests {

    def searchMoreAction = { [moreResults: ["one", "two", "three"]] }
    def subber = {
        input {
            shortHand()
            constantDefault1('constantDefault1Value')
            constantDefault2(required: false, value: 'constantDefault2Value')
            dynamicDefault1 {conversation.get('conversationAttribute1')}
            dynamicDefault2(value: {conversation.get('conversationAttribute2')})
        }

        subberStart {
            on("next").to "subberEnd"
        }

        subberEnd {
            output {
                constantOut1('constantOut1Value')
                constantOut2(value: 'constantOut2Value')
                dynamicOut1 {conversation.get('conversationAttribute1')}
                dynamicOut2(value: {conversation.get('conversationAttribute2')})
            }
        }
    }

    def requiredInputSubber = {
        input {
            requiredInput(required: true)
        }

        subberStart {
            on("next").to "end"
        }

        end()
    }


    Closure getFlowClosure() {
        return {
            start {
                on("next") {
                    conversation.put('conversationAttribute1', 'conversationAttribute1Value')
                    conversation.put('conversationAttribute2', 'conversationAttribute2Value')
                    flow.put('dynamicDefaultIn', 'dynamicDefaultInValue')
                }.to('defaultInputs')
            }

            defaultInputs {
                subflow(controller: 'subber', action: 'subber')
                on('subberEnd').to('providedInputs')
            }

            providedInputs {
                subflow(controller: 'subber', action: 'subber',
                        input: [
                                shortHand: 'shortHandInValue',
                                constantDefault1: 'constantDefault1InValue',
                                constantDefault2: 'constantDefault2InValue',
                                dynamicDefault1: {flow.get('dynamicDefaultIn')},
                        ])
                on('subberEnd') {
                    flow.put('constantOut1', currentEvent.attributes.constantOut1)
                    flow.put('constantOut2', currentEvent.attributes.constantOut2)
                    flow.put('dynamicOut1', currentEvent.attributes.dynamicOut1)
                    flow.put('dynamicOut2', currentEvent.attributes.dynamicOut2)
                }.to('check')
            }

            check {
                on('next').to('end')
            }

            end()
        }
    }


    void testSubFlowInputOutput() {
        registerFlow('subber/subber', subber)
        GrailsWebRequest webrequest = grails.util.GrailsWebUtil.bindMockWebRequest()
        startFlow()

        signalEvent('next')
        assertCurrentStateEquals 'subberStart'
        assert flowScope.shortHand == null
        assert flowScope.constantDefault1 == 'constantDefault1Value'
        assert flowScope.constantDefault2 == 'constantDefault2Value'
        assert flowScope.dynamicDefault1 == 'conversationAttribute1Value'
        assert flowScope.dynamicDefault2 == 'conversationAttribute2Value'

        signalEvent('next')
        assertCurrentStateEquals 'subberStart'
        assert flowScope.shortHand == 'shortHandInValue'
        assert flowScope.constantDefault1 == 'constantDefault1InValue'
        assert flowScope.constantDefault2 == 'constantDefault2InValue'
        assert flowScope.dynamicDefault1 == 'dynamicDefaultInValue'

        signalEvent('next')
        assertCurrentStateEquals 'check'
        assert flowScope.constantOut1 == 'constantOut1Value'
        assert flowScope.constantOut2 == 'constantOut2Value'
        assert flowScope.dynamicOut1 == 'conversationAttribute1Value'
        assert flowScope.dynamicOut2 == 'conversationAttribute2Value'

    }

    void testFailOnRequiredInput() {
        registerFlow('subber/subber', requiredInputSubber)
        grails.util.GrailsWebUtil.bindMockWebRequest()
        startFlow()
        try {
            signalEvent('next')
            fail('expected FlowInputMappingException')
        }
        catch (FlowInputMappingException e) {}
    }
}