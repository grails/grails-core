/*
* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class CodecArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Codec";


    public CodecArtefactHandler() {
        super(TYPE, GrailsCodecClass.class, DefaultGrailsCodecClass.class, null);
    }

    public boolean isArtefactClass( Class clazz ) {
        if(clazz == null) return false;
        if(!clazz.getName().endsWith(DefaultGrailsCodecClass.CODEC)) {
            return false;
        }
        // @todo Is this really needed, after all the convention is there who cares if missing props?
        Object encodeMethod = GrailsClassUtils.getStaticPropertyValue(clazz, "encode");
        Object decodeMethod = GrailsClassUtils.getStaticPropertyValue(clazz, "decode");

        return encodeMethod != null || decodeMethod != null;
    }
}
