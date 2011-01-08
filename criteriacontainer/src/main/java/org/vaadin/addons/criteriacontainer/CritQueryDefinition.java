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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;

/**
 * Definition of a JPA 2.0 Criteria Query.
 * 
 * Extends LazyQueryDefinition instead of EntityQueryDefinition to avoid passing the Entity class.
 * 
 * @author jflamy
 * 
 * @param <T> the Entity type returned by the query being defined
 */
@SuppressWarnings("serial")
public class CritQueryDefinition<T> extends LazyQueryDefinition {

	protected Class<T> entityClass;
	protected boolean applicationManagedTransactions;
	protected Object[] nativeSortPropertyIds;
	protected boolean[] nativeSortPropertyAscendingStates;
	protected Object[] sortPropertyIds;
	protected boolean[] sortPropertyAscendingStates;
	protected Map<String, Object> namedParameterValues;
	
	private ArrayList<Order> ordering;
	private List<Predicate> filterExpressions;
	private Collection<CritRestriction> restrictions;
	
	@SuppressWarnings("unused")
	final static private Logger logger = LoggerFactory.getLogger(CritQueryDefinition.class);


	
	/**
	 * Constructor for simple usage.
	 * With this constructor, entities are retrieved and sorted.  The container's {@link CriteriaContainer#filter(java.util.LinkedList)}
	 * is used to restrict information.
	 * 
	 * @param applicationManagedTransactions true unless the JPA persistence unit is defined by the container
	 * @param entityClass the class for the entity (should be the same as T when the class is instanciated)
	 * @param batchSize how many entities to recover at one time.
	 * @param nativeSortPropertyIds the property names to be sorted
	 * @param nativeSortPropertyAscendingStates for each property name, true means sort in ascending order, false in descending order
	 */
	public CritQueryDefinition(
			boolean applicationManagedTransactions,
			Class<T> entityClass, 
			int batchSize,
			Object[] nativeSortPropertyIds,
			boolean[] nativeSortPropertyAscendingStates
			) {
		this(applicationManagedTransactions, entityClass, batchSize);
        
        this.applicationManagedTransactions = applicationManagedTransactions;
        this.nativeSortPropertyIds = nativeSortPropertyIds;
        this.nativeSortPropertyAscendingStates = nativeSortPropertyAscendingStates;
	}
	
	/**
	 * Simple constructor, used when extending the class.
	 * 
	 * @param applicationManagedTransactions true unless the JPA persistence unit is defined by the container
	 * @param entityClass the class for the entity (should be the same as T when the class is instanciated)
	 * @param batchSize how many entities to recover at one time.
	 */
	public CritQueryDefinition(
			boolean applicationManagedTransactions,
			final Class<T> entityClass,
			int batchSize
			) {
		super(batchSize);
		this.entityClass = entityClass;
		this.applicationManagedTransactions = applicationManagedTransactions;
	}

