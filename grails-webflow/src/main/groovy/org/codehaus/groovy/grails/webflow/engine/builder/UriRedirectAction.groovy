/* Copyright 2011 the original author or authors
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
package org.codehaus.groovy.grails.webflow.engine.builder

import org.codehaus.groovy.grails.commons.metaclass.BeanBinding
import org.codehaus.groovy.grails.commons.metaclass.PropertyExpression
import org.codehaus.groovy.grails.web.mapping.UrlCreator
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder

import org.springframework.webflow.action.AbstractAction
import org.springframework.webflow.execution.Event
import org.springframework.webflow.execution.RequestContext

/**
* Takes the url or uri formulated in the builder and produces an ExternalRedirect at runtime.
*
* @author Adrian Stachowiak
* @since 1.0
*/
class UriRedirectAction extends AbstractAction {

   def uri

   protected Event doExecute(RequestContext context) {
       def uri = uri
       if (uri instanceof PropertyExpression) {
          def delegate = new ExpressionDelegate(context)
          uri = new GroovyShell(new BeanBinding(delegate)).evaluate(uri.getValue())
       }

       context.getExternalContext().requestExternalRedirect(uri)
       success()
   }
}
