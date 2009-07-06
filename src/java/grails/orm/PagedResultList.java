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

import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.util.ListIterator;
import java.io.Serializable;

/**
* A result list for Criteria list calls, which is aware of the totalCount for the paged result
*
* @author Siegfried Puchbauer
* @since 1.0
*/
public class PagedResultList implements List, Serializable {

    protected List list;

    protected int totalCount;

    public PagedResultList(List list) {
        this.list = list;
    }

    public PagedResultList(List list, int totalCount) {
        this.list = list;
        this.totalCount = totalCount;
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

    public boolean equals(Object o) {
        return list.equals(o);
    }

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
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
}
