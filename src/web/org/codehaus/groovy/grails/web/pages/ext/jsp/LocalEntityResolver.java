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
package org.codehaus.groovy.grails.web.pages.ext.jsp;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves commons JSP DTDs and Schema definitions locally
 * 
 * @author Graeme Rocher
 */
public class LocalEntityResolver implements EntityResolver{

    private static final Map ENTITIES = new HashMap() {{
        // JSP taglib 2.1
        put("http://java.sun.com/xml/ns/jee/web-jsptaglibrary_2_1.xsd", "web-jsptaglibrary_2_1.xsd");
        // JSP taglib 2.0
        put("http://java.sun.com/xml/ns/j2ee/web-jsptaglibrary_2_0.xsd", "web-jsptaglibrary_2_0.xsd");
        // JSP taglib 1.2
        put("-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN", "web-jsptaglibrary_1_2.dtd");
        put("http://java.sun.com/dtd/web-jsptaglibrary_1_2.dtd", "web-jsptaglibrary_1_2.dtd");
        // JSP taglib 1.1
        put("-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN", "web-jsptaglibrary_1_1.dtd");
        put("http://java.sun.com/j2ee/dtds/web-jsptaglibrary_1_1.dtd", "web-jsptaglibrary_1_1.dtd");
        // Servlet 2.5
        put("http://java.sun.com/xml/ns/jee/web-app_2_5.xsd", "web-app_2_5.xsd");
        // Servlet 2.4
        put("http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd", "web-app_2_4.xsd");
        // Servlet 2.3
        put("-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN", "web-app_2_3.dtd");
        put("http://java.sun.com/dtd/web-app_2_3.dtd", "web-app_2_3.dtd");
        // Servlet 2.2
        put("-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN", "web-app_2_2.dtd");
        put("http://java.sun.com/j2ee/dtds/web-app_2_2.dtd", "web-app_2_2.dtd");
    }};

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        String name = (String)ENTITIES.get(publicId);

        if(name == null) name = (String)ENTITIES.get(systemId);

        InputStream stream = name != null ? getClass().getResourceAsStream(name)
                                          : new ByteArrayInputStream(new byte[0]);

        InputSource is = new InputSource();
        is.setByteStream(stream);
        is.setPublicId(publicId);
        is.setSystemId(systemId);
        return is;
    }
}
