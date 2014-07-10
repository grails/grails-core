package org.grails.test.support

import org.grails.test.support.ControllerNameExtractor

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

        assertEquals 'the', ControllerNameExtractor.extractControllerNameFromTestClassName(
                'com.foo.TheControllerIntegrationTests', testClassSuffixes)

        assertEquals 'the', ControllerNameExtractor.extractControllerNameFromTestClassName(
            'com.foo.TheController', null)

        assertEquals null, ControllerNameExtractor.extractControllerNameFromTestClassName(
            'com.foo.TheTests', testClassSuffixes)
    }
}
