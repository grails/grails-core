/* Copyright 2004-2005 the original author or authors.
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

package org.codehaus.groovy.grails.web.metaclass

import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.exceptions.GrailsRuntimeException
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerToken
import org.springframework.web.context.request.RequestContextHolder

 /**
 * @author Graeme Rocher
 * @since 1.1
 */
class WithFormMethodTests extends GroovyTestCase {
    @Override protected void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }


    void testMissingToken() {
        def withForm = new WithFormMethod()

        def request = GrailsWebUtil.bindMockWebRequest()

        shouldFail(GrailsRuntimeException) {
            withForm.withForm(request) {
               // should not get here
            }.invalidToken {
                throw new GrailsRuntimeException("no token in request")
            }
        }

    }

    void testTokenNotProvided() {
        def withForm = new WithFormMethod()

        def request = GrailsWebUtil.bindMockWebRequest()

        request.session.setAttribute(SynchronizerToken.KEY,new SynchronizerToken())

        shouldFail(GrailsRuntimeException) {
            withForm.withForm(request) {
               // should not get here
            }.invalidToken {
                throw new GrailsRuntimeException("invalid token")
            }
        }
    }

    void testTokenInvalid() {
        def withForm = new WithFormMethod()

        def request = GrailsWebUtil.bindMockWebRequest()

        request.session.setAttribute(SynchronizerToken.KEY,new SynchronizerToken())

        request.currentRequest.addParameter(SynchronizerToken.KEY,UUID.randomUUID().toString())

        shouldFail(GrailsRuntimeException) {
            withForm.withForm(request) {
               // should not get here
            }.invalidToken {
                throw new GrailsRuntimeException("invalid token")
            }
        }
    }

    void testTokenValid() {
       def withForm = new WithFormMethod()

        def request = GrailsWebUtil.bindMockWebRequest()

        SynchronizerToken token = new SynchronizerToken()
        request.session.setAttribute(SynchronizerToken.KEY,token)

        request.currentRequest.addParameter(SynchronizerToken.KEY,token.currentToken.toString())


        def result = withForm.withForm(request) {
           return [foo:"bar"]
        }.invalidToken {
            throw new GrailsRuntimeException("invalid token")
        }

        assertEquals "bar", result.foo
    }

    void testHandleDoubleSubmit() {
        def withForm = new WithFormMethod()

        def request = GrailsWebUtil.bindMockWebRequest()

        SynchronizerToken token = new SynchronizerToken()
        request.session.setAttribute(SynchronizerToken.KEY,token)

        request.currentRequest.addParameter(SynchronizerToken.KEY,token.currentToken.toString())

        def result = withForm.withForm(request) {
           return [foo:"bar"]
        }.invalidToken {
            throw new GrailsRuntimeException("invalid token")
        }

        assertEquals "bar", result.foo

        shouldFail(GrailsRuntimeException) {
            withForm.withForm(request) {
               // should not get here
            }.invalidToken {
                throw new GrailsRuntimeException("duplicate token")
            }
        }
    }
}
