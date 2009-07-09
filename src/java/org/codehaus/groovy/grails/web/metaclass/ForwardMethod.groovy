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

import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.springframework.util.Assert
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.apache.commons.beanutils.BeanUtils
import org.codehaus.groovy.grails.web.util.WebUtils
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.codehaus.groovy.grails.web.mapping.ForwardUrlMappingInfo

/**
 * Implements performing a forward
 * 
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Jan 9, 2009
 */

public class ForwardMethod {

    public static final String CALLED = "org.codehaus.groovy.grails.FORWARD_CALLED"

    UrlMappingsHolder holder
    public ForwardMethod(UrlMappingsHolder urlMappingsHolder) {
        super();

        Assert.notNull(urlMappingsHolder, "Argument [urlMappingsHolder] is required")
        this.holder = urlMappingsHolder
    }

    def forward(HttpServletRequest request, HttpServletResponse response, Map params) {
        def urlInfo = new ForwardUrlMappingInfo()
        if(params.action && !params.controller) {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup(request)
            urlInfo.controllerName = webRequest?.controllerName
        }

        BeanUtils.populate(urlInfo, params)

        String uri
        if(params.model instanceof Map) {
            uri = WebUtils.forwardRequestForUrlMappingInfo(request, response, urlInfo, params.model, true)
        }
        else {
            uri = WebUtils.forwardRequestForUrlMappingInfo(request, response, urlInfo, Collections.EMPTY_MAP, true)
        }
        request.setAttribute(CALLED,true)
        return uri
    }
}
