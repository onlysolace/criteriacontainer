/**
 * Copyright 2011 Jean-François Lamy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.addons.beantuplecontainer;

import java.util.Collection;
import java.util.LinkedList;

import javax.persistence.metamodel.SingularAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacore.CritRestriction;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

import com.vaadin.data.Buffered;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Container.ItemSetChangeNotifier;
import com.vaadin.data.Container.PropertySetChangeNotifier;
import com.vaadin.data.Container.Sortable;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

/**
 * CriteriaContainer enables using JPA 2.0 Criteria type-safe queries with lazy batch loading, filter, sort
 * and buffered writes.
 * 
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
public class BeanTupleContainer implements Container, Indexed, Sortable, ItemSetChangeNotifier, PropertySetChangeNotifier, Buffered {
	final static Logger logger = LoggerFactory.getLogger(BeanTupleContainer.class);
    
	private LazyQueryContainer lazyQueryContainer;

	private BeanTupleQueryView queryView;
	
    /**
     * Standard constructor for type-safe queries.
     * @param cd the definition of the query
     */
    public BeanTupleContainer(BeanTupleQueryDefinition cd){
    	queryView = new BeanTupleQueryView(cd,new BeanTupleQueryFactory());
		lazyQueryContainer = new LazyQueryContainer(queryView);
    }
	
    /**
     * Standard constructor for type-safe queries.
     * @param qv a custom query view for customization of item access
     */
    public BeanTupleContainer(BeanTupleQueryView qv){
        queryView = qv;
        lazyQueryContainer = new LazyQueryContainer(queryView);
    }
    
	/**
	 * @param entityAlias the alias under which the parent entity is retrieved
	 * @param attribute the attribute being retrieved 
	 * @param defaultValue value to set for the item
	 * @param readOnly the property cannot be set
	 */
	public void addSortableContainerProperty(
			String entityAlias, 
			SingularAttribute<?, ?> attribute,
			Object defaultValue, 
			boolean readOnly) {
		String propertyId = entityAlias+"."+attribute.getName();
		Class<?> javaType = instantatiableType(attribute.getJavaType());
		lazyQueryContainer.addContainerProperty(propertyId, javaType, defaultValue, readOnly, true);
	}


	/**
	 * @param javaType
	 * @return the corresponding class for which newInstance can be called.
	 */
	private Class<?> instantatiableType(Class<?> javaType) {
		if (javaType == long.class) {
			javaType = Long.class;
		} else if (javaType == int.class) {
			javaType = Integer.class;
		} else if (javaType == boolean.class) {
			javaType = Boolean.class;
		}
		return javaType;
	}

	
	/* ----- method replacements 
	 * 
	 * All the methods below are defined in LazyQueryContainer, but not in a Vaadin interface.
	 * They are specific to LazyQueryContainer.   All these methods need to delegate properly
	 * to the wrapped container.
	 * 
	 */

	
	/**
	 * Filters the container content by setting "where" criteria in the JPA Criteria.
	 * @param restrictions  restrictions to set to JPA query or null to clear.
	 */
	public void filter(LinkedList<CritRestriction> restrictions) {
        BeanTupleQueryDefinition critQueryDefinition = queryView.getQueryDefinition();
        critQueryDefinition.setRestrictions(restrictions);
        critQueryDefinition.refresh();
        refresh();
	}
	

	/**
	 * Add a property to the container.
	 * This method should not be called, because sorted properties need special processing
	 * in the BeanTuple container.
	 * 
	 * @param propertyId id of the property
	 * @param type type of the property
	 * @param defaultValue value expected when creating an item (an the corresponding instance)
	 * @param readOnly value can be changed
	 * @param sortable container must be able to sort on this query.
	 * @return true, always.
	 * @see org.vaadin.addons.lazyquerycontainer.LazyQueryContainer#addContainerProperty(java.lang.Object, java.lang.Class, java.lang.Object, boolean, boolean)
	 */
	
	public final boolean addContainerProperty(Object propertyId, Class<?> type,
			Object defaultValue, boolean readOnly, boolean sortable) {
		return lazyQueryContainer.addContainerProperty(propertyId, type, defaultValue, readOnly, sortable);

	}


	/**
	 * 
	 * @see org.vaadin.addons.lazyquerycontainer.LazyQueryContainer#refresh()
	 */
	public final void refresh() {
		lazyQueryContainer.refresh();
	}
	
	
	/* ----- method overrides that are delegated to the wrapped LazyQueryContainer -----------------------
	 * 
	 * All the methods below are defined in the declared Vaadin interfaces, and are delegated to the LazyQuery container.
	 * 
	 */

	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Sortable#getSortableContainerPropertyIds()
	 */
	@Override
	public final Collection<?> getSortableContainerPropertyIds() {
		return lazyQueryContainer.getSortableContainerPropertyIds();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getType(java.lang.Object)
	 */
	@Override
	public final Class<?> getType(Object propertyId) {
		return lazyQueryContainer.getType(propertyId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getItemIds()
	 */
	@Override
	public final Collection<?> getItemIds() {
		return lazyQueryContainer.getItemIds();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getItem(java.lang.Object)
	 */
	@Override
	public final Item getItem(Object itemId) {
		return lazyQueryContainer.getItem(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getContainerProperty(java.lang.Object, java.lang.Object)
	 */
	@Override
	public final Property getContainerProperty(Object itemId, Object propertyId) {
		return lazyQueryContainer.getContainerProperty(itemId, propertyId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Indexed#getIdByIndex(int)
	 */
	@Override
	public final Object getIdByIndex(int index) {
		return lazyQueryContainer.getIdByIndex(index);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#containsId(java.lang.Object)
	 */
	@Override
	public final boolean containsId(Object itemId) {
		return lazyQueryContainer.containsId(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Indexed#addItemAt(int)
	 */
	@Override
	public final Object addItemAt(int index) {
		return lazyQueryContainer.addItemAt(index);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#addItemAfter(java.lang.Object)
	 */
	@Override
	public final Object addItemAfter(Object previousItemId) {
		return lazyQueryContainer.addItemAfter(previousItemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Indexed#addItemAt(int, java.lang.Object)
	 */
	@Override
	public final Item addItemAt(int index, Object newItemId) {
		return lazyQueryContainer.addItemAt(index, newItemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#addItemAfter(java.lang.Object, java.lang.Object)
	 */
	@Override
	public final Item addItemAfter(Object previousItemId, Object newItemId) {
		return lazyQueryContainer.addItemAfter(previousItemId, newItemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#addItem(java.lang.Object)
	 */
	@Override
	public final Item addItem(Object itemId) {
		return lazyQueryContainer.addItem(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#addItem()
	 */
	@Override
	public final Object addItem() {
		return lazyQueryContainer.addItem();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.ItemSetChangeNotifier#addListener(com.vaadin.data.Container.ItemSetChangeListener)
	 */
	@Override
	public final void addListener(ItemSetChangeListener listener) {
		lazyQueryContainer.addListener(listener);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.PropertySetChangeNotifier#addListener(com.vaadin.data.Container.PropertySetChangeListener)
	 */
	@Override
	public final void addListener(PropertySetChangeListener listener) {
		lazyQueryContainer.addListener(listener);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#commit()
	 */
	@Override
	public final void commit() {
		lazyQueryContainer.commit();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#discard()
	 */
	@Override
	public final void discard() {
		lazyQueryContainer.discard();
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return lazyQueryContainer.equals(obj);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#firstItemId()
	 */
	@Override
	public final Object firstItemId() {
		return lazyQueryContainer.firstItemId();
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return lazyQueryContainer.hashCode();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#removeContainerProperty(java.lang.Object)
	 */
	@Override
	public final boolean removeContainerProperty(Object propertyId) {
		return lazyQueryContainer.removeContainerProperty(propertyId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#size()
	 */
	@Override
	public final int size() {
		return lazyQueryContainer.size();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Indexed#indexOfId(java.lang.Object)
	 */
	@Override
	public final int indexOfId(Object itemId) {
		return lazyQueryContainer.indexOfId(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#isFirstId(java.lang.Object)
	 */
	@Override
	public final boolean isFirstId(Object itemId) {
		return lazyQueryContainer.isFirstId(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#isLastId(java.lang.Object)
	 */
	@Override
	public final boolean isLastId(Object itemId) {
		return lazyQueryContainer.isLastId(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#lastItemId()
	 */
	@Override
	public final Object lastItemId() {
		return lazyQueryContainer.lastItemId();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#nextItemId(java.lang.Object)
	 */
	@Override
	public final Object nextItemId(Object itemId) {
		return lazyQueryContainer.nextItemId(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#prevItemId(java.lang.Object)
	 */
	@Override
	public final Object prevItemId(Object itemId) {
		return lazyQueryContainer.prevItemId(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#removeItem(java.lang.Object)
	 */
	@Override
	public final boolean removeItem(Object itemId) {
		return lazyQueryContainer.removeItem(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#removeAllItems()
	 */
	@Override
	public final boolean removeAllItems() {
		return lazyQueryContainer.removeAllItems();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.ItemSetChangeNotifier#removeListener(com.vaadin.data.Container.ItemSetChangeListener)
	 */
	@Override
	public final void removeListener(ItemSetChangeListener listener) {
		lazyQueryContainer.removeListener(listener);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.PropertySetChangeNotifier#removeListener(com.vaadin.data.Container.PropertySetChangeListener)
	 */
	@Override
	public final void removeListener(PropertySetChangeListener listener) {
		lazyQueryContainer.removeListener(listener);
	}

	
	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#isModified()
	 */
	@Override
	public final boolean isModified() {
		return lazyQueryContainer.isModified();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#isReadThrough()
	 */
	@Override
	public final boolean isReadThrough() {
		return lazyQueryContainer.isReadThrough();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#isWriteThrough()
	 */
	@Override
	public final boolean isWriteThrough() {
		return lazyQueryContainer.isWriteThrough();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#setReadThrough(boolean)
	 */
	@Override
	public final void setReadThrough(boolean readThrough) {
		lazyQueryContainer.setReadThrough(readThrough);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#setWriteThrough(boolean)
	 */
	@Override
	public final void setWriteThrough(boolean writeThrough) {
		lazyQueryContainer.setWriteThrough(writeThrough);
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return lazyQueryContainer.toString();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Sortable#sort(java.lang.Object[], boolean[])
	 */
	@Override
	public final void sort(Object[] sortPropertyIds, boolean[] ascendingStates) {
		lazyQueryContainer.sort(sortPropertyIds, ascendingStates);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getContainerPropertyIds()
	 */
	@Override
	public final Collection<?> getContainerPropertyIds() {
		return lazyQueryContainer.getContainerPropertyIds();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#addContainerProperty(java.lang.Object, java.lang.Class, java.lang.Object)
	 */
	@Override
	public final boolean addContainerProperty(Object propertyId, Class<?> type,
			Object defaultValue) {
		return lazyQueryContainer.addContainerProperty(propertyId, type,
				defaultValue);
	}


}
