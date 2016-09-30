package org.grails.web.databinding.bindingsource

import grails.web.mime.MimeType
import groovy.transform.CompileStatic

/**
 * Creates DataBindingSource objects from JSON API in the request body
 *
 * @since 3.2.1
 *
 * @author James Kleeh
 *
 * @see grails.databinding.DataBindingSource
 * @see DataBindingSourceCreator
 */
@CompileStatic
class JsonApiDataBindingSourceCreator extends JsonDataBindingSourceCreator {

    protected static final String DATA = "data"
    protected static final String RELATIONSHIPS = "relationships"
    protected static final String ID = "id"
    protected static final String ATTRIBUTES = "attributes"

    @Override
    MimeType[] getMimeTypes() {
        [MimeType.JSON_API] as MimeType[]
    }

    @Override
    protected Map createJsonMap(Object jsonElement) {
        if(jsonElement instanceof Map) {
            def jsonMap = (Map) jsonElement
            if(jsonMap.containsKey(DATA)) {
                jsonMap = new LinkedHashMap(jsonMap)
                def data = jsonMap.get(DATA)
                if (data instanceof Map) {
                    if (data.containsKey(ID)) {
                        jsonMap.put(ID, data.get(ID))
                    }
                    if (data.containsKey(ATTRIBUTES)) {
                        jsonMap.putAll((Map)data.get(ATTRIBUTES))
                    }
                    if (data.containsKey(RELATIONSHIPS)) {
                        Map relationships = (Map)data.get(RELATIONSHIPS)
                        relationships.each { key, val ->
                            if (val instanceof Map && ((Map)val).containsKey(DATA)) {
                                def rData = ((Map)val).get(DATA)
                                if (rData instanceof Map) {
                                    jsonMap.put(key, rData.get(ID))
                                } else if (rData instanceof List) {
                                    jsonMap.put(key, rData.collect { d -> d[ID] })
                                }
                            }
                        }
                    }
                }
            }
            return jsonMap
        }
        else {
            return super.createJsonMap(jsonElement)
        }
    }

}