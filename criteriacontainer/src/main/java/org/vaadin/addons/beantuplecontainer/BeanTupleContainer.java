/**
 * Copyright 2011-2012 Jean-François Lamy
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacore.FilterRestriction;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

import com.vaadin.data.Buffered;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Filterable;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Container.ItemSetChangeNotifier;
import com.vaadin.data.Container.PropertySetChangeNotifier;
import com.vaadin.data.Container.Sortable;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.filter.UnsupportedFilterException;

/**
 * <p>BeanTupleContainer enables using JPA 2.0 Tuple type-safe queries with lazy batch loading, filter, sort
 * and buffered writes, and supports joins where more than one entity is returned (as is the case with joins).</p>
 * 
 * <p>A BeanTupleContainer contains a set of {@link BeanTupleItem}. Such an item exists for each line of the join.
 * Nested properties are supported. For example, if the Person entity is joined to the Task entity via the
 * assignedTo relationship, it will be possible to retrieve all the "Person, Task" pairs for which this relationship
 * is true, and the item will allow retrieval of Person.name and Task.dueDate as item properties if these fields
 * are present in the entities.  Also, each of the entities returned is editable. When an entity appears in several tuples,
 * it is the same entity -- changing the entity will be reflected everywhere when the item is written and the container
 * refreshed.</p>
 * 
 * <p>BeanTupleContainer relies on JPA 2.0 Criteria queries: these queries are described as Java objects. The container
 * will ask the BeanTupleQueryDefinition for the properties it returns. This is done automatically, by inspecting
 * the JPA 2.0 query.</p>
 * 
 * <p>Note that BeanTupleContainer cannot add items (each item is a tuple from a join). It is of course possible to
 * add entities using JPA, and refresh the container.  Another way to add items is to use the specialized version
 * of BeanTupleContainer called CriteriaContainer, which only allows one entity and is therefore able to support addition.</p>
 * 
 * @author Jean-François Lamy
 */
@SuppressWarnings({ "serial", "deprecation" })
public class BeanTupleContainer implements Container, Indexed, Sortable, ItemSetChangeNotifier, PropertySetChangeNotifier, Buffered, Filterable {
	final static Logger logger = LoggerFactory.getLogger(BeanTupleContainer.class);

	private LazyQueryContainer lazyQueryContainer;

	private BeanTupleQueryView queryView;

	/** Attach data to the container, such as a name for debugging purposes.	 */
	protected Object data;

	/**
	 * Standard constructor for type-safe queries.
	 * @param cd the definition of the query
	 */
	public BeanTupleContainer(BeanTupleQueryDefinition cd){
		BeanTupleQueryFactory queryFactory = new BeanTupleQueryFactory();
		queryView = new BeanTupleQueryView(cd,queryFactory);
		// query factory must know its view
		queryFactory.setKeyToIdMapper(queryView);
		lazyQueryContainer = new LazyQueryContainer(queryView);
	}

	/**
	 * Standard constructor for type-safe queries.
	 * @param qv a custom query view for customization of item access
	 */
	public BeanTupleContainer(BeanTupleQueryView qv){
		queryView = qv;
		lazyQueryContainer = new LazyQueryContainer(queryView);
		// query factory must know its view
		queryView.getQueryFactory().setKeyToIdMapper(queryView);
	}



	/* ----- method replacements 
	 * 
	 * The methods below replace those of LazyQueryContainer.
	 * 
	 */


	/**
	 * Filters the container content by setting "where" criteria in the JPA Criteria.
	 * <p>The query view and query definition must be refreshed, and then the container.</p>
	 * @deprecated use the {@link Filterable} interface instead.
	 * @param restrictions  restrictions to set to JPA query or null to clear.
	 */
	@Deprecated
	public void filter(LinkedList<FilterRestriction> restrictions) {
		BeanTupleQueryDefinition critQueryDefinition = queryView.getQueryDefinition();
		critQueryDefinition.setFilters(restrictions);
		refresh(); // refresh the queryView and the queryDefinition container.
	}

