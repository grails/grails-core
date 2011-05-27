/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.collections.iterators.AbstractIteratorDecorator;
import org.apache.commons.collections.list.UnmodifiableList;
import org.apache.commons.collections.set.AbstractSerializableSetDecorator;

/**
 * Forked from Apache Commons Collections' implementation of ListOrderedSet. This one actually implements the List interface.
 *
 * Yes we are away of the warnings in the javadoc about problems with incompatibilities between the List and Set interfaces, however
 * this class is designed to used internally only for data binding and not by end users.
 *
 * @author Stephen Colebourne
 * @author Henning P. Schmiedehausen
 * @author Graeme Rocher
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ListOrderedSet extends AbstractSerializableSetDecorator implements Set, List {
    private static final long serialVersionUID = -228664372470420141L;

    /** Internal list to hold the sequence of objects */
    protected final List setOrder;

    /**
     * Factory method to create an ordered set.
     *
     * An ArrayList is used to retain order.
     *
     * @param set the set to decorate, must not be null
     * @throws IllegalArgumentException if set is null
     */
    public static ListOrderedSet decorate(Set set) {
        return new ListOrderedSet(set);
    }

    /**
     * Factory method to create an ordered set using the supplied list to retain order.
     *
     * A HashSet is used for the set behaviour.
     *
     * @param list the list to decorate, must not be null
     * @throws IllegalArgumentException if list is null
     */
    public static ListOrderedSet decorate(List list) {
        if (list == null) {
            throw new IllegalArgumentException("List must not be null");
        }
        Set set = new HashSet(list);
        list.retainAll(set);

        return new ListOrderedSet(set, list);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructs a new empty ListOrderedSet using
     * a HashSet and an ArrayList internally.
     *
     * @since Commons Collections 3.1
     */
    public ListOrderedSet() {
        super(new HashSet());
        setOrder = new ArrayList();
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param set the set to decorate, must not be null
     * @throws IllegalArgumentException if set is null
     */
    protected ListOrderedSet(Set set) {
        super(set);
        setOrder = new ArrayList(set);
    }

    /**
     * Constructor that wraps (not copies) the Set and specifies the list to use.
     *
     * The set and list must both be correctly initialised to the same elements.
     *
     * @param set the set to decorate, must not be null
     * @param list the list to decorate, must not be null
     * @throws IllegalArgumentException if set or list is null
     */
    protected ListOrderedSet(Set set, List list) {
        super(set);
        if (list == null) {
            throw new IllegalArgumentException("List must not be null");
        }
        setOrder = list;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets an unmodifiable view of the order of the Set.
     *
     * @return an unmodifiable list view
     */
    public List asList() {
        return UnmodifiableList.decorate(setOrder);
    }

    //-----------------------------------------------------------------------
    @Override
    public void clear() {
        collection.clear();
        setOrder.clear();
    }

    @Override
    public Iterator iterator() {
        return new OrderedSetIterator(setOrder.iterator(), collection);
    }

    @Override
    public boolean add(Object object) {
        if (collection.contains(object)) {
            // re-adding doesn't change order
            return collection.add(object);
        }
        // first add, so add to both set and list
        boolean result = collection.add(object);
        setOrder.add(object);
        return result;
    }

    @Override
    public boolean addAll(Collection coll) {
        boolean result = false;
        for (Iterator it = coll.iterator(); it.hasNext();) {
            Object object = it.next();
            result = result | add(object);
        }
        return result;
    }

    @Override
    public boolean remove(Object object) {
        boolean result = collection.remove(object);
        setOrder.remove(object);
        return result;
    }

    @Override
    public boolean removeAll(Collection coll) {
        boolean result = false;
        for (Iterator it = coll.iterator(); it.hasNext();) {
            Object object = it.next();
            result = result | remove(object);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection coll) {
        boolean result = collection.retainAll(coll);
        if (result == false) {
            return false;
        }

        if (collection.size() == 0) {
            setOrder.clear();
        }
        else {
            for (Iterator it = setOrder.iterator(); it.hasNext();) {
                Object object = it.next();
                if (collection.contains(object) == false) {
                    it.remove();
                }
            }
        }
        return result;
    }

    @Override
    public Object[] toArray() {
        return setOrder.toArray();
    }

    @Override
    public Object[] toArray(Object a[]) {
        return setOrder.toArray(a);
    }

    //-----------------------------------------------------------------------
    public Object get(int index) {
        return setOrder.get(index);
    }

    public int indexOf(Object object) {
        return setOrder.indexOf(object);
    }

    public void add(int index, Object object) {
        if (contains(object) == false) {
            collection.add(object);
            setOrder.add(index, object);
        }
    }

    public boolean addAll(int index, Collection coll) {
        boolean changed = false;
        for (Iterator it = coll.iterator(); it.hasNext();) {
            Object object = it.next();
            if (contains(object) == false) {
                collection.add(object);
                setOrder.add(index, object);
                index++;
                changed = true;
            }
        }
        return changed;
    }

    public Object remove(int index) {
        Object obj = setOrder.remove(index);
        remove(obj);
        return obj;
    }

    /**
     * Uses the underlying List's toString so that order is achieved.
     * This means that the decorated Set's toString is not used, so
     * any custom toStrings will be ignored.
     */
    // Fortunately List.toString and Set.toString look the same
    @Override
    public String toString() {
        return setOrder.toString();
    }

    //-----------------------------------------------------------------------
    /**
     * Internal iterator handle remove.
     */
    static class OrderedSetIterator extends AbstractIteratorDecorator {

        /** Object we iterate on */
        protected final Collection set;
        /** Last object retrieved */
        protected Object last;

        private OrderedSetIterator(Iterator iterator, Collection set) {
            super(iterator);
            this.set = set;
        }

        @Override
        public Object next() {
            last = iterator.next();
            return last;
        }

        @Override
        public void remove() {
            set.remove(last);
            iterator.remove();
            last = null;
        }
    }

    public int lastIndexOf(Object o) {
        return setOrder.lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return setOrder.listIterator();
    }

    public ListIterator listIterator(int index) {
        return setOrder.listIterator(index);
    }

    public Object set(int index, Object element) {
        Object current = get(index);
        remove(current);
        add(element);
        return setOrder.set(index, element);
    }

    public List subList(int fromIndex, int toIndex) {
        return setOrder.subList(fromIndex, toIndex);
    }
}
