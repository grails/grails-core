package org.codehaus.groovy.grails.commons
/**
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Jan 9, 2009
 */

public class DefaultGrailsCodecClassTests extends GroovyTestCase {

    protected void setUp() {
        ExpandoMetaClass.enableGlobally()
    }

    protected void tearDown() {
        ExpandoMetaClass.disableGlobally()
    }






    void testCodecWithClosures() {

         def codecClass = new DefaultGrailsCodecClass(CodecWithClosuresCodec.class)
        
        assertEquals "encoded", codecClass.getEncodeMethod().call("stuff")
        assertEquals "decoded", codecClass.getDecodeMethod().call("stuff")
    }

    void testCodecWithMethods() {
        def codecClass = new DefaultGrailsCodecClass(CodecWithMethodsCodec)

       assertEquals "encoded", codecClass.encodeMethod.call("stuff")
       assertEquals "decoded", codecClass.decodeMethod.call("stuff")

    }
}
class CodecWithClosuresCodec {
    def encode = {
        "encoded"
    }
    def decode = {
        "decoded"
    }
}
class CodecWithMethodsCodec {
    def encode(obj) {
        "encoded"
    }
    def decode(obj) {
        "decoded"        
    }
}