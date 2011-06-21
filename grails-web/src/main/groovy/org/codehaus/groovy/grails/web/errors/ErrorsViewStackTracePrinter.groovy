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
package org.codehaus.groovy.grails.web.errors

import org.codehaus.groovy.grails.core.io.ResourceLocator
import org.codehaus.groovy.grails.exceptions.DefaultStackTracePrinter
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.springframework.core.io.Resource

 /**
 * Customized Stack trace output for the errors view
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class ErrorsViewStackTracePrinter extends DefaultStackTracePrinter{

    ErrorsViewStackTracePrinter(ResourceLocator resourceLocator) {
        super(resourceLocator)
    }

    @Override protected boolean shouldSkipNextCause(Throwable e) {
        return super.shouldSkipNextCause(e)
    }

    @Override
    String prettyPrint(Throwable t) {
        if(t instanceof GrailsWrappedRuntimeException) {
            return super.prettyPrint(t.cause)
        }
        else {
            return super.prettyPrint(t)
        }
    }

    @Override
    String prettyPrintCodeSnippet(Throwable exception) {
        if(exception instanceof GrailsWrappedRuntimeException) {
            return super.prettyPrintCodeSnippet(exception.cause)
        }
        else {
            return super.prettyPrintCodeSnippet(exception)
        }
    }




    @Override
    String formatCodeSnippetStart(Resource resource, int lineNumber) {
        def path = resource.filename
        // try calc better path
        try {
            def abs = resource.file.absolutePath
            def i = abs.indexOf(GrailsResourceUtils.GRAILS_APP_DIR)
            if(i > -1) {
                path = abs[i..-1]
            }
        } catch (e) {
            path = resource.filename
        }
        """<h2>Around line ${lineNumber} of <span class="filename">${path}</span></h2>
<pre class="snippet">"""
    }

    @Override
    String formatCodeSnippetEnd(Resource resource, int lineNumber) {
        "</pre>"
    }

    @Override protected String formatCodeSnippetLine(int currentLineNumber, Object currentLine) {
        return "<code class=\"line\"><span class=\"lineNumber\">${currentLineNumber}:</span>${currentLine.encodeAsHTML()}</code>"
    }

    @Override protected String formatCodeSnippetErrorLine(int currentLineNumber, Object currentLine) {
        return "<code class=\"line error\"><span class=\"lineNumber\">${currentLineNumber}:</span>${currentLine.encodeAsHTML()}</code>"
    }

    @Override protected int getLineNumberInfo(Throwable cause, int defaultInfo) {
        return super.getLineNumberInfo(cause, defaultInfo)
    }


}
