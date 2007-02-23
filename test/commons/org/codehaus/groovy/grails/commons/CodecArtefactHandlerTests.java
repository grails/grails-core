/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons;

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;

/**
 * @author Marc Palmer
 * @since 22-Feb-2007
 */
public class CodecArtefactHandlerTests extends TestCase {

    public void testIsCodecClass() throws Exception
    {

        ArtefactHandler handler = new CodecArtefactHandler();
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class fullCodecClass = gcl.parseClass("class FullCodec {\n" +
                "static def encode = { str -> }\n" +
                "static def decode = { str -> }\n" +
                "}\n");
        assertTrue("class was an encoder/decoder", handler.isArtefact( fullCodecClass));

        Class decodeOnlyCodecClass = gcl.parseClass("class DecodeOnlyCodec {\n" +
                "static def decode = { str -> }\n" +
                "}\n");
        assertTrue("class was a decoder", handler.isArtefact(decodeOnlyCodecClass));

        Class encodeOnlyCodecClass = gcl.parseClass("class EncodeOnlyCodec {\n" +
                "static def encode = { str -> }\n" +
                "}\n");
        assertTrue("class was an encoder", handler.isArtefact(encodeOnlyCodecClass));

        Class emptyCodecClass = gcl.parseClass("class EmptyCodec {\n" +
                "}\n");
        assertFalse("codec class had no encode or decode method", handler.isArtefact(emptyCodecClass));

        Class nonCodecClass = gcl.parseClass("class SomeFoo {\n" +
                "static def encode = { str -> }\n" +
                "}\n");
        assertFalse("class was not a codec", handler.isArtefact(nonCodecClass));
    }

}
