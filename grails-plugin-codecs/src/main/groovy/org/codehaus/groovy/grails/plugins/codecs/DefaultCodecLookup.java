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
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.OrderComparator;

public class DefaultCodecLookup implements GrailsApplicationAware, InitializingBean, CodecLookup {
    private static final String NONE_CODEC_NAME = "none";
    protected ApplicationContext applicationContext;
    protected GrailsApplication grailsApplication;
    protected Map<String, Encoder> encoders;
    protected Map<String, Decoder> decoders;

    public void afterPropertiesSet() throws Exception {
        registerCodecs();
    }

    public void reInitialize() {
        registerCodecs();
    }
    
    public Encoder lookupEncoder(String codecName) {
        return lookupCodec(codecName, encoders);
    }

    public Decoder lookupDecoder(String codecName) {
        return lookupCodec(codecName, decoders);
    }

    private <T> T lookupCodec(String codecName, Map<String, T> map) {
        if(codecName != null && codecName.length() > 0 && !NONE_CODEC_NAME.equalsIgnoreCase(codecName)) {
            return map.get(codecName);
        } else {
            return null;
        }
    }
    
    protected void registerCodecs() {
        List<GrailsClass> codecs = Arrays.asList(grailsApplication.getArtefacts(CodecArtefactHandler.TYPE));
        Collections.sort(codecs, OrderComparator.INSTANCE);
        Collections.reverse(codecs);
        
        encoders=new HashMap<String, Encoder>();
        decoders=new HashMap<String, Decoder>();
        
        for (GrailsClass grailsClass : codecs) {
            registerCodec((GrailsCodecClass)grailsClass);
        }
    }
    
    private void registerCodec(GrailsCodecClass grailsClass) {
        grailsClass.configureCodecMethods();
        Encoder encoder=grailsClass.getEncoder();
        if(encoder != null) {
            registerWithNameVaritions(encoders, encoder);
        }
        Decoder decoder=grailsClass.getDecoder();
        if(decoder != null) {
            registerWithNameVaritions(decoders, decoder);
        }
    }

    private <T extends CodecIdentifierProvider> void registerWithNameVaritions(Map<String, T> destinationMap, T target) {
        String name=target.getCodecIdentifier().getCodecName();
        registerVariationsOfName(destinationMap, target, name);
        Set<String> aliases = target.getCodecIdentifier().getCodecAliases();
        if(aliases != null)  {
            for(String alias : aliases) {
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
