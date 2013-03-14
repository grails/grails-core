package org.codehaus.groovy.grails.web.util;

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
    public static Closure<?> createWithCodecClosure(final GrailsApplication grailsApplication) {
        return { Object codecInfo, Closure<?> closure ->
            GroovyPageOutputStack outputStack=GroovyPageOutputStack.currentStack();
            try {
                outputStack.push(createOutputStackAttributesBuilder(codecInfo, grailsApplication).build(), false);
                return closure.call();
            } finally {
                outputStack.pop();
            }
        }
    }

	public static org.codehaus.groovy.grails.web.pages.GroovyPageOutputStackAttributes.Builder createOutputStackAttributesBuilder(Object codecInfo, GrailsApplication grailsApplication) {
		GroovyPageOutputStackAttributes.Builder builder=new GroovyPageOutputStackAttributes.Builder()
		builder.inheritPreviousEncoders(true)
        if(codecInfo != null) {
    		if(codecInfo instanceof Map) {
    			Map codecInfoMap = (Map)codecInfo
    			def pageEncoderName = codecInfoMap.pageCodec ?: codecInfoMap.name
    			builder.pageEncoder(lookupEncoder(grailsApplication, pageEncoderName?.toString()))
    			def defaultEncoderName = codecInfoMap.defaultCodec ?: codecInfoMap.name
    			builder.defaultEncoder(lookupEncoder(grailsApplication, defaultEncoderName?.toString()))
    			builder.templateEncoder(lookupEncoder(grailsApplication, codecInfoMap.templateCodec?.toString()))
    		} else {
    			Encoder encoder = lookupEncoder(grailsApplication, codecInfo.toString())
    			builder.pageEncoder(encoder).defaultEncoder(encoder)
    		}
        }
		return builder
	}

    public static Encoder lookupEncoder(GrailsApplication grailsApplication, String codecName) {
        GrailsCodecClass codecArtefact = null;
        if(codecName != null && codecName.length() > 0 && !"none".equalsIgnoreCase(codecName)) {
            if(codecName.equalsIgnoreCase("html")) {
                codecName="HTML";
            } else if (codecName.equalsIgnoreCase("html4")) {
                codecName="HTML4";
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
