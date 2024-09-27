/*
 * Copyright 2013 the original author or authors.
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
package org.grails.plugins.codecs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.grails.commons.CodecArtefactHandler;
import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import org.grails.commons.GrailsCodecClass;
import grails.core.support.GrailsApplicationAware;
import org.grails.encoder.impl.BasicCodecLookup;
import org.springframework.context.ApplicationContext;
import org.springframework.core.OrderComparator;

/**
 * @author Lari Hotari
 * @since 2.3
 */
public class DefaultCodecLookup extends BasicCodecLookup implements GrailsApplicationAware {
    protected ApplicationContext applicationContext;
    protected GrailsApplication grailsApplication;

    public DefaultCodecLookup(GrailsApplication grailsApplication) {
        Objects.requireNonNull(grailsApplication);
        this.grailsApplication = grailsApplication;
        this.applicationContext = grailsApplication.getMainContext();
    }

    public DefaultCodecLookup() {
    }

    protected void registerCodecs() {
        List<GrailsClass> codecs = Arrays.asList(grailsApplication.getArtefacts(CodecArtefactHandler.TYPE));
        Collections.sort(codecs, OrderComparator.INSTANCE);
        Collections.reverse(codecs);
        for (GrailsClass grailsClass : codecs) {
            registerCodec((GrailsCodecClass)grailsClass);
        }
    }

    public void registerCodec(GrailsCodecClass grailsClass) {
        grailsClass.configureCodecMethods();
        registerCodecFactory(grailsClass);
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        Objects.requireNonNull(grailsApplication);
        this.grailsApplication = grailsApplication;
        this.applicationContext = grailsApplication.getMainContext();
    }
}
