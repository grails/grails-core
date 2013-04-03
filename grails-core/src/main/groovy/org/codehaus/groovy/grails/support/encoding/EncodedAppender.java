/* Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;

public interface EncodedAppender {
    public void append(Encoder encoder, EncodingState encodingState, CharSequence str, int off, int len) throws IOException ;
    public void append(Encoder encoder, EncodingState encodingState, char[] b, int off, int len) throws IOException ;
    public void append(Encoder encoder, StreamEncodeable streamEncodeable) throws IOException ;
    public void append(Encoder encoder, char ch) throws IOException;
    public void flush() throws IOException;
}
