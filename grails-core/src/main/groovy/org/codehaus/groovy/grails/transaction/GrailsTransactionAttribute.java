package org.codehaus.groovy.grails.transaction;

import grails.transaction.PreboundResourcesUsage;

import java.util.List;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

public class GrailsTransactionAttribute extends RuleBasedTransactionAttribute {
    private static final long serialVersionUID = 1L;
    private boolean inheritRollbackOnly = true;
    private PreboundResourcesUsage preboundResources = PreboundResourcesUsage.ADAPTIVE;

    public GrailsTransactionAttribute() {
        super();
    }

    public GrailsTransactionAttribute(int propagationBehavior, List<RollbackRuleAttribute> rollbackRules) {
        super(propagationBehavior, rollbackRules);
    }
    
    public GrailsTransactionAttribute(TransactionAttribute other) {
        super();
        setPropagationBehavior(other.getPropagationBehavior());
        setIsolationLevel(other.getIsolationLevel());
        setTimeout(other.getTimeout());
        setReadOnly(other.isReadOnly());
        setName(other.getName());
    }

    public GrailsTransactionAttribute(TransactionDefinition other) {
        super();
        setPropagationBehavior(other.getPropagationBehavior());
        setIsolationLevel(other.getIsolationLevel());
        setTimeout(other.getTimeout());
        setReadOnly(other.isReadOnly());
        setName(other.getName());
    }
    
    public GrailsTransactionAttribute(GrailsTransactionAttribute other) {
        this((RuleBasedTransactionAttribute)other);
    }
    
    public GrailsTransactionAttribute(RuleBasedTransactionAttribute other) {
        super(other);
        if(other instanceof GrailsTransactionAttribute) {
            this.inheritRollbackOnly = ((GrailsTransactionAttribute)other).inheritRollbackOnly;
            this.preboundResources = ((GrailsTransactionAttribute)other).preboundResources;
        }
    }

    public boolean isInheritRollbackOnly() {
        return inheritRollbackOnly;
    }

    public void setInheritRollbackOnly(boolean inheritRollbackOnly) {
        this.inheritRollbackOnly = inheritRollbackOnly;
    }

    public PreboundResourcesUsage getPreboundResources() {
        return preboundResources;
    }

    public void setPreboundResources(PreboundResourcesUsage unbindResources) {
        this.preboundResources = unbindResources;
    }
}
