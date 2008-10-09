package org.codehaus.groovy.grails.web;

import org.springframework.web.multipart.MultipartHttpServletRequest;

public class MultipartRequestHolder {

    static ThreadLocal holder = new ThreadLocal();


    public static void setMultipartRequest(MultipartHttpServletRequest mpr) {
        holder.set(mpr);
    }

    public static MultipartHttpServletRequest getMultipartRequest(){
        return (MultipartHttpServletRequest) holder.get();
    }
}
