/*
 * Copyright 2004-2008 the original author or authors.
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
package org.codehaus.groovy.grails.web.converters.configuration;

import org.codehaus.groovy.grails.web.converters.Converter;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;

import java.util.Collections;
import java.util.List;

/**
 * Immutable Converter Configuration
 *
 * @author Siegfried Puchbauer
 * @see org.codehaus.groovy.grails.web.converters.configuration.ChainedConverterConfiguration
 */
public class ImmutableConverterConfiguration<C extends Converter> implements ConverterConfiguration<C> {

    protected final List<ObjectMarshaller<C>> marshallers;

    private final String encoding;

    private final Converter.CircularReferenceBehaviour circularReferenceBehaviour;

    private final boolean prettyPrint;

    public ImmutableConverterConfiguration(ConverterConfiguration<C> cfg) {
        marshallers = Collections.unmodifiableList(cfg.getOrderedObjectMarshallers());
        encoding = cfg.getEncoding();
        prettyPrint = cfg.isPrettyPrint();
        circularReferenceBehaviour = cfg.getCircularReferenceBehaviour();
    }

    /**
     * @see ConverterConfiguration#getMarshaller(Object) 
     */
    public ObjectMarshaller<C> getMarshaller(Object o) {
        for(ObjectMarshaller<C> om : marshallers) {
            if(om.supports(o)) {
                return om;
            }
        }
        return null;
    }

    /**
     * @see ConverterConfiguration#getEncoding()
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @see ConverterConfiguration#getCircularReferenceBehaviour()
     */
    public Converter.CircularReferenceBehaviour getCircularReferenceBehaviour() {
        return circularReferenceBehaviour;
    }

    /**
     * @see ConverterConfiguration#isPrettyPrint()
     */
    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public List<ObjectMarshaller<C>> getOrderedObjectMarshallers() {
        return marshallers;
    }
}
