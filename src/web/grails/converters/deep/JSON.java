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
package grails.converters.deep;

import org.codehaus.groovy.grails.web.converters.configuration.ConverterConfiguration;
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder;

/**
 * A converter that converts domain classes, Maps, Lists, Arrays, POJOs and POGOs
 * to JSON (Including nested Domain Classes)
 *
 * @author Siegfried Puchbauer
 * @deprecated The Converters framework has been refactored and this made the deep Converters obsolete
 */
public class JSON extends grails.converters.JSON{

    public static final String CONFIGURATION_NAME = "deep";

    protected ConverterConfiguration<grails.converters.JSON> initConfig() {
        return ConvertersConfigurationHolder.getNamedConverterConfiguration(CONFIGURATION_NAME, grails.converters.JSON.class);
    }
}
