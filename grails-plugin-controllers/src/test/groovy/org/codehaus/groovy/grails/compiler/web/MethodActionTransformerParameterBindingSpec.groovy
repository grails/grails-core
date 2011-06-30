package org.codehaus.groovy.grails.compiler.web

import grails.util.BuildSettings
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification

class MethodActionTransformerParameterBindingSpec extends Specification {

    static controllerClass
    def controller

    void setupSpec() {
        def gcl = new GrailsAwareClassLoader()
        def transformer = new MethodActionTransformer() {
                @Override
                boolean shouldInject(URL url) {
                    return true;
                }

            }
         def transformer2 = new ControllerTransformer() {
                @Override
                boolean shouldInject(URL url) {
                    return true;
                }

            }
        gcl.classInjectors = [transformer,transformer2] as ClassInjector[]
        controllerClass = gcl.parseClass('''
        class TestBindingController {
            def methodAction(String stringParam,
                             Short shortParam,
                             short primitiveShortParam,
                             Integer integerParam,
                             int primitiveIntParam,
                             Long longParam,
                             long primitiveLongParam,
                             Float floatParam,
                             float primitiveFloatParam,
                             Double doubleParam,
                             double primitiveDoubleParam,
                             Boolean booleanParam,
                             boolean primitiveBooleanParam,
                             Byte byteParam,
                             byte primitiveByteParam) {
                [ stringParam: stringParam,
                  integerParam: integerParam,
                  primitiveIntParam: primitiveIntParam,
                  shortParam: shortParam,
                  primitiveShortParam: primitiveShortParam,
                  longParam: longParam,
                  primitiveLongParam: primitiveLongParam,
                  floatParam: floatParam,
                  primitiveFloatParam: primitiveFloatParam,
                  doubleParam: doubleParam,
                  primitiveDoubleParam: primitiveDoubleParam,
                  booleanParam: booleanParam,
                  primitiveBooleanParam: primitiveBooleanParam,
                  byteParam: byteParam,
                  primitiveByteParam: primitiveByteParam ]
            }
            def methodActionWithDefaultValues(String stringParam = 'defaultString',
                             Short shortParam = 1,
                             short primitiveShortParam = 2,
                             Integer integerParam = 3,
                             int primitiveIntParam = 4,
                             Long longParam = 5,
                             long primitiveLongParam = 6,
                             Float floatParam = 7.7f,
                             float primitiveFloatParam = 8.8f,
                             Double doubleParam = 9.9,
                             double primitiveDoubleParam = 10.10,
                             Boolean booleanParam = true,
                             boolean primitiveBooleanParam = true,
                             Byte byteParam = 11,
                             byte primitiveByteParam = 12) {
                [ stringParam: stringParam,
                  integerParam: integerParam,
                  primitiveIntParam: primitiveIntParam,
                  shortParam: shortParam,
                  primitiveShortParam: primitiveShortParam,
                  longParam: longParam,
                  primitiveLongParam: primitiveLongParam,
                  floatParam: floatParam,
                  primitiveFloatParam: primitiveFloatParam,
                  doubleParam: doubleParam,
                  primitiveDoubleParam: primitiveDoubleParam,
                  booleanParam: booleanParam,
                  primitiveBooleanParam: primitiveBooleanParam,
                  byteParam: byteParam,
                  primitiveByteParam: primitiveByteParam ]
            }
        }
        ''')

    }
    
    def setup() {
        GrailsWebUtil.bindMockWebRequest()
        controller = controllerClass.newInstance()
    }
    
    void "Test binding request parameters to basic types"() {
        when:
           controller.params.stringParam = 'Herbert'
           controller.params.shortParam = '1001'
           controller.params.primitiveShortParam = '1002'
           controller.params.integerParam = '1003'
           controller.params.primitiveIntParam = '1004'
           controller.params.longParam = '1005'
           controller.params.primitiveLongParam = '1006'
           controller.params.floatParam = '1007.1007'
           controller.params.primitiveFloatParam = '1008.1008'
           controller.params.doubleParam = '1009.1009'
           controller.params.primitiveDoubleParam = '1010.1010'
           controller.params.booleanParam = 'true'
           controller.params.primitiveBooleanParam = 'true'
           controller.params.byteParam = '101'
           controller.params.primitiveByteParam = '102'
           
           def model = controller.methodAction()
           
        then:
            'Herbert' == model.stringParam
            1001 == model.shortParam
            1002 == model.primitiveShortParam
            1003 == model.integerParam
            1004 == model.primitiveIntParam
            1005 == model.longParam
            1006 == model.primitiveLongParam
            1007.1007f == model.floatParam
            1008.1008f == model.primitiveFloatParam
            1009.1009 == model.doubleParam
            1010.1010 == model.primitiveDoubleParam
            model.booleanParam
            model.primitiveBooleanParam
            101 == model.byteParam
            102 == model.primitiveByteParam
    }

