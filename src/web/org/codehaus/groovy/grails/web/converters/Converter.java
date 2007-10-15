/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.web.converters;

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;

import javax.servlet.http.HttpServletResponse;
import java.io.Writer;

/**
 * An interface that defines an Object that can convert an instance and render it to the
 * response or a supplied writer
 *
 * @author Siegfried Puchbauer
 */
public interface Converter {

    public static String DEFAULT_REQUEST_ENCODING = "UTF-8";

    /**
     * Marshalls the target and writes it to a java.io.Writer
     *
     * @param out The Writer to write to
     * @throws ConverterException
     */
    public void render(Writer out) throws ConverterException;

    /**
     * Marshalls the target and writes it a HttpServletResponse
     * The response will be comitted after this operation
     *
     * @param response The response to write to
     * @throws ConverterException
     */
    public void render(HttpServletResponse response) throws ConverterException;

//    /**
//     * Unmarshalls the given Source and returns the Java respresention
//     *
//     * @param source a String
//     * @return the Java representation
//     * @throws ConverterException
//     */
//    public Object parse(String source) throws ConverterException;
//
//    /**
//     * Unmarshalls the given Source and returns the Java respresention
//     *
//     * @param is The InputStream to read from
//     * @param encoding The Character Encoding to use
//     * @return the Java respresentation
//     * @throws ConverterException
//     */
//    public Object parse(InputStream is, String encoding) throws ConverterException;
//
//    /**
//     * Unmarshalls the given Source and returns the Java respresention
//     *
//     * @param request The HttpServletRequest to read from
//     * @return the Java representation
//     * @throws ConverterException
//     */
//    public Object parse(HttpServletRequest request) throws ConverterException;
//
//    /**
//     * Sets the Object which is later converted using the render method
//     * @param target the Object
//     */
//    public void setTarget(Object target);

}
