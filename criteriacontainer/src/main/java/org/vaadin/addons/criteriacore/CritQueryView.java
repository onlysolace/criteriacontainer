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
import org.vaadin.addons.lazyquerycontainer.LazyQueryView;
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
 * @param <T> the type of entity (class annotated by @Entity returned by the Query
 *
 */
@SuppressWarnings("serial")
public class CritQueryView<T> implements QueryView, ValueChangeListener {

	private LazyQueryView lazyQueryView;
	private CritQueryFactory<T> queryFactory;
	private CritQueryDefinition<T> queryDefinition;
	

    /**
     * Constructs LazyQueryView with given QueryDefinition and QueryFactory. The
     * role of this constructor is to enable use of custom QueryDefinition
     * implementations.
     * @param queryDefinition The QueryDefinition to be used.
     * @param queryFactory The QueryFactory to be used.
     */
    public CritQueryView(final CritQueryDefinition<T> queryDefinition, final CritQueryFactory<T> queryFactory) {
    	this.queryFactory = queryFactory;
    	this.queryDefinition = queryDefinition;
        lazyQueryView = new LazyQueryView(queryDefinition, queryFactory);
    }

	
	/* ----------------------------------------------------------------------------------------
	 * Additional methods
	 */
	
	/**
	 * @return the query factory that creates the views.
	 */
	public CritQueryFactory<?> getQueryFactory() {
		return queryFactory;
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
		lazyQueryView.discard();
	}

	@Override
	public boolean equals(Object obj) {
		return lazyQueryView.equals(obj);
	}

	@Override
	public CritQueryDefinition<T> getQueryDefinition() {
		return queryDefinition;
	}

	/**
	 * @return the number of instances retrieved each time
	 */
	public int getBatchSize() {
		return lazyQueryView.getBatchSize();
	}

	@Override
	public Item getItem(int index) {
		return lazyQueryView.getItem(index);
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
		lazyQueryView.refresh();
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
		return lazyQueryView.size();
	}
	

}
