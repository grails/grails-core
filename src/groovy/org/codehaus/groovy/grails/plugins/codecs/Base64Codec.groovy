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

package org.codehaus.groovy.grails.plugins.codecs


import org.codehaus.groovy.runtime.DefaultGroovyMethods

 /**
  * A code that encodes and decodes Objects using Base64 encoding
  *
  * @author Drew Varner
  */
class Base64Codec {
    static encode = { theTarget ->
       if (theTarget == null) {
           return null
       } else if (theTarget instanceof byte[] || theTarget instanceof Byte[]) {
           return theTarget.encodeBase64()
       } else {
    	   return theTarget.toString().getBytes().encodeBase64()
       }
    }

    static decode = { theTarget ->
        if (theTarget == null) {
            return null
        } else {
            return DefaultGroovyMethods.decodeBase64(theTarget.toString())
        }
    }
}
