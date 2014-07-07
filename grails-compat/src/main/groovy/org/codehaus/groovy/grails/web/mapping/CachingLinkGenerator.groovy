/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.web.mapping

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.web.mapping.CachingLinkGenerator} instead
 */
@Deprecated
@CompileStatic
class CachingLinkGenerator extends org.grails.web.mapping.CachingLinkGenerator {
    CachingLinkGenerator(String serverBaseURL, String contextPath) {
        super(serverBaseURL, contextPath)
    }

    CachingLinkGenerator(String serverBaseURL) {
        super(serverBaseURL)
    }

    CachingLinkGenerator(String serverBaseURL, Map<String, Object> linkCache) {
        super(serverBaseURL, linkCache)
    }

    CachingLinkGenerator(String serverBaseURL, String contextPath, Map<String, Object> linkCache) {
        super(serverBaseURL, contextPath, linkCache)
    }
}
