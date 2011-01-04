/**
 * Copyright 2010 Jean-Fran�ois Lamy
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
package org.vaadin.addons.criteriacontainer;

import java.util.LinkedList;
import java.util.Map;

import javax.persistence.EntityManager;

import org.vaadin.addons.lazyquerycontainer.CompositeItem;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

import com.vaadin.data.util.BeanItem;

/**
 * CriteriaContainer enables using JPA 2.0 Criteria type-safe queries with lazy batch loading, filter, sort
 * and buffered writes.
 * @param <T> Entity class.
 * @author Jean-Fran�ois Lamy
 */
@SuppressWarnings("serial")
public final class CriteriaContainer<T extends Object> extends LazyQueryContainer {
    
    /**
     * Standard constructor for type-safe queries.
     * Note: contrary to standard LazyQueryContainer, the entityManager is specified on the
     * Factory, to allow better reuse of CritQueryDefinition.
     * @param cd
     * @param cf
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
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public CriteriaContainer(
    		final EntityManager entityManager,
    		final boolean applicationManagedTransactions,
    		final Class<T> entityClass,
            final int batchSize,
            final Object[] nativeSortPropertyIds,
            final boolean[] nativeSortPropertyAscendingStates
            ) throws InstantiationException, IllegalAccessException 
            {
    	super(
    			new CritQueryDefinition<T>(
    					applicationManagedTransactions,
    					entityClass,
    					batchSize,
    					nativeSortPropertyIds,
    					nativeSortPropertyAscendingStates),
    			new CritQueryFactory<T>(entityManager));
    }
    
    /**
     * Filters the container content by setting "where" criteria in the JPA Criteria.
     * @param whereParameters the where parameters to set to JPA query or null to clear.
     */
    @SuppressWarnings("unchecked")
	public void filter(final Map<String, Object> whereParameters) {
        ((CritQueryDefinition<T>) getQueryView().getQueryDefinition()).setWhereParameters(whereParameters);
        refresh();
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
