package org.grails.buffer

class StreamCharBufferMetaUtils {
    static registerStreamCharBufferMetaClass() {
        StreamCharBuffer.metaClass.methodMissing = { String name, args ->
            def retval = delegate.toString().invokeMethod(name, args)
            StreamCharBuffer.metaClass."$name" = { Object[] varArgs ->
                delegate.toString().invokeMethod(name, varArgs)
            }
            retval
        }

        StreamCharBuffer.metaClass.asType = { Class clazz ->
            if (clazz == String) {
                delegate.toString()
            } else if (clazz == char[]) {
                delegate.toCharArray()
            } else if (clazz == Boolean || clazz == boolean) {
                delegate.asBoolean()
            } else {
                delegate.toString().asType(clazz)
            }
        }
    }
}
