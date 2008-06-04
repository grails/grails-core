package org.codehaus.groovy.grails.support;

import org.springframework.core.io.ByteArrayResource;

import java.net.URL;
import java.io.IOException;

/**
 * Hacky version of the ByteArrayResource that implements the {@link
 * #getURL()} method, required for the resource to work with Spring's
 * ServletContextResource.
 * @author pledbrook
 */
public class GrailsByteArrayResource extends ByteArrayResource {
    public GrailsByteArrayResource(byte[] byteArray) {
        super(byteArray);
    }

    public GrailsByteArrayResource(byte[] byteArray, String location) {
        super(byteArray, location);
    }

    /**
     * Overrides the default behaviour to generate a fake "file:" URL
     * so that the resource can be used from a ServletContextResource.
     */
    public URL getURL() throws IOException {
        return new URL("file", null, getDescription());
    }
}
