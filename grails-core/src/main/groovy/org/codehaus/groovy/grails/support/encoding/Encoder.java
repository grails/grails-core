package org.codehaus.groovy.grails.support.encoding;

import java.util.Set;

public interface Encoder {
    public String getCodecName();
    public Set<String> getEquivalentCodecNames();
    public boolean isPreventAllOthers();
    public Object encode(Object o);
    public void markEncoded(CharSequence string);
}