package org.codehaus.groovy.grails.web.util;

import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked

import org.codehaus.groovy.grails.commons.CodecArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsCodecClass
import org.codehaus.groovy.grails.support.encoding.Encoder
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStackAttributes

@CompileStatic
public class WithCodecHelper {
    /**  escapes the static html parts coming from the GSP file to output */
    public static String OUT_CODEC_NAME="outCodec"
    /** escapes values inside ${} to output */
    public static String EXPRESSION_CODEC_NAME="expressionCodec"
    public static String EXPRESSION_CODEC_NAME_ALIAS="defaultCodec"
    /**  escapes the static html parts coming from the GSP file to output */
    public static String TEMPLATE_CODEC_NAME="templateCodec"
    
    /** key to set all codecs at once */
    public static String ALL_CODECS_FALLBACK_KEY_NAME="all"
    /** key to set out and expression codecs at once */
    public static String OUT_AND_EXPRESSION_CODECS_FALLBACK_KEY_NAME="name"
    
    
	public static withCodec(GrailsApplication grailsApplication, Object codecInfo, Closure closure) {
		GroovyPageOutputStack outputStack=GroovyPageOutputStack.currentStack();
		try {
			outputStack.push(createOutputStackAttributesBuilder(codecInfo, grailsApplication).build(), false);
			return closure.call();
		} finally {
			outputStack.pop();
		}
	}

	public static org.codehaus.groovy.grails.web.pages.GroovyPageOutputStackAttributes.Builder createOutputStackAttributesBuilder(Object codecInfo, GrailsApplication grailsApplication) {
		GroovyPageOutputStackAttributes.Builder builder=new GroovyPageOutputStackAttributes.Builder()
		builder.inheritPreviousEncoders(true)
        if(codecInfo != null) {
    		if(codecInfo instanceof Map) {
    			Map codecInfoMap = (Map)codecInfo
                Map<String, Encoder> encoders = [:]
                
                codecInfoMap.each { k, v ->
                    String codecName=v.toString()
                    if(!encoders.containsKey(codecName)) {
                        encoders[codecName] = lookupEncoder(grailsApplication, codecName)
                    }        
                }
                
    			def outEncoderName = codecInfoMap[(OUT_CODEC_NAME)] ?: codecInfoMap[(OUT_AND_EXPRESSION_CODECS_FALLBACK_KEY_NAME)] ?: codecInfoMap[(ALL_CODECS_FALLBACK_KEY_NAME)]
    			builder.outEncoder(lookupEncoderFromMap(encoders, outEncoderName?.toString()))
    			def defaultEncoderName = codecInfoMap[(EXPRESSION_CODEC_NAME)] ?: codecInfoMap[(EXPRESSION_CODEC_NAME_ALIAS)] ?: codecInfoMap[(OUT_AND_EXPRESSION_CODECS_FALLBACK_KEY_NAME)] ?: codecInfoMap[(ALL_CODECS_FALLBACK_KEY_NAME)]
    			builder.expressionEncoder(lookupEncoderFromMap(encoders, defaultEncoderName?.toString()))
                def templateEncoderName = codecInfoMap[(TEMPLATE_CODEC_NAME)] ?: codecInfoMap[(ALL_CODECS_FALLBACK_KEY_NAME)]                
    			builder.templateEncoder(lookupEncoderFromMap(encoders, templateEncoderName?.toString()))
    		} else {
    			Encoder encoder = lookupEncoder(grailsApplication, codecInfo.toString())
    			builder.outEncoder(encoder).expressionEncoder(encoder)
    		}
        }
		return builder
	}
    
    private static Encoder lookupEncoderFromMap(Map<String, Encoder> encoders, String codecName) {
        codecName != null ? encoders[codecName] : null
    }

    public static Encoder lookupEncoder(GrailsApplication grailsApplication, String codecName) {
        GrailsCodecClass codecArtefact = null;
        if(codecName != null && codecName.length() > 0 && !"none".equalsIgnoreCase(codecName)) {
            if(codecName.equalsIgnoreCase("html")) {
                codecName="HTML";
            } else if (codecName.equalsIgnoreCase("html4")) {
                codecName="HTML4";
            } else {
                codecName=GrailsNameUtils.getPropertyNameRepresentation(codecName)
            }
            codecArtefact = (GrailsCodecClass) grailsApplication.getArtefactByLogicalPropertyName(CodecArtefactHandler.TYPE, codecName);
            if(codecArtefact==null) {
                codecArtefact = (GrailsCodecClass) grailsApplication.getArtefactByLogicalPropertyName(CodecArtefactHandler.TYPE, codecName.toUpperCase());
            }
        }
        Encoder encoder = codecArtefact != null ? codecArtefact.getEncoder() : null;
        return encoder;
    }    
}
