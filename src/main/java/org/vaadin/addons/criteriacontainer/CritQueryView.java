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

package org.vaadin.addons.criteriacontainer;
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
 */
@SuppressWarnings("serial")
public class CritQueryView<T> implements QueryView, ValueChangeListener {

	private LazyQueryView lazyQueryView;
	private CritQueryFactory<T> queryFactory;
	

    /**
     * Constructs LazyQueryView with given QueryDefinition and QueryFactory. The
     * role of this constructor is to enable use of custom QueryDefinition
     * implementations.
     * @param queryDefinition The QueryDefinition to be used.
     * @param queryFactory The QueryFactory to be used.
     */
    public CritQueryView(final CritQueryDefinition<T> queryDefinition, final CritQueryFactory<T> queryFactory) {
    	this.queryFactory = queryFactory;
        lazyQueryView = new LazyQueryView(queryDefinition, queryFactory);
    }
    

//	/* Modified to use the counting view available through the queryDefinition.
//	 * @see org.vaadin.addons.lazyquerycontainer.QueryView#size()
//	 */
//	@Override
//	public int size() {
//		final EntityManager em = getQueryFactory().getEntityManager();
//		TypedQuery<Long> qd = getQueryDefinition().getCountQuery(em);
//		return qd.getSingleResult().intValue();
//	}

	
	/* ----------------------------------------------------------------------------------------
	 * Additional methods
	 */
	
//	/* Use the counting view available through the queryDefinition.
//	 * @see org.vaadin.addons.lazyquerycontainer.QueryView#size()
//	 */
//	public Long sizeAsLong() {
//		final EntityManager em = getQueryFactory().getEntityManager();
//		TypedQuery<Long> qd = getQueryDefinition().getCountQuery(em);
//		return qd.getSingleResult();
//	}
	
	public void setLazyQueryView(LazyQueryView lazyQueryView) {
		this.lazyQueryView = lazyQueryView;
	}

	public LazyQueryView getLazyQueryView() {
		return lazyQueryView;
	}

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

	@SuppressWarnings("unchecked")
	@Override
	public CritQueryDefinition<T> getQueryDefinition() {
		return (CritQueryDefinition<T>) lazyQueryView.getQueryDefinition();
	}

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
		lazyQueryView.sort(sortPropertyIds, ascendingStates);
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
