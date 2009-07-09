package org.codehaus.groovy.grails.commons.metaclass;

import java.util.regex.*

/* Copyright 2004-2005 Graeme Rocher
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

class ClosureInvokingDynamicMethodTests extends GroovyTestCase {

    void testClosureInvokingDynamicMethod() {
         def method1 = new ClosureInvokingDynamicMethod(/^find\w+$/, {
                assertTrue it instanceof Matcher
               return "foo"
            }
         )
         def method2 = new ClosureInvokingDynamicMethod(/^find\w+$/, { matcher, args ->
                assertTrue matcher instanceof Matcher
                return args[1]
            }
         )
         def method3 = new ClosureInvokingDynamicMethod(/^find\w+$/, {-> "foo" })

         assertTrue method1.isMethodMatch("findBar")
         assertTrue method2.isMethodMatch("findBar")
         assertTrue method3.isMethodMatch("findBar")

         assertEquals "foo", method1.invoke(this, "findBar", null)
         assertEquals  2, method2.invoke(this, "findBar",[1,2,3] as Object[])
         assertEquals "foo", method3.invoke(this,"findBar", null)
    }

    void testClosureInvokingDynamicMethodFailures() {
        shouldFail {
             new ClosureInvokingDynamicMethod(null, { })
        }

        shouldFail {
             new ClosureInvokingDynamicMethod(null, null)
        }

        shouldFail {
             new ClosureInvokingDynamicMethod("foo", null)
        }
    }
}