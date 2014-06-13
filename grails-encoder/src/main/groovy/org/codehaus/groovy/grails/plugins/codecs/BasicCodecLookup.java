/* Copyright 2014 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.grails.support.encoding.CodecFactory;
import org.codehaus.groovy.grails.support.encoding.CodecIdentifierProvider;
import org.codehaus.groovy.grails.support.encoding.CodecLookup;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;
import org.springframework.beans.factory.InitializingBean;

public class BasicCodecLookup implements CodecLookup, InitializingBean {
    private static final String NONE_CODEC_NAME = "none";
    protected Map<String, Encoder> encoders;
    protected Map<String, Decoder> decoders;
    public static final StreamingEncoder NONE_ENCODER = new NoneEncoder();

    public BasicCodecLookup() {
        super();
    }

    public Encoder lookupEncoder(String codecName) {
        return lookupCodec(codecName, encoders, Encoder.class);
    }
    
    public Decoder lookupDecoder(String codecName) {
        return lookupCodec(codecName, decoders, Decoder.class);
    }    

    @SuppressWarnings("unchecked")
    protected <T> T lookupCodec(String codecName, Map<String, T> map, Class<T> returnType) {
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

    protected <T extends CodecIdentifierProvider> void registerWithNameVaritions(Map<String, T> destinationMap, T target) {
        String name=target.getCodecIdentifier().getCodecName();
        registerVariationsOfName(destinationMap, target, name);
        Set<String> aliases = target.getCodecIdentifier().getCodecAliases();
        if (aliases != null)  {
            for (String alias : aliases) {
                registerVariationsOfName(destinationMap, target, alias);
            }
        }
    }

    protected <T extends CodecIdentifierProvider> void registerVariationsOfName(Map<String, T> destinationMap, T target, String name) {
        Collection<String> nameVariations = createNameVariations(name, target);
        for(String nameVariation : nameVariations) {
            destinationMap.put(nameVariation, target);
        }
    }
    
    protected Collection<String> createNameVariations(String name, CodecIdentifierProvider target) {
        Set<String> nameVariations = new LinkedHashSet<String>();
        nameVariations.add(name);
        nameVariations.add(name.toLowerCase());
        nameVariations.add(name.toUpperCase());
        nameVariations.add(GrailsNameUtils.getPropertyNameRepresentation(name));
        return nameVariations;
    }

    public void registerCodecFactory(CodecFactory codecFactory) {
        Encoder encoder=codecFactory.getEncoder();
        if (encoder != null) {
            registerEncoder(encoder);
        }
        Decoder decoder=codecFactory.getDecoder();
        if (decoder != null) {
            registerDecoder(decoder);
        }
    }

    public void registerDecoder(Decoder decoder) {
        registerWithNameVaritions(decoders, decoder);
    }

    public void registerEncoder(Encoder encoder) {
        registerWithNameVaritions(encoders, encoder);
    }

    public void reInitialize() {
        encoders = new HashMap<String, Encoder>();
        decoders = new HashMap<String, Decoder>();
        registerCodecs();
    }
    
    protected void registerCodecs() {
        
    }

    public void afterPropertiesSet() throws Exception {
        reInitialize();
    }
}
