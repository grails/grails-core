package org.grails.web.servlet.mvc;

import java.io.Serializable;

public interface HibernateProxy extends Serializable {
    Object writeReplace();
    LazyInitializer getHibernateLazyInitializer();
}
