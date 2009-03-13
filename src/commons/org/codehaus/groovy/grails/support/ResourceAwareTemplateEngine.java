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
package org.codehaus.groovy.grails.support;

import groovy.text.Template;
import groovy.text.TemplateEngine;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * An abstract TemplateEngine that extends the default Groovy TemplateEngine (@see groovy.text.TemplateEngine) and
 * provides the ability to create templates from the Spring Resource API
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Feb 22, 2007
 *        Time: 6:37:08 PM
 */
abstract public class ResourceAwareTemplateEngine extends TemplateEngine {


    /**
     * Creates the specified Template using the given Spring Resource
     *
     * @param resource The Spring Resource to create the template for
     * @return A Template instance
     * @throws IOException Thrown when there was an error reading the Template
     * @throws ClassNotFoundException Thrown when there was a problem loading the Template into a class
     */
    public Template createTemplate(Resource resource) throws IOException, ClassNotFoundException {
        return createTemplate(resource.getInputStream());
    }

    public final Template createTemplate(Reader reader) throws IOException {
        return createTemplate(new ReaderInputStream(reader));
    }
    /**
     * Unlike groovy.text.TemplateEngine, implementors need to provide an implementation that operates
     * with an InputStream
     *
     * @param inputStream The InputStream
     * @return A Template instance
     * @throws IOException Thrown when an IO error occurs reading the stream
     */
    abstract public Template createTemplate(InputStream inputStream) throws IOException;

    // wraps a Reader in an InputStream
    private class ReaderInputStream extends InputStream {
        private Reader reader;

        public ReaderInputStream(Reader reader) {
            this.reader = reader;
        }
        public int read() throws IOException {
            return reader.read();
        }
    }

}
