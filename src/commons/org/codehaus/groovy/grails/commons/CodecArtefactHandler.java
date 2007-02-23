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
