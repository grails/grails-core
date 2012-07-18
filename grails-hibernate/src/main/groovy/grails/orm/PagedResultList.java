/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.orm;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTemplate;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.impl.CriteriaImpl;
import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * A result list for Criteria list calls, which is aware of the totalCount for
 * the paged result.
 *
 * @author Siegfried Puchbauer
 * @since 1.0
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class PagedResultList implements List, Serializable {

    private static final long serialVersionUID = -5820655628956173929L;

    protected List list;

    protected int totalCount = Integer.MIN_VALUE;

    private transient GrailsHibernateTemplate hibernateTemplate;
    private final Criteria criteria;

    public PagedResultList(GrailsHibernateTemplate template, Criteria crit) {
        list = crit.list();
        criteria = crit;
        hibernateTemplate = template;
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean contains(Object o) {
        return list.contains(o);
    }

    public Iterator iterator() {
        return list.iterator();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public Object[] toArray(Object[] objects) {
        return list.toArray(objects);
    }

    public boolean add(Object o) {
        return list.add(o);
    }

    public boolean remove(Object o) {
        return list.remove(o);
    }

    public boolean containsAll(Collection collection) {
        return list.containsAll(collection);
    }

    public boolean addAll(Collection collection) {
        return list.addAll(collection);
    }

    public boolean addAll(int i, Collection collection) {
        return list.addAll(i, collection);
    }

    public boolean removeAll(Collection collection) {
        return list.removeAll(collection);
    }

    public boolean retainAll(Collection collection) {
        return list.retainAll(collection);
    }

    public void clear() {
        list.clear();
    }

    @Override
    public boolean equals(Object o) {
        return list.equals(o);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    public Object get(int i) {
        return list.get(i);
    }

    public Object set(int i, Object o) {
        return list.set(i, o);
    }

    public void add(int i, Object o) {
        list.add(i, o);
    }

    public Object remove(int i) {
        return list.remove(i);
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return list.listIterator();
    }

    public ListIterator listIterator(int i) {
        return list.listIterator(i);
    }

    public List subList(int i, int i1) {
        return list.subList(i, i1);
    }

    public int getTotalCount() {
        if (totalCount == Integer.MIN_VALUE) {
            totalCount = (Integer)hibernateTemplate.execute(new HibernateCallback<Object>() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    CriteriaImpl impl = (CriteriaImpl) criteria;
                    Criteria totalCriteria = session.createCriteria(impl.getEntityOrClassName());
                    hibernateTemplate.applySettings(totalCriteria);

                    Iterator iterator = impl.iterateExpressionEntries();
                    while (iterator.hasNext()) {
                        CriteriaImpl.CriterionEntry entry = (CriteriaImpl.CriterionEntry) iterator.next();
                        totalCriteria.add(entry.getCriterion());
                    }
                    Iterator subcriteriaIterator = impl.iterateSubcriteria();
                    while (subcriteriaIterator.hasNext()) {
                        CriteriaImpl.Subcriteria sub = (CriteriaImpl.Subcriteria) subcriteriaIterator.next();
                        totalCriteria.createAlias(sub.getPath(), sub.getAlias(), sub.getJoinType(), sub.getWithClause());
                    }
                    totalCriteria.setProjection(impl.getProjection());
                    totalCriteria.setProjection(Projections.rowCount());
                    return ((Number)totalCriteria.uniqueResult()).intValue();
                }
            });
        }
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {

        // find the total count if it hasn't been done yet so when this is deserialized
        // the null GrailsHibernateTemplate won't be an issue
        getTotalCount();

        out.defaultWriteObject();
    }
}
