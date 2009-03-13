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
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable ConverterConfiguration which chains the lookup calls for ObjectMarshallers for performance reasons
 *
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class ChainedConverterConfiguration<C extends Converter> implements ConverterConfiguration<C> {

    private List<ObjectMarshaller<C>> marshallerList;

    private ChainedObjectMarshaller<C> root;

    private final String encoding;

    private final Converter.CircularReferenceBehaviour circularReferenceBehaviour;

    private final boolean prettyPrint;

    public ChainedConverterConfiguration(ConverterConfiguration<C> cfg) {
        this.marshallerList = cfg.getOrderedObjectMarshallers();

        encoding = cfg.getEncoding();
        prettyPrint = cfg.isPrettyPrint();
        circularReferenceBehaviour = cfg.getCircularReferenceBehaviour();

        List<ObjectMarshaller<C>> oms = new ArrayList<ObjectMarshaller<C>>(marshallerList);
        Collections.reverse(oms);
        ChainedObjectMarshaller<C> prev = null;
        for(ObjectMarshaller<C> om : oms) {
            prev = new ChainedObjectMarshaller<C>(om, prev);
        }
        root = prev;
    }

    public ObjectMarshaller<C> getMarshaller(Object o) {
        return root.findMarhallerFor(o);
    }

    public String getEncoding() {
        return encoding;
    }

    public Converter.CircularReferenceBehaviour getCircularReferenceBehaviour() {
        return circularReferenceBehaviour;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public List<ObjectMarshaller<C>> getOrderedObjectMarshallers() {
        return marshallerList;
    }

    public class ChainedObjectMarshaller<C extends Converter> implements ObjectMarshaller<C> {

        private ObjectMarshaller<C> om;

        private ChainedObjectMarshaller<C> next;

        public ChainedObjectMarshaller(ObjectMarshaller<C> om, ChainedObjectMarshaller<C> next) {
            this.om = om;
            this.next = next;
        }

        public ObjectMarshaller<C> findMarhallerFor(Object o) {
            if(supports(o)){
                return this.om;
            } else {
                return next != null ? next.findMarhallerFor(o) : null;
            }
        }

        public boolean supports(Object object) {
            return om.supports(object);
        }

        public void marshalObject(Object object, C converter) throws ConverterException {
            om.marshalObject(object, converter);
        }

    }
}
