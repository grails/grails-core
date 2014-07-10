/*
 * Copyright 2014 the original author or authors.
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
package org.grails.web.sitemesh;

import java.io.IOException;
import java.io.Writer;

import com.opensymphony.sitemesh.Content;

final class TokenizedHTMLPage2Content implements Content {
    private final GrailsTokenizedHTMLPage page;

    TokenizedHTMLPage2Content(GrailsTokenizedHTMLPage page) {
        this.page = page;
    }

    @Override
    public void writeOriginal(Writer out) throws IOException {
        out.write(page.getData());
    }

    @Override
    public void writeHead(Writer out) throws IOException {
        page.writeHead(out);              
    }

    @Override
    public void writeBody(Writer out) throws IOException {
        page.writeBody(out);
    }

    @Override
    public int originalLength() {
        return page.getContentLength();
    }

    @Override
    public String getTitle() {
        return page.getTitle();
    }

    @Override
    public String[] getPropertyKeys() {
        return getPropertyKeys();
    }

    @Override
    public String getProperty(String name) {
        return page.getProperty(name);
    }

    @Override
    public void addProperty(String name, String value) {
        page.addProperty(name, value);                    
    }

    public GrailsTokenizedHTMLPage getPage() {
        return page;
    }
}