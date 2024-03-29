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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacontainer.CriteriaQueryDefinition;
import org.vaadin.addons.criteriacontainer.CriteriaQueryView;
import org.vaadin.addons.lazyquerycontainer.LazyQueryView;
import org.vaadin.addons.lazyquerycontainer.NaturalNumbersList;
import org.vaadin.addons.lazyquerycontainer.QueryView;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;


/**
 * Wrapper around the LazyQueryView.
 * 
 * For some reason, LazyQueryView was defined as final.
 * 
 * @author jflamy
 * 
 */
@SuppressWarnings("serial")
public class BeanTupleQueryView implements QueryView, ValueChangeListener, KeyManager {
    
    @SuppressWarnings("unused")
    final private static Logger logger = LoggerFactory.getLogger(BeanTupleQueryView.class);

	private BeanTupleQueryFactory queryFactory;
	private BeanTupleQueryDefinition queryDefinition;
	private LazyQueryView lazyQueryView;
	
    private Object keyPropertyId;
    private Map<Object,Integer> keyToId = new HashMap<Object,Integer>();

    private int size;

    private boolean initialized = false;
	

    /**
     * Constructs LazyQueryView with given QueryDefinition and QueryFactory. 
     * BeanTupleQueryView delegates as much as it can to a wrapped LazyQueryView, hence
     * the need to conform to the constructor of the underlying implementation.
     * 
     * @param queryDefinition The QueryDefinition to be used (will be injected in queryFactory)
     * @param queryFactory The QueryFactory to be used.
     */
    public BeanTupleQueryView(final BeanTupleQueryDefinition queryDefinition, final BeanTupleQueryFactory queryFactory) {
        if (queryDefinition instanceof CriteriaQueryDefinition && !(this instanceof CriteriaQueryView)) {
            throw new javax.persistence.PersistenceException("Using "+CriteriaQueryDefinition.class.getSimpleName()+" to initialize a "+this.getClass().getSimpleName());
        }
    	this.queryFactory = queryFactory;
    	this.queryDefinition = queryDefinition;
        this.lazyQueryView = new LazyQueryView(queryDefinition, queryFactory);
    }

	
	/* ----------------------------------------------------------------------------------------
	 * Additional methods
	 */
	
	/**
	 * @return the query factory that creates the views.
	 */
	public BeanTupleQueryFactory getQueryFactory() {
		return queryFactory;
	}
	
    /** Property Id used as item identifier 
     * @return the keyPropertyId, or null if the default int index is used.
     */
    @Override
    public Object getKeyPropertyId() {
        return keyPropertyId;
    }


    /**
     * If not null, use the property from the container as itemId
     * If null, use the integer index from the underlying LazyQueryContainer
     * @param keyPropertyId the keyPropertyId to set
     */
    public void setKeyPropertyId(Object keyPropertyId) {
        queryDefinition.init();
        if (queryDefinition.getPropertyIds().contains(keyPropertyId)) {
            this.keyPropertyId = keyPropertyId;    
        } else {
            throw new javax.persistence.PersistenceException("Query does not define property "+keyPropertyId);
        }
        
    }

	
	/* ----------------------------------------------------------------------------------------
	 * Methods below are simply delegates
	 */

	@Override
	public void commit() {
		lazyQueryView.commit();
	}

	@Override
	public void discard() {
	    keyToId.clear();
		lazyQueryView.discard();
	}

	@Override
	public boolean equals(Object obj) {
		return lazyQueryView.equals(obj);
	}

	@Override
	public BeanTupleQueryDefinition getQueryDefinition() {
		return queryDefinition;
	}

	/**
	 * @return the number of instances retrieved each time
	 */
	@Override
	public int getBatchSize() {
		return lazyQueryView.getBatchSize();
	}

	/**
	 * Retrieve an item from the container.
	 * <p>
	 * If retrieving by key (i.e. setKeyPropertyId() has been called), the item must have been previously loaded.
	 * </p><p>
     * If an int is used as key, then we fallback to the LazyQueryContainer behavior for filling up
     * the cache.
     * </p>.
	 * @param id which item to retrieve
	 * @return the item, if found, or null if not.
	 */
	public Item getItem(Object id) {
	    init();
	    if (id == null) return null;

	    if (getKeyPropertyId() != null) {
	        return keyedAccess(id);
        } else {
            return indexedAccess(id);
        }
	}


    /**
     * @param index
     * @return
     */
    private Item indexedAccess(Object index) {
        // key property not set, expecting an integer
        Class<?> clasz = index.getClass();
        if (clasz == int.class)  {
            int intIndex = ((Integer) index).intValue();
            return getItem(intIndex);
        } else if (clasz == Integer.class && getKeyPropertyId() == null) {
            int intIndex = ((Integer) index).intValue();
            return getItem(intIndex);
        } else {
            // passed a non-integer but no key property is set.
            return null;
        }
    }


    /**
     * @param keyOrIndex int index or Object key being sought.
     * @return
     */
    private Item keyedAccess(Object keyOrIndex) {
        // key property is set, expecting an int for indexed access
        // or a non-int for access according to key.
        Class<?> clasz = keyOrIndex.getClass();
        if (clasz == int.class)  {
            // fetch directly by index
            int intIndex = ((Integer) keyOrIndex).intValue();
            return getItem(intIndex);
        } else {
            // consider that id is a key -- typical case for auto-increment database key
            Integer intId = keyToId.get(keyOrIndex);
            if (intId != null) {
                // we must use the int value otherwise we create a loop.
                Item item = lazyQueryView.getItem(intId.intValue());
                return item;
            } else {
                return null;
            }
        }
    }

