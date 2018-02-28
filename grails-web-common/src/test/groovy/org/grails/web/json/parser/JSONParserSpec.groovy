package org.grails.web.json.parser

import groovy.transform.CompileStatic
import org.grails.web.json.JSONObject
import spock.lang.Specification

import java.nio.charset.StandardCharsets

@CompileStatic
class JSONParserSpec extends Specification {

    void "Test JSONParser.parseJSON() for long array"() {
        given: "JSONParser with input stream containing large array data"
        def largeArray = generateByteArray(15000)
        def inputStream = getJsonObjectInputStream(largeArray)
        JSONParser jsonParser = new JSONParser(inputStream)
        def expectedArray = largeArray

        when: "parsing object with long array"
        JSONObject jsonElement = jsonParser.parseJSON() as JSONObject

        then: "data is parsed as expected"
        jsonElement
        byte[] actualArray = jsonElement.get('array') as byte[]
        expectedArray == actualArray
    }

    private static InputStream getJsonObjectInputStream(byte[] array) {
        String arrayObjectStr = "{\"array\": ${array}}"
        return new ByteArrayInputStream(arrayObjectStr.getBytes(StandardCharsets.UTF_8))
    }

    private static byte[] generateByteArray(int length) {
        byte[] byteArray = new byte[length]
        new Random().nextBytes(byteArray)
        byteArray
    }
}
