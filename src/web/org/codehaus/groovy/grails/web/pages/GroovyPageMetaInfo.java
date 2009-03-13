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
package org.codehaus.groovy.grails.web.pages;

import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * A class that encapsulates the information necessary to describe a GSP
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 23, 2007
 *        Time: 11:38:40 AM
 */
class GroovyPageMetaInfo {

    private TagLibraryLookup tagLibraryLookup;
    private TagLibraryResolver jspTagLibraryResolver;

    private Class pageClass;
    private long lastModified;
    private InputStream groovySource;
    private String contentType;
    private int[] lineNumbers;
    private Map jspTags = Collections.EMPTY_MAP;
    private GroovyPagesException compilationException;


    public TagLibraryLookup getTagLibraryLookup() {
        return tagLibraryLookup;
    }

    public void setTagLibraryLookup(TagLibraryLookup tagLibraryLookup) {
        this.tagLibraryLookup = tagLibraryLookup;
    }

    public TagLibraryResolver getJspTagLibraryResolver() {
        return jspTagLibraryResolver;
    }

    public void setJspTagLibraryResolver(TagLibraryResolver jspTagLibraryResolver) {
        this.jspTagLibraryResolver = jspTagLibraryResolver;
    }

    public Class getPageClass() {
        return pageClass;
    }

    public void setPageClass(Class pageClass) {
        this.pageClass = pageClass;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public InputStream getGroovySource() {
        return groovySource;
    }

    public void setGroovySource(InputStream groovySource) {
        this.groovySource = groovySource;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int[] getLineNumbers() {
        return lineNumbers;
    }

    public void setLineNumbers(int[] lineNumbers) {
        this.lineNumbers = lineNumbers;
    }

    public void setJspTags(Map jspTags) {
        this.jspTags = jspTags != null ? jspTags : Collections.EMPTY_MAP;
    }

    public Map getJspTags() {
        return jspTags;
    }

    public void setCompilationException(GroovyPagesException e) {
        this.compilationException = e;
    }

    public GroovyPagesException getCompilationException() {
        return compilationException;
    }
}