    void "Test conversion errors for parameters with default values"() {
        when:
        controller.params.shortParam = 'bogus'
        controller.params.primitiveShortParam = 'bogus'
        controller.params.integerParam = 'bogus'
        controller.params.primitiveIntParam = 'bogus'
        controller.params.longParam = 'bogus'
        controller.params.primitiveLongParam = 'bogus'
        controller.params.floatParam = 'bogus'
        controller.params.primitiveFloatParam = 'bogus'
        controller.params.doubleParam = 'bogus'
        controller.params.primitiveDoubleParam = 'bogus'
        controller.params.booleanParam = 'bogus'
        controller.params.primitiveBooleanParam = 'bogus'
        controller.params.byteParam = 'bogus'
        controller.params.primitiveByteParam = 'bogus'

        def model = controller.methodActionWithDefaultValues()
            
        then:
            'defaultString' == model.stringParam
            1 == model.shortParam
            2 == model.primitiveShortParam
            3 == model.integerParam
            4 == model.primitiveIntParam
            5 == model.longParam
            6 == model.primitiveLongParam
            7.7f == model.floatParam
            8.8f == model.primitiveFloatParam
            9.9 == model.doubleParam
            10.10 == model.primitiveDoubleParam
            !model.booleanParam
            !model.primitiveBooleanParam
            11 == model.byteParam
            12 == model.primitiveByteParam
    }   
     
    void "Test conversion errors"() {
        when:
           controller.params.shortParam = 'bogus'
           controller.params.primitiveShortParam = 'bogus'
           controller.params.integerParam = 'bogus'
           controller.params.primitiveIntParam = 'bogus'
           controller.params.longParam = 'bogus'
           controller.params.primitiveLongParam = 'bogus'
           controller.params.floatParam = 'bogus'
           controller.params.primitiveFloatParam = 'bogus'
           controller.params.doubleParam = 'bogus'
           controller.params.primitiveDoubleParam = 'bogus'
           controller.params.booleanParam = 'bogus'
           controller.params.primitiveBooleanParam = 'bogus'
           controller.params.byteParam = 'bogus'
           controller.params.primitiveByteParam = 'bogus'

           def model = controller.methodAction()
           
        then:
            null == model.stringParam
            null == model.shortParam
            0 == model.primitiveShortParam
            null == model.integerParam
            0 == model.primitiveIntParam
            null == model.longParam
            0 == model.primitiveLongParam
            null == model.floatParam
            0f == model.primitiveFloatParam
            null == model.doubleParam
            0 == model.primitiveDoubleParam
            !model.booleanParam
            !model.primitiveBooleanParam
            null == model.byteParam
            0 == model.primitiveByteParam
    }

    void "Test uninitialized action parameters"() {
        when:
           def model = controller.methodAction()
           
        then:
            null == model.stringParam
            null == model.shortParam
            0 == model.primitiveShortParam
            null == model.integerParam
            0 == model.primitiveIntParam
            null == model.longParam
            0 == model.primitiveLongParam
            null == model.floatParam
            0f == model.primitiveFloatParam
            null == model.doubleParam
            0 == model.primitiveDoubleParam
            !model.booleanParam
            !model.primitiveBooleanParam
            null == model.byteParam
            0 == model.primitiveByteParam
    }

    void "Test default parameter values"() {
        when:
            def model = controller.methodActionWithDefaultValues()
            
        then:
            'defaultString' == model.stringParam
            1 == model.shortParam
            2 == model.primitiveShortParam
            3 == model.integerParam
            4 == model.primitiveIntParam
            5 == model.longParam
            6 == model.primitiveLongParam
            7.7f == model.floatParam
            8.8f == model.primitiveFloatParam
            9.9 == model.doubleParam
            10.10 == model.primitiveDoubleParam
            model.booleanParam
            model.primitiveBooleanParam
            11 == model.byteParam
            12 == model.primitiveByteParam
    }
    
    void "Test overriding default parameter values"() {
        when:
           controller.params.stringParam = 'Herbert'
           controller.params.shortParam = '1001'
           controller.params.primitiveShortParam = '1002'
           controller.params.integerParam = '1003'
           controller.params.primitiveIntParam = '1004'
           controller.params.longParam = '1005'
           controller.params.primitiveLongParam = '1006'
           controller.params.floatParam = '1007.1007'
           controller.params.primitiveFloatParam = '1008.1008'
           controller.params.doubleParam = '1009.1009'
           controller.params.primitiveDoubleParam = '1010.1010'
           controller.params.booleanParam = 'false'
           controller.params.primitiveBooleanParam = 'false'
           controller.params.byteParam = '101'
           controller.params.primitiveByteParam = '102'

           def model = controller.methodActionWithDefaultValues()
           
        then:
            'Herbert' == model.stringParam
            1001 == model.shortParam
            1002 == model.primitiveShortParam
            1003 == model.integerParam
            1004 == model.primitiveIntParam
            1005 == model.longParam
            1006 == model.primitiveLongParam
            1007.1007f == model.floatParam
            1008.1008f == model.primitiveFloatParam
            1009.1009 == model.doubleParam
            1010.1010 == model.primitiveDoubleParam
            !model.booleanParam
            !model.primitiveBooleanParam
            101 == model.byteParam
            102 == model.primitiveByteParam
    }

    def cleanupSpec() {
        RequestContextHolder.setRequestAttributes(null)
    }
}

