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

package org.grails.web.metaclass

import grails.artefact.Controller
import grails.util.GrailsWebMockUtil

import org.grails.core.exceptions.GrailsRuntimeException
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class WithFormMethodTests extends GroovyTestCase {

    private withForm = new WithFormMethod()
    private request

    @Override
    protected void setUp() {
        super.setUp()
        request = GrailsWebMockUtil.bindMockWebRequest()
    }

    @Override
    protected void tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    void testMissingToken() {

        shouldFail(GrailsRuntimeException) {
            withForm.withForm(request) {
                // should not get here
            }.invalidToken {
                throw new GrailsRuntimeException("no token in request")
            }
        }
    }

    void testTokenHolderEmpty() {

        request.session.setAttribute(SynchronizerTokensHolder.HOLDER,new SynchronizerTokensHolder())

        shouldFail(GrailsRuntimeException) {
            withForm.withForm(request) {
                // should not get here
            }.invalidToken {
                throw new GrailsRuntimeException("invalid token")
            }
        }
    }

    void testTokenInvalidWithEmptyTokenHolder() {
        def url = "http://grails.org/submit"

        request.session.setAttribute(SynchronizerTokensHolder.HOLDER,new SynchronizerTokensHolder())

        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,UUID.randomUUID().toString())

        shouldFail(GrailsRuntimeException) {
            withForm.withForm(request) {
                // should not get here
            }.invalidToken {
                throw new GrailsRuntimeException("invalid token")
            }
        }
    }

    void testTokenInvalid() {
        def url = "http://grails.org/submit"

        SynchronizerTokensHolder tokensHolder = new SynchronizerTokensHolder()
        tokensHolder.generateToken(url)
        request.session.setAttribute(SynchronizerTokensHolder.HOLDER,tokensHolder)

        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,UUID.randomUUID().toString())

        shouldFail(GrailsRuntimeException) {
            withForm.withForm(request) {
                // should not get here
            }.invalidToken {
                throw new GrailsRuntimeException("invalid token")
            }
        }
    }

    void testTokenValid() {
        def url = "http://grails.org/submit"

        SynchronizerTokensHolder tokensHolder = new SynchronizerTokensHolder()
        def token = tokensHolder.generateToken(url)
        request.session.setAttribute(SynchronizerTokensHolder.HOLDER,tokensHolder)

        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,token)

        def result = withForm.withForm(request) {
            return [foo:"bar"]
        }.invalidToken {
            throw new GrailsRuntimeException("invalid token")
        }

        assertEquals "bar", result.foo
    }

    void testNonEmptyHolderStays() {
        def url1 = "http://grails.org/submit1"
        def url2 = "http://grails.org/submit2"

        SynchronizerTokensHolder tokensHolder = new SynchronizerTokensHolder()
        def token1 = tokensHolder.generateToken(url1)
        def token2 = tokensHolder.generateToken(url2)

        request.session.setAttribute(SynchronizerTokensHolder.HOLDER,tokensHolder)

        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url1)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,token1)

        def result = withForm.withForm(request) {
            return [foo:"bar"]
        }.invalidToken {
            throw new GrailsRuntimeException("invalid token")
        }

        tokensHolder = request.session.getAttribute(SynchronizerTokensHolder.HOLDER)
        assertNotNull tokensHolder
        assertTrue tokensHolder.isValid(url2, token2)
    }

    void testEmptyHolderIsDeleted() {
        def url = "http://grails.org/submit"

        SynchronizerTokensHolder tokensHolder = new SynchronizerTokensHolder()
        def token = tokensHolder.generateToken(url)
        request.session.setAttribute(SynchronizerTokensHolder.HOLDER,tokensHolder)

        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,token)

        def result = withForm.withForm(request) {
            return [foo:"bar"]
        }.invalidToken {
            throw new GrailsRuntimeException("invalid token")
        }

        assertNull request.session.getAttribute(SynchronizerTokensHolder.HOLDER)
    }

    void testHandleDoubleSubmit() {
        def url = "http://grails.org/submit"

        SynchronizerTokensHolder tokensHolder = new SynchronizerTokensHolder()
        def token = tokensHolder.generateToken(url)
        request.session.setAttribute(SynchronizerTokensHolder.HOLDER,tokensHolder)

        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,token)

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

    void testHandleSubmitOfTwoForms() {
        def url1 = "http://grails.org/submit1"
        def url2 = "http://grails.org/submit2"

        SynchronizerTokensHolder tokensHolder = new SynchronizerTokensHolder()
        def token1 = tokensHolder.generateToken(url1)
        def token2 = tokensHolder.generateToken(url2)

        request.session.setAttribute(SynchronizerTokensHolder.HOLDER,tokensHolder)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url1)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,token1)

        def result1 = withForm.withForm(request) {
            return [foo:"bar"]
        }.invalidToken {
            throw new GrailsRuntimeException("invalid token")
        }

        assertEquals "bar", result1.foo

        def request2 = GrailsWebMockUtil.bindMockWebRequest()
        request2.session.setAttribute(SynchronizerTokensHolder.HOLDER,tokensHolder)
        request2.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url2)
        request2.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,token2)

        def result2 = withForm.withForm(request2) {
            return [foo:"bar"]
        }.invalidToken {
            throw new GrailsRuntimeException("invalid token")
        }

        assertEquals "bar", result2.foo
    }

    void testHandleSubmitOfTwoFormsWithSameURL() {
        def url1 = "http://grails.org/submit"
        def url2 = "http://grails.org/submit"

        SynchronizerTokensHolder tokensHolder = new SynchronizerTokensHolder()
        def token1 = tokensHolder.generateToken(url1)
        def token2 = tokensHolder.generateToken(url2)

        request.session.setAttribute(SynchronizerTokensHolder.HOLDER,tokensHolder)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url1)
        request.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,token1)

        def result1 = withForm.withForm(request) {
            return [foo:"bar"]
        }.invalidToken {
            throw new GrailsRuntimeException("invalid token")
        }

        assertEquals "bar", result1.foo

        def request2 = GrailsWebMockUtil.bindMockWebRequest()
        request2.session.setAttribute(SynchronizerTokensHolder.HOLDER,tokensHolder)
        request2.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_URI,url2)
        request2.currentRequest.addParameter(SynchronizerTokensHolder.TOKEN_KEY,token2)

        def result2 = withForm.withForm(request2) {
            return [foo:"bar"]
        }.invalidToken {
            throw new GrailsRuntimeException("invalid token")
        }

        assertEquals "bar", result2.foo

        shouldFail(GrailsRuntimeException) {
            withForm.withForm(request2) {
                return [foo:"bar"]
            }.invalidToken {
                throw new GrailsRuntimeException("invalid token")
            }
        }
    }
}

class WithFormMethod implements Controller {}