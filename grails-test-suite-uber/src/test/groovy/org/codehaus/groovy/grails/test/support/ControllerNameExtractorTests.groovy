package org.codehaus.groovy.grails.test.support

class ControllerNameExtractorTests extends GroovyTestCase {

    void testExtractControllerNameFromTestClassName() {

        String[] testClassSuffixes = ['Test', 'Tests']

        assertEquals 'the', ControllerNameExtractor.extractControllerNameFromTestClassName(
                'TheControllerTest', testClassSuffixes)

        assertEquals 'the', ControllerNameExtractor.extractControllerNameFromTestClassName(
                'TheControllerTests', testClassSuffixes)

        assertEquals 'the', ControllerNameExtractor.extractControllerNameFromTestClassName(
                'com.foo.TheControllerTest', testClassSuffixes)

        assertEquals 'the', ControllerNameExtractor.extractControllerNameFromTestClassName(
                'com.foo.TheControllerTests', testClassSuffixes)
    }
}
