/* Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.web.util;

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.encoding.CodecLookup
import org.codehaus.groovy.grails.support.encoding.Encoder
import org.codehaus.groovy.grails.web.pages.GroovyPageConfig
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStackAttributes
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Helper methods for {@link #withCodec} feature 
 *
 * @author Lari Hotari
 * @since 2.3
 */
@CompileStatic
public class WithCodecHelper {
    private static final Logger log = LoggerFactory.getLogger(WithCodecHelper)
    
    /** all is the key to set all codecs at once */
    public static String ALL_CODECS_FALLBACK_KEY_NAME="all"
    /** name is the key to set out and expression codecs at once */
    public static String OUT_AND_EXPRESSION_CODECS_FALLBACK_KEY_NAME="name"
    
    public static String INHERIT_KEY_NAME="inherit"


    /**
     * Executes closure with given codecs
     * 
     * codecInfo parameter can be a single String value or a java.util.Map.
     * When it's a single String value, "out", "expression" and "taglib" get set with the given codec 
     * When it's a java.util.Map, these keys get used:
     * <ul>
     * <li>out - escapes output from scriptlets to output (the codec attached to "out" writer instance in GSP scriptlets)</li>
     * <li>taglib - escapes output from taglibs to output (the codec attached to "out" writer instance in taglibs)</li>
     * <li>expression - escapes values inside ${} to output</li>
     * <li>static - escapes the static html parts coming from the GSP file to output</li>
     * </ul>
     * These keys set several codecs at once:
     * <ul>
     * <li>all - sets out, taglib, expression and static</li>
     * <li>name - sets out, taglib and expression</li>
     * </ul>
     * In addition there is
     * <ul>
     * <li>inherit (boolean) - defaults to true. Control whether codecs should be inherited to deeper level (taglib calls)</li> 
     * </ul>
     *
     * @param grailsApplication the grailsApplication instance
     * @param codecInfo this parameter is explained above
     * @param closure the closure to execute
     * @return the return value of the closure
     */
    public static withCodec(GrailsApplication grailsApplication, Object codecInfo, Closure closure) {
        GroovyPageOutputStack outputStack=GroovyPageOutputStack.currentStack();
        try {
            outputStack.push(createOutputStackAttributesBuilder(codecInfo, grailsApplication).build(), false);
            return closure.call();
        } finally {
            outputStack.pop();
        }
    }

    /**
     * Creates a builder for building a new {@link GroovyPageOutputStackAttributes} instance
     *
     * @param codecInfo the codec info, see {@link #withCodec} method for more info 
     * @param grailsApplication the grails application
     * @return the builder instance for building {@link GroovyPageOutputStackAttributes} instance
     */
    public static org.codehaus.groovy.grails.web.pages.GroovyPageOutputStackAttributes.Builder createOutputStackAttributesBuilder(Object codecInfo, GrailsApplication grailsApplication) {
        GroovyPageOutputStackAttributes.Builder builder=new GroovyPageOutputStackAttributes.Builder()
        builder.inheritPreviousEncoders(true)
        if(codecInfo != null) {
            if(codecInfo instanceof Map) {
                Map<String, Encoder> encoders = [:]
                
                Map<String, String> codecInfoMap = (Map<String,String>)((Map)codecInfo).collectEntries { k, v ->
                    String codecWriterName = k.toString() - 'Codec'
                    String codecName=v?.toString() ?: 'none'
                    if(!encoders.containsKey(codecName)) {
                        encoders[codecName] = lookupEncoder(grailsApplication, codecName)
                    }
                    [codecWriterName, codecName]
                }

                def outEncoderName = codecInfoMap[(GroovyPageConfig.OUT_CODEC_NAME)] ?: codecInfoMap[(OUT_AND_EXPRESSION_CODECS_FALLBACK_KEY_NAME)] ?: codecInfoMap[(ALL_CODECS_FALLBACK_KEY_NAME)]
                builder.outEncoder(lookupEncoderFromMap(encoders, outEncoderName))
                
                def taglibEncoderName = codecInfoMap[(GroovyPageConfig.TAGLIB_CODEC_NAME)] ?: codecInfoMap[(OUT_AND_EXPRESSION_CODECS_FALLBACK_KEY_NAME)] ?: codecInfoMap[(ALL_CODECS_FALLBACK_KEY_NAME)]
                builder.taglibEncoder(lookupEncoderFromMap(encoders, taglibEncoderName))
                
                def defaultEncoderName = codecInfoMap[(GroovyPageConfig.EXPRESSION_CODEC_NAME)] ?: codecInfoMap[(OUT_AND_EXPRESSION_CODECS_FALLBACK_KEY_NAME)] ?: codecInfoMap[(ALL_CODECS_FALLBACK_KEY_NAME)]
                builder.expressionEncoder(lookupEncoderFromMap(encoders, defaultEncoderName))
                
                def staticEncoderName = codecInfoMap[(GroovyPageConfig.STATIC_CODEC_NAME)] ?: codecInfoMap[(ALL_CODECS_FALLBACK_KEY_NAME)]
                builder.staticEncoder(lookupEncoderFromMap(encoders, staticEncoderName))
                
                if(codecInfoMap.containsKey(INHERIT_KEY_NAME)) {
                    Object inheritVal = codecInfoMap.get(INHERIT_KEY_NAME)
                    boolean inheritPrevious = inheritVal as boolean
                    if(inheritPrevious && inheritVal instanceof CharSequence && inheritVal.toString()=="false") {
                        inheritPrevious = false
                    }
                    builder.inheritPreviousEncoders(inheritPrevious)
                }
            } else {
                Encoder encoder = lookupEncoder(grailsApplication, codecInfo.toString())
                builder.outEncoder(encoder).expressionEncoder(encoder).taglibEncoder(encoder);
            }
        }
        return builder
    }

    private static Encoder lookupEncoderFromMap(Map<String, Encoder> encoders, String codecName) {
        codecName != null ? encoders[codecName] : null
    }

    /**
     * Lookup encoder.
     *
     * @param grailsApplication the grailsApplication instance
     * @param codecName the codec name
     * @return the encoder instance
     */
    public static Encoder lookupEncoder(GrailsApplication grailsApplication, String codecName) {
        try {
            CodecLookup codecLookup = grailsApplication.getMainContext().getBean("codecLookup", CodecLookup.class);
            return codecLookup.lookupEncoder(codecName);
        } catch (NullPointerException e) {
            // ignore NPE for encoder lookups
            log.debug("NPE in lookupEncoder, grailsApplication.mainContext is null or codecLookup bean is missing from test context.", e)
        }
    }
}
