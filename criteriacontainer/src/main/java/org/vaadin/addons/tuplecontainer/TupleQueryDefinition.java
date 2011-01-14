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
package org.vaadin.addons.tuplecontainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import org.vaadin.addons.criteriacontainer.CritQueryDefinition;

/**
 * Type-safe implementation of a query definition.
 * 
 * Uses JPA2.0 Criteria mechanisms to create a safe version of the query that can be validated
 * at compile time.
 * 
 * This version is the most straightforward usage, and is useful if the filter() method is not used.
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public abstract class TupleQueryDefinition extends CritQueryDefinition<Tuple> {

	/**
	 * Map from a property name to the expression that is used to set the property.
	 * Each item property is retrieved by an expression in a {@link CriteriaQuery#multiselect(List)}
	 * In order to be able to sort, we need to create an {@link Order} using that expression, so
	 * we must memorize the correspondence between propertyIds and the expression that fetch
	 * item properties for that id.
	 */
	protected Map<Object,Expression<?>> sortExpressions = new HashMap<Object, Expression<?>>();


	/**
	 * Constructor.
	 * @param entityManager the entityManager that gives us access to the database and cache
	 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
	 * @param batchSize how many tuples to retrieve at once.
	 */
	public TupleQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
		// We pass Task.class because the parameterized type of this class is <Task>
		super(entityManager, applicationManagedTransactions, Tuple.class, batchSize);
	}
	

	@Override
	public TypedQuery<Tuple> getSelectQuery() {
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    	CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    	
    	defineQuery(cb, cq, sortExpressions);
		
		// apply the ordering defined by the container on the returned entity.
		final List<Order> ordering = getTupleOrdering(cb);
		if (ordering != null) {
			cq.orderBy(ordering);
		}		
		
		final TypedQuery<Tuple> tq = getEntityManager().createQuery(cq);
		// the container deals with the parameter values set through the filter() method
		// so we only handle those that we add ourselves
		setParameters(tq);
		return tq;
	}

	
	/**
	 * Define the common part of the Query to be used for counting and selecting.
	 * Also memorizes the parts of the query that can be used for sorting.
	 * 
	 * @param cb the CriteriaBuilder used to define the from, where and select
	 * @param cq the CriteriaQuery to build the tuples,
	 * @param sortExpressions a map from a propertyId to the expression used by the CriteriaQuery to set its value
	 * @return the root of the query (used for counting)
	 */
	protected abstract Root<?> defineQuery(CriteriaBuilder cb, CriteriaQuery<Tuple> cq, Map<Object,Expression<?>> sortExpressions);
	
	
	/* (non-Javadoc)
	 * @see org.vaadin.addons.criteriacontainer.CritQueryDefinition#defineQuery(javax.persistence.criteria.CriteriaBuilder, javax.persistence.criteria.CriteriaQuery)
	 */
	@SuppressWarnings("unchecked")
	@Override
	final protected Root<?> defineQuery(CriteriaBuilder cb, CriteriaQuery<?> cq) {
		return defineQuery(cb, (CriteriaQuery<Tuple>) cq, sortExpressions);
	}

	/**
	 * Applies the sort state.
	 * A JPA ordering is created based on the saved sort orders.
	 * 
	 * @param cb the criteria builder for the query being built
	 * @return a list of Order objects to be added to the query.
	 */
	protected List<Order> getTupleOrdering(CriteriaBuilder cb) {
        if (sortPropertyIds == null || sortPropertyIds.length == 0) {
            sortPropertyIds = nativeSortPropertyIds;
            sortPropertyAscendingStates = nativeSortPropertyAscendingStates;
        }
  
        ArrayList<Order> ordering = new ArrayList<Order>();
    	if (sortPropertyIds == null || sortPropertyIds.length == 0) return ordering;

		for (int curItem = 0; curItem < sortPropertyIds.length; curItem++ ) {
	    	final String id = (String)sortPropertyIds[curItem];
			final Expression<?> sortExpression = sortExpressions.get(id);
			if (sortExpression != null && sortPropertyAscendingStates[curItem]) {
				ordering.add(cb.asc(sortExpression));
			} else {
				ordering.add(cb.desc(sortExpression));
			}
		}
		return ordering;
	}




}

