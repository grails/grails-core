/* Copyright 2006-2007 Graeme Rocher
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
package grails.converters.deep;

import org.codehaus.groovy.grails.web.converters.Converter;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A converter that converts domain classes, Maps, Lists, Arrays, POJOs and POGOs
 * to JSON (Including nested Domain Classes)
 *
 * @author Siegfried Puchbauer
 */
public class JSON extends grails.converters.JSON implements Converter {

    private Stack stack = new Stack();

    protected void bean(Object o) throws ConverterException {
        if (stack.contains(o)) {
            //value(new HashMap());//throw new ConverterException("Circular Relationship detected!");
            handleCircularRelationship(o);
            return;
        }
        stack.push(o);
        super.bean(o);
        stack.pop();
    }

    protected void domain(Object o) throws ConverterException {
        if (stack.contains(o)) {
            //value(new HashMap());//throw new ConverterException("Circular Relationship detected!");
            handleCircularRelationship(o);
            return;
        }
        stack.push(o);
        super.domain(o);
        stack.pop();
    }

    protected void handleCircularRelationship(Object o) throws ConverterException {
        Map props = new HashMap();
        props.put("class", o.getClass());
        StringBuffer ref = new StringBuffer();
        int idx = stack.indexOf(o);
        for (int i = stack.size(); i > idx; i--) {
            ref.append("../");
        }
        props.put("_ref", ref.substring(0, ref.length() - 1));
        value(props);
    }

    /**
     * internal Getter
     *
     * @return true
     */
    public boolean isRenderDomainClassRelations() {
        return true;
    }
}
