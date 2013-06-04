/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.codecs;

import java.io.IOException;

import org.codehaus.groovy.grails.support.encoding.CodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncodingState;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;

/**
 * Abstract base class for implementing encoders that do character replacements
 * Implements the {@link StreamingEncoder} interface that enables efficient
 * streaming encoding
 *
 * @author Lari Hotari
 * @since 2.3
 */
public abstract class AbstractCharReplacementEncoder implements Encoder, StreamingEncoder {
    protected CodecIdentifier codecIdentifier;

    public AbstractCharReplacementEncoder(CodecIdentifier codecIdentifier) {
        this.codecIdentifier = codecIdentifier;
    }

    /**
     * Escape the character, return null if no replacement has to be made
     *
     * @param ch the character to escape
     * @param previousChar  the previous char
     * @return the replacement string, null if no replacement has to be made
     */
    protected abstract String escapeCharacter(char ch, char previousChar);

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.encoding.Encoder#encode(java.lang.Object)
     */
    public Object encode(Object o) {
        if (o == null) {
            return null;
        }

        CharSequence str = null;
        if (o instanceof CharSequence) {
            str = (CharSequence)o;
        }
        else if (o instanceof Character) {
            String escaped = escapeCharacter((Character)o, (char)0);
            if (escaped != null) {
                return escaped;
            }
            else {
                return o;
            }
        }
        else {
            str = String.valueOf(o);
        }

        if (str.length() == 0) {
            return str;
        }

        StringBuilder sb = null;
        int n = str.length(), i;
        int startPos = -1;
        char prevChar = (char)0;
        for (i = 0; i < n; i++) {
            char ch = str.charAt(i);
            if (startPos == -1) {
                startPos = i;
            }
            String escaped = escapeCharacter(ch, prevChar);
            if (escaped != null) {
                if (sb == null) {
                    sb = new StringBuilder(str.length() * 110 / 100);
                }
                if (i - startPos > 0) {
                    sb.append(str, startPos, i);
                }
                if (escaped.length() > 0) {
                    sb.append(escaped);
                }
                startPos = -1;
            }
            prevChar = ch;
        }
        if (sb != null) {
            if (startPos > -1 && i - startPos > 0) {
                sb.append(str, startPos, i);
            }
            return sb.toString();
        }
        else {
            return str;
        }
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.encoding.StreamingEncoder#encodeToStream(org.codehaus.groovy.grails.support.encoding.Encoder, java.lang.CharSequence, int, int, org.codehaus.groovy.grails.support.encoding.EncodedAppender, org.codehaus.groovy.grails.support.encoding.EncodingState)
     */
    public void encodeToStream(Encoder thisInstance, CharSequence str, int off, int len, EncodedAppender appender, EncodingState encodingState)
            throws IOException {
        if (str == null || len <= 0) {
            return;
        }
        int n = Math.min(str.length(), off + len);
        int i;
        int startPos = -1;
        char prevChar = (char)0;
        for (i = off; i < n; i++) {
            char ch = str.charAt(i);
            if (startPos == -1) {
                startPos = i;
            }
            String escaped = escapeCharacter(ch, prevChar);
            if (escaped != null) {
                if (i - startPos > 0) {
                    appender.appendEncoded(thisInstance, encodingState, str, startPos, i - startPos);
                }
                if (escaped.length() > 0) {
                    appender.appendEncoded(thisInstance, encodingState, escaped, 0, escaped.length());
                }
                startPos = -1;
            }
            prevChar = ch;
        }
        if (startPos > -1 && i - startPos > 0) {
            appender.appendEncoded(thisInstance, encodingState, str, startPos, i - startPos);
        }
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.encoding.Encoder#markEncoded(java.lang.CharSequence)
     */
    public void markEncoded(CharSequence string) {
        // no need to implement, wrapped automaticly
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.encoding.Encoder#isSafe()
     */
    public boolean isSafe() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.encoding.Encoder#isApplyToSafelyEncoded()
     */
    public boolean isApplyToSafelyEncoded() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.encoding.CodecIdentifierProvider#getCodecIdentifier()
     */
    public CodecIdentifier getCodecIdentifier() {
        return codecIdentifier;
    }
}
