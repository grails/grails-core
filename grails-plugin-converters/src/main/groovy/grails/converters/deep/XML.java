/*
 * Copyright 2006-2007 Graeme Rocher
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
 * A converter that converts domain classes to XML (including nested Domain Classes)
 *
 * @author Siegfried Puchbauer
 * @deprecated The Converters framework has been refactored and this made the deep Converters obsolete
 */
@Deprecated
public class XML extends grails.converters.XML {

    public static final String CONFIGURATION_NAME = "deep";

    @Override
    protected ConverterConfiguration<grails.converters.XML> initConfig() {
        return ConvertersConfigurationHolder.getNamedConverterConfiguration(
                CONFIGURATION_NAME, grails.converters.XML.class);
    }
}
