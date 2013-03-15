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
                
    			def pageEncoderName = codecInfoMap.pageCodec ?: codecInfoMap.name ?: codecInfoMap.all
    			builder.pageEncoder(lookupEncoderFromMap(encoders, pageEncoderName?.toString()))
    			def defaultEncoderName = codecInfoMap.defaultCodec ?: codecInfoMap.name ?: codecInfoMap.all
    			builder.defaultEncoder(lookupEncoderFromMap(encoders, defaultEncoderName?.toString()))
                def templateEncoderName = codecInfoMap.templateCodec ?: codecInfoMap.all                
    			builder.templateEncoder(lookupEncoderFromMap(encoders, templateEncoderName?.toString()))
    		} else {
    			Encoder encoder = lookupEncoder(grailsApplication, codecInfo.toString())
    			builder.pageEncoder(encoder).defaultEncoder(encoder)
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
