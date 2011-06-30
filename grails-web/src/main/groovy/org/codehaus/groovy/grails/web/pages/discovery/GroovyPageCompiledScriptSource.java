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

package org.codehaus.groovy.grails.web.pages.discovery;

import java.io.IOException;

/**
 * Represents a pre-compiled GSP
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GroovyPageCompiledScriptSource implements GroovyPageScriptSource {

    private String uri;
    private Class compiledClass;

    public GroovyPageCompiledScriptSource(String uri, Class compiledClass) {
        this.uri = uri;
        this.compiledClass = compiledClass;
    }

    public String getURI() {
        return uri;
    }

    /**
     *
     * @return The compiled class
     */
    public Class getCompiledClass() {
        return compiledClass;
    }

    public String getScriptAsString() throws IOException {
        throw new UnsupportedOperationException("You cannot retrieve the source of a pre-compiled GSP script: " + uri);
    }

    public boolean isModified() {
        return false; // not modifiable
    }

    public String suggestedClassName() {
        return compiledClass.getName();
    }
}
