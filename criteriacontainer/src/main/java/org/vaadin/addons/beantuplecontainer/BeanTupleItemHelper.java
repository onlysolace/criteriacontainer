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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.lazyquerycontainer.Query;

import com.vaadin.data.Item;
import com.vaadin.data.util.ObjectProperty;

/**
 * Query that returns a Tuple
 * 
 * @author Jean-François Lamy
 */

public class BeanTupleItemHelper implements Query {
	
    @SuppressWarnings("unused")
    private Logger logger = LoggerFactory.getLogger(BeanTupleItemHelper.class);
	
    /** The JPA EntityManager. */
    protected EntityManager entityManager;
    
    /** Flag reflecting whether application manages transactions. */
    protected boolean applicationTransactionManagement;

    /** The JPA select query. */
    private TypedQuery<Tuple> selectQuery;
    
    /** The JPA select count query. */
    private TypedQuery<Long> selectCountQuery;
    
    /** QueryDefinition contains definition of the query properties and batch size. */
    protected BeanTupleQueryDefinition queryDefinition;
    
    /** The size of the query. */
    private int querySize = -1;


    /**
     * Constructor for configuring the query.
     * @param criteriaQueryDefinition The entity query definition..
     */
    public BeanTupleItemHelper(final BeanTupleQueryDefinition criteriaQueryDefinition) {
        this.queryDefinition = criteriaQueryDefinition;
        
        this.entityManager = queryDefinition.getEntityManager();
        this.selectQuery = criteriaQueryDefinition.getSelectQuery();
        this.selectCountQuery = criteriaQueryDefinition.getCountQuery();
        this.applicationTransactionManagement = criteriaQueryDefinition.isApplicationManagedTransactions();
    }
   
    
    /**
     * Constructs new item based on QueryDefinition.
     * Item is initialized with the default values.
     * 
     * @return new item with default values.
     */
	@Override
	public Item constructItem() {
        try {
            BeanTupleItem tupleItem = new BeanTupleItem();
            
            for (Object propertyId : queryDefinition.getPropertyIds()) {
            	tupleItem.addItemProperty(propertyId, 
            			new ObjectProperty<Object>(queryDefinition.getPropertyDefaultValue(propertyId)));
            }
            return tupleItem;
        } catch (Exception e) {
            throw new RuntimeException("Error in bean construction or property population with default values.", e);
        }
    }

    /**
     * Number of beans returned by query.
     * @return number of beans.
     */
    @Override
	public int size() {
        if (querySize == -1) {
            querySize = ((Number) selectCountQuery.getSingleResult()).intValue();
        }
        return querySize;
    }

    /**
     * Load batch of items.
     * @param startIndex Starting index of the item list.
     * @param count Count of the items to be retrieved.
     * @return List of items.
     */
    @Override
	public List<Item> loadItems(final int startIndex, final int count) {
        selectQuery.setFirstResult(startIndex);
        selectQuery.setMaxResults(count);

        List<?> tuples = selectQuery.getResultList();
        List<Item> items = new ArrayList<Item>();
        for (Object entity : tuples) {
            items.add(toItem((Tuple) entity));
        }
        return items;
    }

    
    /**
     * Saves the modifications done by container to the query result.
     * Query will be discarded after changes have been saved
     * and new query loaded so that changed items are sorted
     * appropriately.
     * @param addedItems Items to be inserted.
     * @param modifiedItems Items to be updated.
     * @param removedItems Items to be deleted.
     */
    @Override
	public void saveItems(final List<Item> addedItems, final List<Item> modifiedItems, final List<Item> removedItems) {
        if (applicationTransactionManagement) {
            entityManager.getTransaction().begin();
        }
        for (Item item : addedItems) {
        	((BeanTupleItem)item).persist(entityManager);
        }
        for (Item item : modifiedItems) {
        	((BeanTupleItem)item).persist(entityManager);
        }
        for (Item item : removedItems) {
        	((BeanTupleItem)item).remove(entityManager);
        }
        if (applicationTransactionManagement) {
            entityManager.getTransaction().commit();
        }
    }

    /**
     * Removes all items.
     * Query will be discarded after delete all items has been called.
     * @return true if the operation succeeded or false in case of a failure.
     */
    @Override
	public boolean deleteAllItems() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts tuple to Item.
     * The item is filled with default value for each property.
     * 
     * @param tuple to be converted.
     * @return item converted from tuple.
     */
	@SuppressWarnings("unchecked")
    protected Item toItem(final Tuple tuple) {
        BeanTupleItem tupleItem = new BeanTupleItem();
        tupleItem.setTuple(tuple);

        // if tuple did not fill all properties, set default values according to property definitions
        for (Object propertyId : queryDefinition.getPropertyIds()) {
            if (tupleItem.getItemProperty(propertyId) == null) {
                Class<?> propertyType = queryDefinition.getPropertyType(propertyId);
				tupleItem.addItemProperty(
                        propertyId,
                        new ObjectProperty<Object>(
                        		queryDefinition.getPropertyDefaultValue(propertyId),
                        		(Class<Object>) propertyType,
                        		queryDefinition.isPropertyReadOnly(propertyId)));
            }
        }

        return tupleItem;
    }

	
	

    /**
     * @param selectQuery the selectQuery to set
     */
    public void setSelectQuery(TypedQuery<Tuple> selectQuery) {
        this.selectQuery = selectQuery;
    }


    /**
     * @return the selectQuery
     */
    public TypedQuery<Tuple> getSelectQuery() {
        return selectQuery;
    }
}