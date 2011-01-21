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
package org.vaadin.addons.criteriacore;

import java.util.LinkedList;

import javax.persistence.EntityManager;

import org.vaadin.addons.lazyquerycontainer.CompositeItem;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

import com.vaadin.data.util.BeanItem;

/**
 * CriteriaContainer enables using JPA 2.0 Criteria type-safe queries with lazy batch loading, filter, sort
 * and buffered writes.
 * @param <T> Entity class.
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
public final class CriteriaContainer<T extends Object> extends LazyQueryContainer {
    
    /**
     * Standard constructor for type-safe queries.
     * @param cd the definition of the query
     */
    public CriteriaContainer(CritQueryDefinition<T> cd){
    	super(new CritQueryView<T>(cd,new CritQueryFactory<T>()));
    }
    
    /**
     * Standard constructor for type-safe queries.
     * @param cd the definition of the query (independent of its execution context)
     * @param cf the factory that will generate a context in which the query will run.
     */
    public CriteriaContainer(
    		CritQueryDefinition<T> cd,
    		CritQueryFactory<T> cf
            ){
    	super(new CritQueryView<T>(cd,cf));
    }

    /**
     * Constructor for typical case where an entity is queried and sorted.
     * @param entityManager The JPA EntityManager.
     * @param applicationManagedTransactions True if application manages transactions instead of container.
     * @param entityClass The entity class.
     * @param batchSize The batch size.
     * @param nativeSortPropertyIds Properties participating in the native sort.
     * @param nativeSortPropertyAscendingStates List of property sort directions for the native sort.
     */
    public CriteriaContainer(
    		final EntityManager entityManager,
    		final boolean applicationManagedTransactions,
    		final Class<T> entityClass,
            final int batchSize,
            final Object[] nativeSortPropertyIds,
            final boolean[] nativeSortPropertyAscendingStates
            )
            {
    	super(
    			new CritQueryDefinition<T>(
    					entityManager,
    					applicationManagedTransactions,
    					entityClass,
    					batchSize,
    					nativeSortPropertyIds,
    					nativeSortPropertyAscendingStates),
    			new CritQueryFactory<T>());
    }
    
    
	/**
	 * Filters the container content by setting "where" criteria in the JPA Criteria.
	 * @param restrictions  restrictions to set to JPA query or null to clear.
	 */
	@SuppressWarnings("unchecked")
	public void filter(LinkedList<CritRestriction> restrictions) {
        ((CritQueryDefinition<T>) getQueryView().getQueryDefinition()).setRestrictions(restrictions);
        refresh();
	}


    /**
     * Adds entity to the container as first item i.e. at index 0.
     * @return the new constructed entity.
     */
    public T addEntity() {
        final Object itemId = addItem();
        return getEntity((Integer) itemId);
    }

    /**
     * Removes given entity at given index and returns it.
     * @param index Index of the entity to be removed.
     * @return The removed entity.
     */
    public T removeEntity(final int index) {
        final T entityToRemove = getEntity(index);
        removeItem(new Integer(index));
        return entityToRemove;
    }
    
    /**
     * Gets entity at given index.
     * @param index The index of the entity.
     * @return the entity.
     */
    @SuppressWarnings("unchecked")
    public T getEntity(final int index) {
        final CompositeItem compositeItem = (CompositeItem) getItem(new Integer(index));
        final BeanItem<T> beanItem = (BeanItem<T>) compositeItem.getItem("bean");
        return beanItem.getBean();
    }



}