	/* ----- LazyQueryContainer methods delegated to the wrapped LazyQueryContainer -----------------------
	 * 
	 * The methods below are defined in LazyQueryContainer, but not in a Vaadin interface.
	 * They are specific to LazyQueryContainer; they are delegated to the wrapped container.
	 */

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

	public boolean addContainerProperty(Object propertyId, Class<?> type,
			Object defaultValue, boolean readOnly, boolean sortable) {
		return lazyQueryContainer.addContainerProperty(propertyId, type, defaultValue, readOnly, sortable);
	}


	/**
	 * 
	 * @see org.vaadin.addons.lazyquerycontainer.LazyQueryContainer#refresh()
	 */
	public void refresh() {
		lazyQueryContainer.refresh();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Filterable#addContainerFilter(com.vaadin.data.Container.Filter)
	 */
	@Override
	public void addContainerFilter(Filter filter)
			throws UnsupportedFilterException {
		BeanTupleQueryDefinition queryDefinition = queryView.getQueryDefinition();
		queryDefinition.addFilter(filter);
		refresh();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Filterable#removeContainerFilter(com.vaadin.data.Container.Filter)
	 */
	@Override
	public void removeContainerFilter(Filter filter) {
		BeanTupleQueryDefinition queryDefinition = queryView.getQueryDefinition();
		queryDefinition.removeFilter(filter);
		refresh();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Filterable#removeAllContainerFilters()
	 */
	@Override
	public void removeAllContainerFilters() {
		BeanTupleQueryDefinition queryDefinition = queryView.getQueryDefinition();
		queryDefinition.clearFilters();
		refresh();
	}

	/**
	 * Remove all filters.
	 * The refreshContainer argument should be false if you are doing several manipulations
	 * in a row and don't want a refresh
	 * @param refreshContainer  if true, refresh the container immediately, else wait. 
	 */
	public void removeAllContainerFilters(boolean refreshContainer) {
		BeanTupleQueryDefinition queryDefinition = queryView.getQueryDefinition();
		queryDefinition.clearFilters();
		if (refreshContainer){
			refresh();
		} else {
			queryDefinition.refresh();
		}
	}


	/**
	 * Add a filter to the Container.
	 * The refreshContainer argument should be false if you are doing several manipulations
	 * in a row and don't want a refresh.
	 * @param filter the filter to remove
	 * @param refreshContainer  if true, refresh the container immediately, else wait.
	 * @throws UnsupportedFilterException if the filter is not supported by the container.
	 */
	public void addContainerFilter(Filter filter, boolean refreshContainer)
			throws UnsupportedFilterException {
		BeanTupleQueryDefinition queryDefinition = queryView.getQueryDefinition();
		queryDefinition.addFilter(filter);
		if (refreshContainer){
			refresh();
		} else {
			queryDefinition.refresh();
		}	
	}

	/**
	 * Remove filters from the container.
	 * The refreshContainer argument should be false if you are doing several manipulations
	 * in a row and don't want a refresh.
	 * @param filter the filter to remove
	 * @param refreshContainer  if true, refresh the container immediately, else wait.
	 */
	public void removeContainerFilter(Filter filter, boolean refreshContainer) {
		BeanTupleQueryDefinition queryDefinition = queryView.getQueryDefinition();
		queryDefinition.removeFilter(filter);
		if (refreshContainer){
			refresh();
		} else {
			queryDefinition.refresh();
		}	
	}



	/* ----- interface methods are delegated to the wrapped LazyQueryContainer -----------------------
	 * 
	 * All the methods below are defined in the declared Vaadin interfaces, and are delegated to the LazyQuery container.
	 * 
	 */

	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Sortable#getSortableContainerPropertyIds()
	 */
	@Override
	public Collection<?> getSortableContainerPropertyIds() {
		return lazyQueryContainer.getSortableContainerPropertyIds();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getType(java.lang.Object)
	 */
	@Override
	public Class<?> getType(Object propertyId) {
		return lazyQueryContainer.getType(propertyId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getItemIds()
	 */
	@Override
	public Collection<?> getItemIds() {
		return queryView.getItemIds();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getItem(java.lang.Object)
	 */
	@Override
	public Item getItem(Object itemId) {
		// we jump over lazyQueryContainer to avoid the cast to Integer
		return queryView.getItem(itemId);
	}


	/**
	 * @param itemId the index inside the container
	 * @return the item found
	 */
	public Item getItem(int itemId) {
		// we call directly the view method to avoid the cast to Integer
		// that the lazy container does.
		return queryView.getItem(itemId);
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getContainerProperty(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Property getContainerProperty(Object itemId, Object propertyId) {
		//logger.debug("itemId={} size={}",itemId,size());
		Item item = queryView.getItem(itemId);
		if (item == null) return null;
		return item.getItemProperty(propertyId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Indexed#getIdByIndex(int)
	 */
	@Override
	public Object getIdByIndex(int index) {
		if (queryView.getKeyPropertyId() == null) {
			return lazyQueryContainer.getIdByIndex(index);
		} else {
			return queryView.getIdByIndex(index);
		}
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#containsId(java.lang.Object)
	 */
	@Override
	public boolean containsId(Object itemId) {
		if (queryView.getKeyPropertyId() != null) {
			return queryView.containsId(itemId);
		} else {
			return lazyQueryContainer.containsId(itemId);
		}
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Indexed#addItemAt(int)
	 */
	@Override
	public Object addItemAt(int index) {
		return lazyQueryContainer.addItemAt(index);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#addItemAfter(java.lang.Object)
	 */
	@Override
	public Object addItemAfter(Object previousItemId) {
		return lazyQueryContainer.addItemAfter(previousItemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Indexed#addItemAt(int, java.lang.Object)
	 */
	@Override
	public Item addItemAt(int index, Object newItemId) {
		return lazyQueryContainer.addItemAt(index, newItemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#addItemAfter(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Item addItemAfter(Object previousItemId, Object newItemId) {
		return lazyQueryContainer.addItemAfter(previousItemId, newItemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#addItem(java.lang.Object)
	 */
	@Override
	public Item addItem(Object itemId) {
		return lazyQueryContainer.addItem(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#addItem()
	 */
	@Override
	public Object addItem() {
		return lazyQueryContainer.addItem();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.ItemSetChangeNotifier#addListener(com.vaadin.data.Container.ItemSetChangeListener)
	 */
	@Override
	public void addListener(ItemSetChangeListener listener) {
		lazyQueryContainer.addListener(listener);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.PropertySetChangeNotifier#addListener(com.vaadin.data.Container.PropertySetChangeListener)
	 */
	@Override
	public void addListener(PropertySetChangeListener listener) {
		lazyQueryContainer.addListener(listener);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#commit()
	 */
	@Override
	public void commit() {
		lazyQueryContainer.commit();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#discard()
	 */
	@Override
	public void discard() {
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
	public Object firstItemId() {
		return queryView.getIdByIndex((Integer)lazyQueryContainer.firstItemId());
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
	public boolean removeContainerProperty(Object propertyId) {
		return lazyQueryContainer.removeContainerProperty(propertyId);
	}


	/**
	 * Compute number of items in the container.
	 * <p>
	 * The size is adjusted if you add items via the container itself (removed items
	 * are considered to be in the container, but marked as invisible until the changes
	 * are actually saved, at which time the container is refreshed and values recomputed.
	 * Important: If you add or delete entities through JPA, you need to refresh the container for
	 * the size to be recomputed.</p>
	 * @return the number of items in the container
	 * @see com.vaadin.data.Container#size()
	 */
	@Override
	public int size() {
		return lazyQueryContainer.size();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Indexed#indexOfId(java.lang.Object)
	 */
	@Override
	public int indexOfId(Object itemId) {
		return queryView.getIndex(itemId);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#isFirstId(java.lang.Object)
	 */
	@Override
	public boolean isFirstId(Object itemId) {
		return lazyQueryContainer.isFirstId(queryView.getIndex(itemId));
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#isLastId(java.lang.Object)
	 */
	@Override
	public boolean isLastId(Object itemId) {
		return lazyQueryContainer.isLastId(queryView.getIndex(itemId));
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#lastItemId()
	 */
	@Override
	public Object lastItemId() {
		return queryView.getIdByIndex((Integer)lazyQueryContainer.lastItemId());
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#nextItemId(java.lang.Object)
	 */
	@Override
	public Object nextItemId(Object itemId) {
		return queryView.getIdByIndex((Integer)lazyQueryContainer.nextItemId(queryView.getIndex(itemId)));
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.Ordered#prevItemId(java.lang.Object)
	 */
	@Override
	public Object prevItemId(Object itemId) {
		return queryView.getIdByIndex((Integer)lazyQueryContainer.prevItemId(queryView.getIndex(itemId)));
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#removeItem(java.lang.Object)
	 */
	@Override
	public boolean removeItem(Object itemId) {
		return lazyQueryContainer.removeItem(queryView.getIndex(itemId));
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#removeAllItems()
	 */
	@Override
	public boolean removeAllItems() {
		return lazyQueryContainer.removeAllItems();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.ItemSetChangeNotifier#removeListener(com.vaadin.data.Container.ItemSetChangeListener)
	 */
	@Override
	public void removeListener(ItemSetChangeListener listener) {
		lazyQueryContainer.removeListener(listener);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container.PropertySetChangeNotifier#removeListener(com.vaadin.data.Container.PropertySetChangeListener)
	 */
	@Override
	public void removeListener(PropertySetChangeListener listener) {
		lazyQueryContainer.removeListener(listener);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#isModified()
	 */
	@Override
	public boolean isModified() {
		return lazyQueryContainer.isModified();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#isReadThrough()
	 */
	@Override
	public boolean isReadThrough() {
		return lazyQueryContainer.isReadThrough();
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#isWriteThrough()
	 */
	@Override
	public boolean isWriteThrough() {
		return lazyQueryContainer.isWriteThrough();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#setReadThrough(boolean)
	 */
	@Override
	public void setReadThrough(boolean readThrough) {
		lazyQueryContainer.setReadThrough(readThrough);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Buffered#setWriteThrough(boolean)
	 */
	@Override
	public void setWriteThrough(boolean writeThrough) {
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
	public void sort(Object[] sortPropertyIds, boolean[] ascendingStates) {
		lazyQueryContainer.sort(sortPropertyIds, ascendingStates);
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#getContainerPropertyIds()
	 */
	@Override
	public Collection<?> getContainerPropertyIds() {
		queryView.getQueryDefinition().init();
		Collection<?> containerPropertyIds = lazyQueryContainer.getContainerPropertyIds();
		return containerPropertyIds;
	}


	/* (non-Javadoc)
	 * @see com.vaadin.data.Container#addContainerProperty(java.lang.Object, java.lang.Class, java.lang.Object)
	 */
	@Override
	public boolean addContainerProperty(Object propertyId, Class<?> type,
			Object defaultValue) {
		return lazyQueryContainer.addContainerProperty(propertyId, type,
				defaultValue);
	}

	/**
	 * @return the data
	 */
	public Object getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(Object data) {
		this.data = data;
	}

	/**
	 * @param keyId the property to use as object identifier, null if the default int is desired.
	 */
	public void setKeyPropertyId(Object keyId) {
		queryView.setKeyPropertyId(keyId);
		queryView.refresh();
	}

	/**
	 * @return the property currently 
	 */
	public Object getKeyPropertyId() {
		return queryView.getKeyPropertyId();
	}


	/**
	 * @param queryView the queryView to set
	 */
	public void setQueryView(BeanTupleQueryView queryView) {
		this.queryView = queryView;
	}

	/**
	 * @return the queryView
	 */
	public BeanTupleQueryView getQueryView() {
		return queryView;
	}

}
