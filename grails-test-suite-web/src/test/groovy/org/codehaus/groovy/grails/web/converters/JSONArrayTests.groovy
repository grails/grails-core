package org.codehaus.groovy.grails.web.converters

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 7/8/11
 * Time: 1:18 PM
 * To change this template use File | Settings | File Templates.
 */
import static org.junit.Assert.*

import org.codehaus.groovy.grails.web.json.JSONArray
import org.junit.Test

class JSONArrayTests {

    @Test
    public void testEquals() {
        assertEquals(getJSONArray(), getJSONArray())
    }

    @Test
    public void testHashCode() {
        assertEquals(getJSONArray().hashCode(), getJSONArray().hashCode())
    }

    def getJSONArray() {
        return new JSONArray(['a', 'b', 'c'])
    }
}