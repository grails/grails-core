/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.plugins.codecs

import org.codehaus.groovy.runtime.NullObject

class MD5BytesCodecExtensionMethods {
    // Returns the byte[] of the digest, taken from UTF-8 of the string representation
    // or the raw data coerced to bytes
    static encodeAsMD5Bytes(theTarget) {
        if(theTarget == null || theTarget instanceof NullObject) {
            return null
        }
        DigestUtils.digest("MD5", theTarget)
    }

    static decodeMD5Bytes(theTarget) {
        throw new UnsupportedOperationException("Cannot decode MD5 hashes")
    }
}
