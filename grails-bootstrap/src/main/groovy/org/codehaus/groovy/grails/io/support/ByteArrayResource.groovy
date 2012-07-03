package org.codehaus.groovy.grails.io.support

import groovy.transform.CompileStatic

/**
 * A Resource impl used represent a Resource as an array of bytes
 */
@CompileStatic
class ByteArrayResource implements Resource{
    byte[] bytes
    String description = "resource loaded from byte array"

    ByteArrayResource(byte[] bytes) {
        this.bytes = bytes
    }

    ByteArrayResource(byte[] bytes, String desc) {
        this.bytes = bytes
        this.description = desc
    }

    InputStream getInputStream() {
        return new ByteArrayInputStream(bytes)
    }

    boolean exists() { true }

    boolean isReadable() { true }

    URL getURL() {
        throw new UnsupportedOperationException("Method getURL not supported")
    }

    URI getURI() {
        throw new UnsupportedOperationException("Method getURI not supported")
    }

    File getFile() {
        throw new UnsupportedOperationException("Method getFile not supported")
    }

    long contentLength() { bytes.length }

    long lastModified() { 0 }

    String getFilename() { description }

    Resource createRelative(String relativePath) {
        throw new UnsupportedOperationException("Method createRelative not supported")
    }
}
