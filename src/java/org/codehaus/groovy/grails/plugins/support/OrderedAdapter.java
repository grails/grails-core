package org.codehaus.groovy.grails.plugins.support;

import org.springframework.core.Ordered;

/**
 * <p>Convenience adapter implementation of the Spring {@link Ordered} interface.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public abstract class OrderedAdapter implements Ordered {
    private int order = Integer.MAX_VALUE;

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
