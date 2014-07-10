package org.grails.plugins.codecs;

/**
 * Escapes some characters for inclusion in XML documents. The decoder part can
 * unescape XML entity references.
 *
 * @author Lari Hotari
 * @since 2.3
 */
public class XMLCodec extends XMLCodecFactory {
    public XMLCodec() {
        super();
        this.encoder = new XMLEncoder();
    }
}
