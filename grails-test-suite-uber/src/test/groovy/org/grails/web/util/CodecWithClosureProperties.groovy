package org.grails.web.util

class CodecWithClosureProperties {
    static encode = { arg ->
        "-> ${arg} <-"
    }
}