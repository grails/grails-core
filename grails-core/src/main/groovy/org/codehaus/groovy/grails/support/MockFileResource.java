/*
 * Copyright 2004-2005 Graeme Rocher
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

import java.io.UnsupportedEncodingException;

import org.springframework.core.io.ByteArrayResource;

/**
 * Mocks the behavior of a FileResource.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class MockFileResource extends ByteArrayResource {

    private String fileName;

    public MockFileResource(String fileName, String contents) throws UnsupportedEncodingException {
        super(contents.getBytes("UTF-8"));
        this.fileName = fileName;
    }

    public MockFileResource(String fileName, String contents, String encoding) throws UnsupportedEncodingException {
        super(contents.getBytes(encoding));
        this.fileName = fileName;
    }

    @Override
    public String getFilename() {
        return fileName;
    }
}
