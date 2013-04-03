package org.codehaus.groovy.grails.support.encoding;

import java.util.Set;

public interface CodecIdentifier {
    public String getCodecName();
    public Set<String> getCodecAliases();
    public boolean isEquivalent(CodecIdentifier other);
}
