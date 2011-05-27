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
package org.codehaus.groovy.grails.web.mime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Graeme Rocher
 * @since 1.4
 */
public class DefaultMimeUtility implements MimeUtility {

    private List<MimeType> mimeTypes;
    private Map<String, MimeType> extensionToMimeMap = new HashMap<String, MimeType>();

    public DefaultMimeUtility(MimeType[] mimeTypes) {
        this(Arrays.asList(mimeTypes));
    }

    public DefaultMimeUtility(List<MimeType> mimeTypes) {
        this.mimeTypes = mimeTypes;
        for (MimeType mimeType : mimeTypes) {
            final String ext = mimeType.getExtension();
            if (!extensionToMimeMap.containsKey(ext)) {
                extensionToMimeMap.put(ext,mimeType);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<MimeType> getKnownMimeTypes() {
        return mimeTypes;
    }

    /**
     * {@inheritDoc}
     */
    public MimeType getMimeTypeForExtension(String extension) {
        if (extension != null) {
            return extensionToMimeMap.get(extension);
        }
        return null;
    }

    public MimeType getMimeTypeForURI(String uri) {
        if (uri != null) {
            final int i = uri.lastIndexOf('.');
            final int length = uri.length();
            if (i > -1 && i < length) {
                final String extension = uri.substring(i+1, length);
                return getMimeTypeForExtension(extension);
            }
        }
        return null;
    }
}
