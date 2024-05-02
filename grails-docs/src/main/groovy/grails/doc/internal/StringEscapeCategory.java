/*
 * Copyright 2024 original authors
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
package grails.doc.internal;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringEscapeUtils;

public class StringEscapeCategory {
    private StringEscapeCategory() {
    }

    public static String encodeAsUrlPath(String str) {
        try {
            String uri = new URI("http", "localhost", '/' + str, "").toASCIIString();
            return uri.substring(17, uri.length() - 1);
        }
        catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String encodeAsUrlFragment(String str) {
        try {
            String uri = new URI("http", "localhost", "/", str).toASCIIString();
            return uri.substring(18, uri.length());
        }
        catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String encodeAsHtml(String str) {
        return StringEscapeUtils.escapeHtml(str);
    }
}
