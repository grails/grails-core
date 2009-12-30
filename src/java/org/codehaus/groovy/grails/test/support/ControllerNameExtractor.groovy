/*
 * Copyright 2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.grails.test.support

import grails.util.GrailsNameUtils

class ControllerNameExtractor {

    static String extractControllerNameFromTestClassName(String testClassName, String[] testClassSuffixes) {
        if (!testClassSuffixes) testClassSuffixes = [''] as String[]
        
        def matchingTail
        testClassSuffixes.find { 
            def tail = "Controller$it"
            if (testClassName.endsWith(tail)) matchingTail = tail }
        
        if (matchingTail) {
            testClassName[0..(testClassName.size() - matchingTail.size() - 1)]
        } else {
            null
        }
    } 
    
}