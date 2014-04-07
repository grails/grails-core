package org.codehaus.groovy.grails.support.encoding;

import grails.util.Holder;

public class EncodingStateRegistryLookupHolder {
    private static Holder<EncodingStateRegistryLookup> holder = new Holder<EncodingStateRegistryLookup>("encodingStateRegistryLookup");

    public static void setEncodingStateRegistryLookup(EncodingStateRegistryLookup lookup) {
        holder.set(lookup);
    }

    public static EncodingStateRegistryLookup getEncodingStateRegistryLookup() {
        return holder.get();
    }
    
    public static void clear() {
        holder.set(null);
    }
}
