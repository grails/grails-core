/*
 * Copyright 2002-2013 the original author or authors.
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
package org.codehaus.groovy.grails.io.support

import groovy.transform.CompileStatic

/**
 *
 * @author Graeme Rocher
 * @since 2.4
 */
@CompileStatic
class GrailsIOUtils extends IOUtils{

    /**
     * Gracefully opens a stream for a file, throwing exceptions where appropriate. Based off the commons-io method
     *
     * @param file The file
     * @return The stream
     */
    static BufferedInputStream openStream(File file)  {
        if(!file.exists()) {
            throw new FileNotFoundException("File $file does not exist")
        }
        else {
            if ( file.directory ) {
                throw new IOException("File $file exists but is a directory")
            }
            else if ( !file.canRead() ) {
                throw new IOException("File $file cannot be read")
            }
            else {
                file.newInputStream()
            }
        }
    }

    /**
     * Convert a reader to a String, reading the data from the reader
     * @param reader The reader
     * @return The string
     */
    static String toString(Reader reader) {
        def writer = new StringWriter()
        copy reader, writer
        writer.toString()
    }

    /**
     * Convert a stream to a String, reading the data from the stream
     * @param stream The stream
     * @return The string
     */
    static String toString(InputStream stream, String encoding = null) {
        def writer = new StringWriter()
        copy stream, writer, encoding
        writer.toString()
    }

    /**
     * Copy an InputStream to the given writer with the given encoding
     * @param input The input
     * @param output The writer
     * @param encoding The encoding
     */
    static void copy(InputStream input, Writer output, String encoding = null) {
        def reader = encoding ? new InputStreamReader(input) : new InputStreamReader(input)
        copy(reader, output)
    }
}
