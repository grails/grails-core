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
package org.codehaus.groovy.grails.web.pages.exceptions;

import org.codehaus.groovy.grails.exceptions.GrailsException;
import org.codehaus.groovy.grails.exceptions.SourceCodeAware;

/**
 * An exception thrown when processing GSP pages
 *
 * @author Graeme Rocher
 * @since 0.5
 *        <p/>
 *        Created: Feb 23, 2007
 *        Time: 10:45:39 AM
 */
public class GroovyPagesException extends GrailsException implements SourceCodeAware {
    private int lineNumber;
    private String fileName;

    public GroovyPagesException(String message, Exception e) {
        super(message,e);
    }

    public GroovyPagesException(String message, Throwable exception, int lineNumber, String fileName) {
        super(message, exception);
        this.lineNumber = lineNumber;
        this.fileName = fileName;
    }

    public GroovyPagesException(String message) {
        super(message);
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }
}
