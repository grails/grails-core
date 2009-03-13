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

import org.springframework.core.io.ByteArrayResource;

import java.io.UnsupportedEncodingException;

/**
 * A resource that mocks the behavior of a FileResource
 * 
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Feb 6, 2009
 */
public class MockFileResource extends ByteArrayResource{
    private String fileName;

    public MockFileResource(String fileName, String contents) {
        super(contents.getBytes());
        this.fileName = fileName;
    }

    public MockFileResource(String fileName, String contents, String encoding) throws UnsupportedEncodingException {
        super(contents.getBytes(encoding));
        this.fileName = fileName;
    }

    @Override
    public String getFilename() throws IllegalStateException {
        return this.fileName;
    }
}
