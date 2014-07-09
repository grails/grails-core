package org.grails.core.io

import groovy.transform.CompileStatic
import org.springframework.core.io.Resource

/**
 * Bridges Grails and Spring resource APIs
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsResource implements Resource{

    org.grails.io.support.Resource resource

    GrailsResource(org.grails.io.support.Resource resource) {
        this.resource = resource
    }

    @Override
    boolean exists() {
        resource.exists()
    }

    @Override
    boolean isReadable() {
        resource.readable
    }

    @Override
    boolean isOpen() {
        false
    }

    @Override
    URL getURL() throws IOException {
        resource.URL
    }

    @Override
    URI getURI() throws IOException {
        resource.URI
    }

    @Override
    File getFile() throws IOException {
        resource.file
    }

    @Override
    long contentLength() throws IOException {
        resource.contentLength()
    }

    @Override
    long lastModified() throws IOException {
        resource.lastModified()
    }

    @Override
    Resource createRelative(String relativePath) throws IOException {
        new GrailsResource(resource.createRelative(relativePath))
    }

    @Override
    String getFilename() {
        resource.filename
    }

    @Override
    String getDescription() {
        resource.description
    }

    @Override
    InputStream getInputStream() throws IOException {
        resource.inputStream
    }
}
