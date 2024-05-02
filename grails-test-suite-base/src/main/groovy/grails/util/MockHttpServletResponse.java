/*
 * Copyright 2024 original authors
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
package grails.util;

/**
 * Subclass Spring's MockHttpServletResponse to tag the methods that have been deprecated in
 * the Servlet API.
 *
 * Because Spring's MockHttpServletResponse doesn't tag these methods as deprecated, the
 * compiler outputs noisy warnings complaining that we're using deprecated methods if we use
 * the raw MockHttpServletResponse from Spring.  By subclassing Spring's
 * MockHttpServletResponse and tagging the methods as deprecated, we acknowledge to the
 * compiler that these methods are deprecated, and we silence the compiler warnings.
 *
 * Created: 08-Feb-2008
 */
class MockHttpServletResponse extends org.springframework.mock.web.MockHttpServletResponse {

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return super.encodeRedirectURL(url);
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return super.encodeURL(url);
    }

    @Override
    @Deprecated
    public void setStatus(int status, String errorMessage) {
        super.setStatus(status, errorMessage);
    }
}
