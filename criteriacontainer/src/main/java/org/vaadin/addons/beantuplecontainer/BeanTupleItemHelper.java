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
    private TypedQuery<Object> selectCountQuery;
    
    /** QueryDefinition contains definition of the query properties and batch size. */
    protected BeanTupleQueryDefinition queryDefinition;
    
    /** The size of the query. */
    private int querySize = -1;


    /** Cache from keys to container index. */
    protected KeyManager keyToIdMapper;


    /**
     * Constructor for configuring the query.
     * @param criteriaQueryDefinition The entity query definition..
     * @param keyToIdMapper Holds cache id to key mappings.
     */
    public BeanTupleItemHelper(final BeanTupleQueryDefinition criteriaQueryDefinition, KeyManager keyToIdMapper) {
        this.queryDefinition = criteriaQueryDefinition;
        this.keyToIdMapper = keyToIdMapper;
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
     * <p>
     * This method is called extremely often.  We store the result to avoid 
     * invoking the query.
     * </p>
     * 
     * @return number of beans.
     */
    @Override
	public int size() {
        if (getQuerySize() == -1) {
            setQuerySize(((Number) selectCountQuery.getSingleResult()).intValue());
        }
        return getQuerySize();
    }

    /**
     * Load batch of items.
     * <p>
     * This is the only location where we actually retrieve items from
     * the database.
     * </p>
     * @param startIndex Starting index of the item list.
     * @param count Count of the items to be retrieved.
     * @return List of items.
     */
    @Override
	public List<Item> loadItems(final int startIndex, final int count) {
        List<Item> items = new ArrayList<Item>();
        if (count <= 0) {
            return items;
        }

        adjustRetrievalBoundaries(startIndex, count);
        
        Object keyPropertyId = keyToIdMapper.getKeyPropertyId();
        
        List<?> tuples = selectQuery.getResultList();
        int curCount = 0;
        for (Object tuple : tuples) {
            Item item = toItem((Tuple) tuple);
            
            //iterate over entities in the tuple.
            if (queryDefinition.isDetachedEntities()) {
            	((BeanTupleItem)item).detach(entityManager);
            }

            items.add(item);
            addToMapping(item, keyPropertyId, startIndex+curCount);
            curCount++;
        }
        return items;
    }


    /**
     * Call back to the cache(s).
     * <p>The view can maintain a cache or mappings of what it has retrieved. In
     * the current implementation, we map the keys to their corresponding index
     * in the container</p>
     * 
     * @param item the item being added
     * @param keyPropertyId the key being used (the real key from the database)
     * @param index the index in the container 
     */
    protected void addToMapping(Item item, Object keyPropertyId, final int index) {
        if (keyPropertyId != null) {
            Object value = item.getItemProperty(keyPropertyId).getValue();
            keyToIdMapper.getKeyToId().put(value, index);
        } else {
            // identity mapping, as precaution
            keyToIdMapper.getKeyToId().put(index, index);
        }
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
        try {
            for (Item item : addedItems) {
                if (!removedItems.contains(item)) {
                    ((BeanTupleItem)item).persist(entityManager);
                }
            }
            for (Item item : modifiedItems) {
                if (!removedItems.contains(item)) {
                    if (queryDefinition.isDetachedEntities()) {
                        ((BeanTupleItem)item).merge(entityManager);
                    }
                    ((BeanTupleItem)item).persist(entityManager);
                }
            }
            for (Item item : removedItems) {
                if (!addedItems.contains(item)) {
                    if (queryDefinition.isDetachedEntities()) {
                        ((BeanTupleItem)item).merge(entityManager);
                    }
                    ((BeanTupleItem)item).remove(entityManager);
                }
            }
            if (applicationTransactionManagement) {
                entityManager.getTransaction().commit();
            }
        } catch (Exception e) {
            if (applicationTransactionManagement) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
            }
            throw new RuntimeException(e);            
        }
        
        // invalidate the query size
        setQuerySize(-1);
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
    
    /**
     * @return the querySize
     */
    protected int getQuerySize() {
        return querySize;
    }


    /**
     * @param querySize the querySize to set
     */
    protected void setQuerySize(int querySize) {
        this.querySize = querySize;
    }


	/**
	 * Compute lower bound and number of tuples to retrieve.
	 * If batching is used, the bounds are adjusted to fit on batch sizes: 
	 * for example for getBatchSize() = 100, and a call with startIndex = 110, count = 100: we need at least 110 to 209.
	 * Aligning will yield batchLowBoundary = 100, and count = 200, and will retrieve 100 to 299,
	 * 
	 * @param startIndex starting position for retrieval
	 * @param count how many items to retrieve
	 */
	protected void adjustRetrievalBoundaries(final int startIndex, final int count) {
		if (KeyManager.USE_BATCHING) {
	    	int batchSize = keyToIdMapper.getBatchSize();
	    	int batchLowBoundary = (int)(Math.floor(startIndex/batchSize)) * batchSize;
	    	getSelectQuery().setFirstResult(batchLowBoundary);
	    	getSelectQuery().setMaxResults((batchLowBoundary == startIndex) ? batchSize : batchSize * 2);
	    } else {
	    	getSelectQuery().setFirstResult(startIndex);
	    	getSelectQuery().setMaxResults(count);        	
	    }
	}
}