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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class BeanTupleQueryView implements QueryView, ValueChangeListener {
    
    final private static Logger logger = LoggerFactory.getLogger(BeanTupleQueryView.class);

	private BeanTupleQueryFactory queryFactory;
	private BeanTupleQueryDefinition queryDefinition;
	private LazyQueryView lazyQueryView;
    private Object keyPropertyId;

    private Map<Object,Integer> keyToId = new HashMap<Object,Integer>();
    private int size;

    private boolean initialized;
	

    /**
     * Constructs LazyQueryView with given QueryDefinition and QueryFactory. The
     * role of this constructor is to enable use of custom QueryDefinition
     * implementations.
     * @param queryDefinition The QueryDefinition to be used.
     * @param queryFactory The QueryFactory to be used.
     */
    public BeanTupleQueryView(final BeanTupleQueryDefinition queryDefinition, final BeanTupleQueryFactory queryFactory) {
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
    public Object getKeyPropertyId() {
        return keyPropertyId;
    }


    /**
     * If not null, use the property from the container as itemId
     * If null, use the integer index from the underlying LazyQueryContainer
     * @param keyPropertyId the keyPropertyId to set
     */
    public void setKeyPropertyId(Object keyPropertyId) {
        this.keyPropertyId = keyPropertyId;
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
	public int getBatchSize() {
		return lazyQueryView.getBatchSize();
	}

	/**
	 * Retrieve an item from the container.
	 * <p>
	 * If retrieving by key (setKeyPropertyId() has been called, the item must have been previously loaded.
	 * </p><p>
     * If an int is used as key, then we fallback to the LazyQueryContainer behavior for filling up
     * the cache.
     * </p>.
	 * @param index which item to retrieve
	 * @return the item, if found, or null if not.
	 */
	public Item getItem(Object index) {
        if (!initialized) {
            refresh();
        }
	    if (index.getClass() == int.class) {
	        return getItem(index);
	    } else if (keyPropertyId != null) {
	        // find the id for the property and fetch.
	        Integer intId = keyToId.get(index);
	        if (intId != null) {
	            Item item = lazyQueryView.getItem(intId);
	            return item;
	        } else {
	            return null;
	        }
	    } else {
	        // passed a non-integer but no key property is set.
	        return null;
	    }
	    
	}

    /**
     * @param index which item to retrieve
     * @return the item, if found, or null if not.
     */
    @Override
    public Item getItem(int index) {
        if (!initialized) {
            refresh();
        }
        // standard behavior of getItem.
        Item item = lazyQueryView.getItem((Integer)index);
        keyToId.put(item.getItemProperty(keyPropertyId),(Integer)index);
        return item;
    }

	@Override
	public int hashCode() {
		return lazyQueryView.hashCode();
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
	public String toString() {
		return lazyQueryView.toString();
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
	    if (! initialized) {
	        refresh();
	        size = lazyQueryView.size();
	    }
	    //logger.warn("size = {}",size);
	    return size;
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
        if (!initialized) {
            refresh();
        }
        if (keyPropertyId != null) {
            // we cannot operate in true lazy mode, we need to fetch all the keys from the query.
            // next loop fills the cache and maps; fetching however is done by batches.
            for (int i = 0; i < size();) {
                getItem(i);
                i++;
            }
            return Collections.unmodifiableCollection(keyToId.keySet());
        } else {
            return new NaturalNumbersList(size());
        }
        
    }
	


}
