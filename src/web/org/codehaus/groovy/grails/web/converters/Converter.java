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

    public void render(Writer out) throws ConverterException;

    public void render(HttpServletResponse response) throws ConverterException;

}
