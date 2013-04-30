/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.codehaus.groovy.grails.web.pages.FastStringPrintWriter
import org.springframework.util.Assert

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class JspTagLibImpl implements JspTagLib {

    private uri
    private tags = [:]

    JspTagLibImpl(String uri, Map tagClasses) {
        Assert.notNull uri, "The URI of the tag library must be specified!"
        this.uri = uri
        for (t in tagClasses) {
            tags[t.key] = new JspTagImpl(t.value)
        }
    }

    JspTag getTag(String name) {
        return tags[name]
    }

    String getURI() {
        return uri
    }

    /**
     * Overrides invoke method so tags can be invoked as methods
     */
    Object invokeMethod(String name, Object args) {
        JspTag tag = getTag(name)

        if (tag) {
            args = args ?: [[:]] // default to an list with an empty map inside
            def sw = new FastStringPrintWriter()

            Map attrs = args[0] instanceof Map ? args[0] : [:]
            def body = args[0] instanceof Closure ? args[0] : null
            if (args.size() > 1) body = args[1] instanceof Closure ? args[1] : null
            if (body == null && args.size() > 1) {
                body = { args[1] }
            }
            else {
                body = {}
            }

            tag.doTag sw,attrs,body

            return sw.toString()
        }

        return super.invokeMethod(name, args)
    }
}
