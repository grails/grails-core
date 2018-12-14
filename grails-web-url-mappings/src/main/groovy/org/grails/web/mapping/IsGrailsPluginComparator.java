package org.grails.web.mapping;

import grails.core.GrailsClass;

import java.util.Comparator;

public class IsGrailsPluginComparator implements Comparator<GrailsClass> {

    @Override
    public int compare(GrailsClass o1, GrailsClass o2) {
        if (o1.getPluginName() != null && o2.getPluginName() == null) {
            return 1;
        }
        if (o1.getPluginName() == null && o2.getPluginName() != null) {
            return -1;
        }
        return 0;
    }
}
