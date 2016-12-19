/*
 * Copyright 2012 SpringSource
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
package org.grails.core.io;

import org.grails.io.support.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * Bridges Grails and Spring Resource APIs
 *
 * @author Graeme Rocher
 * @since 2.2
 */
public class SpringResource implements Resource{
    org.springframework.core.io.Resource springResource;

    public SpringResource(org.springframework.core.io.Resource springResource) {
        this.springResource = springResource;
    }

    public InputStream getInputStream() throws IOException {
        return springResource.getInputStream();
    }

    public boolean exists() {
        return springResource.exists();
    }

    public boolean isReadable() {
        return springResource.isReadable();
    }

    public URL getURL() throws IOException {
        return springResource.getURL();
    }

    public URI getURI() throws IOException {
        return springResource.getURI();
    }

    public File getFile() throws IOException {
        return springResource.getFile();
    }

    public long contentLength() throws IOException {
        return springResource.contentLength();
    }

    public long lastModified() throws IOException {
        return springResource.lastModified();
    }

    public String getFilename() {
        return springResource.getFilename();
    }

    public String getDescription() {
        return springResource.getDescription();
    }

    public Resource createRelative(String relativePath) {
        try {
            return new SpringResource(springResource.createRelative(relativePath));
        } catch (IOException e) {
            return null;
        }
    }

}