    /**
     * @param index which item to retrieve
     * @return the item, if found, or null if not.
     */
    @Override
    public Item getItem(int index) {
        init();
        // standard behavior of getItem.
        Item item = lazyQueryView.getItem(index);
        return item;
    }


	@Override
	public void sort(Object[] sortPropertyIds, boolean[] ascendingStates) {
		queryDefinition.setSortState(sortPropertyIds, ascendingStates);
        refresh();
	}

	@Override
	public void refresh() {
	    queryDefinition.refresh();
		lazyQueryView.refresh();
		size = lazyQueryView.size();
		initialized = true;
	}


	@Override
	public void removeItem(int index) {
		lazyQueryView.removeItem(index);
	}

	@Override
	public void removeAllItems() {
		lazyQueryView.removeAllItems();
	}

	@Override
	public boolean isModified() {
		return lazyQueryView.isModified();
	}

	@Override
	public void valueChange(ValueChangeEvent event) {
		lazyQueryView.valueChange(event);
	}

	@Override
	public int addItem() {
		return lazyQueryView.addItem();
	}


	@Override
	public int size() {
	    init();
	    return size;
	}


    /**
     * Initialization.
     */
    private void init() {
        if (! initialized) {
	        queryDefinition.init();
	        size = lazyQueryView.size();
	        initialized = true;
	    }
    }


    @Override
    public int getMaxCacheSize() {
        return lazyQueryView.getMaxCacheSize();
    }


    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryView#setMaxCacheSize(int)
     */
    @Override
    public void setMaxCacheSize(int maxCacheSize) {
        lazyQueryView.setMaxCacheSize(maxCacheSize);
    }


    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryView#getAddedItems()
     */
    @Override
    public List<Item> getAddedItems() {
        return lazyQueryView.getAddedItems();
    }


    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryView#getModifiedItems()
     */
    @Override
    public List<Item> getModifiedItems() {
        return lazyQueryView.getModifiedItems();
    }


    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryView#getRemovedItems()
     */
    @Override
    public List<Item> getRemovedItems() {
        return lazyQueryView.getRemovedItems();
    }


    /**
     * @return the ids for the items in the list.
     */
	public Collection<?> getItemIds() {
        init();
        if (getKeyPropertyId()!= null) {
            // itemId is an arbitrary attribute, so we must fetch it.
            // we have no way to tell whether calling application just needs the itemIds of what
        	// has been loaded so far, or itemIds for the whole collection.  So we load all.
        	if (keyToId.keySet().size() != size) {
        		// map is incomplete, so we fill it.
        		Collection<Integer> knownValues = keyToId.values();
                for (int i = 0; i < size;) {
                	// fetching from database is expensive so we avoid it.
                	if (!knownValues.contains(i)) {
                		getItem((int) i); // sequential access according to container order.
                	}
                	if (KeyManager.USE_BATCHING) {
                		// loadItems forced loading to align on multiples of batch size.
                		i+= getBatchSize();
                	} else {
                		i++;
                	}
                }
        	}

            Collection<Object> unmodifiableCollection = Collections.unmodifiableCollection(keyToId.keySet());
            return unmodifiableCollection;
        } else {
            return new NaturalNumbersList(size);            
        }
        
    }


    /**
     * @param index the index inside the container.
     * @return the Id associated with container item index
     */
    public Object getIdByIndex(int index) {
        init();
        if (getKeyPropertyId() != null) {
            Item item = getItem(index);
            if (item != null) {
                return item.getItemProperty(getKeyPropertyId()).getValue();
            } else {
                return null;
            }
        } else {
            return index;
        }
    }
    

    /**
     * @param itemId the key for the item sought
     * @return the index for the sought item, or -1 if not found.
     */
    public int getIndex(Object itemId) {
        init();
        if (itemId == null) return -1;
        
        if (getKeyPropertyId() != null) {
            Integer value = keyToId.get(itemId);
            if (value == null) {
                return -1;
            } else {
                return value;
            }
        } else if (itemId.getClass() == Integer.class) {
            return ((Integer) itemId).intValue();
        } if (itemId.getClass() == int.class) {
            // probably can't happen (due to signature, we
            // will get an Integer instead)
            return ((Integer) itemId).intValue();
        } else {
            return -1;
        }
    }
    
    /**
     * @param itemId the int index for the item sought
     * @return the index for the sought item, or -1 if not found.
     */
    public int getIndex(int itemId) {
        init();
        if (itemId < 0 || itemId >= size()) {
            return -1;
        } else {
            return itemId;
        }
    }


    /**
     * @param itemId the item being sought
     * @return true if present -- also true if removed and not yet committed.
     */
    public boolean containsId(Object itemId) {
        init();
        if (getKeyPropertyId() != null) {
            return null != keyToId.get(itemId);
        } else if (itemId.getClass() == Integer.class) {
            return size() > (Integer) itemId && (Integer) itemId >= 0;
        } else {
            return false;
        }
    }

    /**
     * @return the keyToId
     */
    @Override
    public Map<Object, Integer> getKeyToId() {
        return keyToId;
    }
}
