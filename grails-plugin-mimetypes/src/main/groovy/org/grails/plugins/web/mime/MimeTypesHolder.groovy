package org.grails.plugins.web.mime

import grails.web.mime.MimeType

class MimeTypesHolder {
    final MimeType[] mimeTypes

    MimeTypesHolder(MimeType[] mimeTypes) {
        this.mimeTypes = mimeTypes
    }
}