	/**
	 * This method returns the number of entities.
	 * @param em the entity manager for the current query
	 * @return number of entities.
	 */
	public TypedQuery<Long> getCountQuery(EntityManager em) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
    	CriteriaQuery<Long> nb = cb.createQuery(Long.class);
		Root<T> t = defineQuery(cb, nb);
		nb.select(cb.count(t));
		final TypedQuery<Long> countQuery = em.createQuery(nb);
		setParameters(countQuery);
		return countQuery;
	}
	
	/**
	 * Get the class of the entity being managed.
	 * 
	 * @return the Class for the Entity being returned.
	 */
	public Class<T> getEntityClass() {
		return entityClass;
	}

	/**
	 * Create the Predicates that match the filters requested through {@link #setFilterExpressions(ArrayList)}
	 * 
	 * @return a list of Predicates that corresponds to the declared filters.
	 */
	public List<Predicate> getFilterExpressions() {
		return filterExpressions;
	}


	/**
	 * Applies the sort state.
	 * A JPA ordering is created based on the saved sort orders.
	 * 
	 * @param t the root or joins from which columns are being selected
	 * @param cb the criteria builder for the query being built
	 * @return a list of Order objects to be added to the query.
	 */
	public final List<Order> getOrdering(Path<T> t, CriteriaBuilder cb) {
        if (sortPropertyIds == null || sortPropertyIds.length == 0) {
            sortPropertyIds = nativeSortPropertyIds;
            sortPropertyAscendingStates = nativeSortPropertyAscendingStates;
        }
        
        ordering = new ArrayList<Order>();
    	if (sortPropertyIds == null || sortPropertyIds.length == 0) return ordering;
    	
		for (int curItem = 0; curItem < sortPropertyIds.length; curItem++ ) {
	    	final String id = (String)sortPropertyIds[curItem];
			if (sortPropertyAscendingStates[curItem]) {
				ordering.add(cb.asc(t.get(id)));
			} else {
				ordering.add(cb.desc(t.get(id)));
			}
		}
		return ordering;
	}

	
	/**
	 * This method adds the ordering and the parameters on a query definition.
	 * 
	 * {@link #defineQuery(CriteriaBuilder, CriteriaQuery)} creates the portion of
	 * the query that is shared between counting and retrieving. This method returns
	 * a runnable query by adding the ordering and setting the parameters.
	 * 
	 * @param em the entity manager for the current query
	 * @return a runnable TypedQuery
	 */
	public TypedQuery<T> getSelectQuery(EntityManager em) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
    	CriteriaQuery<T> cq = cb.createQuery(getEntityClass());
		Root<T> t = defineQuery(cb, cq);
		final List<Order> ordering = getOrdering(t, cb);
		if (ordering != null) {
			cq.orderBy(ordering);
		}
		final TypedQuery<T> tq = em.createQuery(cq);
		// the container deals with the parameter values set through the filter() method
		// so we only handle those that we add ourselves
		setParameters(tq);
		return tq;
	}

    /**
     * @return the whereParameters
     */
    public final Map<String, Object> getNamedParameterValues() {
    	return namedParameterValues;
    }


	/**
	 * @return the applicationManagedTransactions
	 */
	public final boolean isApplicationManagedTransactions() {
		return applicationManagedTransactions;
	}

	
	/**
	 * Define a list of conditions to be added to the query's WHERE clause
	 * 
	 * @param filterExpressions a list of Predicate objects to be added
	 */
	public void setFilterExpressions(ArrayList<Predicate> filterExpressions) {
		this.filterExpressions = filterExpressions;
	}
	
	/**
	 * Sets the sort state.
	 * 
	 * @param sortPropertyIds
	 *            Properties participating in the sorting.
	 * @param sortPropertyAscendingStates
	 *            List of sort direction for the properties.
	 */
	public final void setSortState(final Object[] sortPropertyIds,
			final boolean[] sortPropertyAscendingStates) {
		this.sortPropertyIds = sortPropertyIds;
		this.sortPropertyAscendingStates = sortPropertyAscendingStates;
	}

	/**
	 * Sets named parameter values
	 * 
	 * @param namedParameterValues
	 *            For each pair, the key is the name of the parameter, and the value is the value to
	 *            set for that parameter.
	 */
	public final void setNamedParameterValues(final Map<String, Object> namedParameterValues) {
		this.namedParameterValues = namedParameterValues;
	}

	/**
	 * Store a list of restrictions.
	 * Each restriction will be transformed into a predicate added to the WHERE clause.
	 * 
	 * @param restrictions a list of objects that each define a condition to be added
	 */
	public final void setRestrictions(Collection<CritRestriction> restrictions) {
		this.restrictions = restrictions;
	}
	
	/**
	 * Prepare the query so that {@link CriteriaContainer#filter(Map)} works.
	 * 
	 * For each field named in the whereParameters map, create a parameter place holder
	 * in the query.  There is no setFilterParameters method, the container does the
	 * processing in the filter() method.
	 * @param filterExpressions 
	 * 
	 * @param cb
	 * @param cq
	 * @param t
	 * @return 
	 */
	protected List<Predicate> addFilterRestrictions(List<Predicate> filterExpressions, CriteriaBuilder cb,
			CriteriaQuery<?> cq, Root<T> t) {
		if (restrictions != null) {
			filterExpressions.add(CritRestriction.getPredicate(restrictions, cb, t));
		}
		return filterExpressions;
	}


	/**
	 * Create conditions in the query
	 * 
	 * @param filterExpressions2 
	 * 			a list of Predicate objects. Note: you may use the cb.parameter() method
	 * 			to create additional parameters.
	 * @param cb
	 * @param cq
	 * @param t
	 * @return 
	 */
	protected List<Predicate> addPredicates(List<Predicate> filterExpressions, CriteriaBuilder cb, CriteriaQuery<?> cq, Root<T> t) {
		// do nothing, will be overridden by classes that need it.
		return filterExpressions;
	}

	/**
	 * Define FROM and WHERE part of query.
	 * This definition is shared between the query returning items and the count.
	 * Must call nb.from(), must call nb.where() for the parameter place holders.
	 * 
	 * @param cb
	 * @param nb
	 * @return
	 */
	protected Root<T> defineQuery(CriteriaBuilder cb, CriteriaQuery<?> cq) {
		Root<T> t = cq.from(getEntityClass());
		filterExpressions = new ArrayList<Predicate>();

		// predicates built by the query itself (typically, type-safe predicates)
		filterExpressions = addPredicates(filterExpressions,cb, cq, t);
		
		// predicates created from CriteriaContainer.filter()
		filterExpressions = addFilterRestrictions(filterExpressions, cb, cq, t);
		
		// peculiar call to toArray with argument is required to cast all the elements.
		final Predicate[] array = getFilterExpressions().toArray(new Predicate[]{});
		
		// must call where() exactly once.  This call to where() expects a sequence of
		// Predicates. Java accepts an array instead.
		cq.where(array);
		return t;
	}

	/**
	 * Set parameters values.
	 * 
	 * Provide values for the named parameters defined via{@link #setNamedParameterValues(java.util.Map)}.
	 * This method is overridden by subclasses that define additional parameters.
	 * 
	 * @param tq
	 */
	protected TypedQuery<?> setParameters(TypedQuery<?> tq) {
        if (namedParameterValues != null) {
            for (String parameterKey : namedParameterValues.keySet()) {
            	tq.setParameter(parameterKey, namedParameterValues.get(parameterKey));
            }
        }
		return tq;
	}
	

	
}
