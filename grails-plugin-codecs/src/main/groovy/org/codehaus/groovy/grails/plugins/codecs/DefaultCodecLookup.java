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
package org.codehaus.groovy.grails.plugins.codecs;

import grails.util.GrailsNameUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.grails.commons.CodecArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsCodecClass;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.support.encoding.CodecIdentifierProvider;
import org.codehaus.groovy.grails.support.encoding.CodecLookup;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.DefaultEncodingStateRegistry;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.OrderComparator;

/**
 * @author Lari Hotari
 * @since 2.3
 */
public class DefaultCodecLookup implements GrailsApplicationAware, InitializingBean, CodecLookup {
    private static final String NONE_CODEC_NAME = "none";
    protected ApplicationContext applicationContext;
    protected GrailsApplication grailsApplication;
    protected Map<String, Encoder> encoders;
    protected Map<String, Decoder> decoders;
    public static final Encoder NONE_ENCODER = new NoneEncoder();
    static {
        DefaultEncodingStateRegistry.NONE_ENCODER = NONE_ENCODER;
    }

    public void afterPropertiesSet() throws Exception {
        registerCodecs();
    }

    public void reInitialize() {
        registerCodecs();
    }

    public Encoder lookupEncoder(String codecName) {
        return lookupCodec(codecName, encoders, Encoder.class);
    }

    public Decoder lookupDecoder(String codecName) {
        return lookupCodec(codecName, decoders, Decoder.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T lookupCodec(String codecName, Map<String, T> map, Class<T> returnType) {
        if (codecName != null && codecName.length() > 0) {
            if (NONE_CODEC_NAME.equalsIgnoreCase(codecName)) {
                if (returnType == Encoder.class) {
                    return (T)NONE_ENCODER;
                }
            } else {
                return map.get(codecName);
            }
        }
        return null;
    }

    protected void registerCodecs() {
        List<GrailsClass> codecs = Arrays.asList(grailsApplication.getArtefacts(CodecArtefactHandler.TYPE));
        Collections.sort(codecs, OrderComparator.INSTANCE);
        Collections.reverse(codecs);

        encoders = new HashMap<String, Encoder>();
        decoders = new HashMap<String, Decoder>();

        for (GrailsClass grailsClass : codecs) {
            registerCodec((GrailsCodecClass)grailsClass);
        }
    }

    private void registerCodec(GrailsCodecClass grailsClass) {
        grailsClass.configureCodecMethods();
        Encoder encoder=grailsClass.getEncoder();
        if (encoder != null) {
            registerWithNameVaritions(encoders, encoder);
        }
        Decoder decoder=grailsClass.getDecoder();
        if (decoder != null) {
            registerWithNameVaritions(decoders, decoder);
        }
    }

    private <T extends CodecIdentifierProvider> void registerWithNameVaritions(Map<String, T> destinationMap, T target) {
        String name=target.getCodecIdentifier().getCodecName();
        registerVariationsOfName(destinationMap, target, name);
        Set<String> aliases = target.getCodecIdentifier().getCodecAliases();
        if (aliases != null)  {
            for (String alias : aliases) {
                registerVariationsOfName(destinationMap, target, alias);
            }
        }
    }

    private <T extends CodecIdentifierProvider> void registerVariationsOfName(Map<String, T> destinationMap, T target,
            String name) {
        destinationMap.put(name, target);
        destinationMap.put(name.toLowerCase(), target);
        destinationMap.put(name.toUpperCase(), target);
        destinationMap.put(GrailsNameUtils.getPropertyNameRepresentation(name), target);
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
}
