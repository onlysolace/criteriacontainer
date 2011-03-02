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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Definition of a JPA 2.0 Criteria Query.
 * 
 * Extends LazyQueryDefinition instead of EntityQueryDefinition to avoid passing the Entity class.
 * 
 * @author jflamy
 * 
 * @param <T> the Entity type returned by the query being defined
 */
public abstract class AbstractCriteriaQueryDefinition<T> implements QueryDefinition {

    @SuppressWarnings("unused")
    final static private Logger logger = LoggerFactory.getLogger(AbstractCriteriaQueryDefinition.class);
	
	/** 
	 * false if the container manages the transactions, true otherwise.
	 * true if running under Jetty or under J2SE 
	 */
	protected boolean applicationManagedTransactions;
	
	/** Batch size of the query. */
    private int batchSize;
	
	/** Default values for the properties. */
    private Map<Object, Object> defaultValues = new HashMap<Object, Object>();
	
	/** the Class for the parameterized type (T.class) */
	protected Class<T> entityClass;
	
	private EntityManager entityManager;
	
	/**
	 * A list of sorting criteria
	 */

	/**
	 * a list of named parameters. Key is the name of the parameter, value is the value to be substituted.
	 */
	protected Map<String, Object> namedParameterValues;

	/** default sort order for each sortable property, true means ascending sort, false means descending sort */
	protected boolean[] nativeSortPropertyAscendingStates;

	/** the default of property ids to be sorted (normally, Strings) */
	protected Object[] nativeSortPropertyIds;

	/** Lust of property IDs included in this QueryDefinition. */
    protected List<Object> propertyIds = null;
	
	/** Map of types of the properties. */
    private Map<Object, Object> propertyTypes = new TreeMap<Object, Object>();

	/** Flags reflecting whether the properties are read only. */
    private Map<Object, Boolean> readOnlyStates = new HashMap<Object, Boolean>();

	/** The sort states of the properties. */
    private Map<Object, Boolean> sortableStates = new HashMap<Object, Boolean>();


	/** actual sort order for each sortable property, true means ascending sort, false means descending sort */
	protected boolean[] sortPropertyAscendingStates;

	
	/** the actual list of property ids to be sorted (normally, Strings) */
	protected Object[] sortPropertyIds;

    /** property ids have been found via query definition */
    protected boolean initialized;

    /**
	 * Simple constructor, used when extending the class.
	 * 
	 * @param entityManager the entityManager that gives us access to the database and cache
	 * @param applicationManagedTransactions true unless the JPA persistence unit is defined by the container
	 * @param entityClass the class for the entity (should be the same as T.class)
	 * @param batchSize how many entities to recover at one time.
	 */
	@SuppressWarnings("unchecked")
    public AbstractCriteriaQueryDefinition(
			EntityManager entityManager,
			boolean applicationManagedTransactions,
			final Class<?> entityClass,
			int batchSize
			) {
		this.batchSize = batchSize;
		this.entityManager = entityManager;
		this.entityClass = (Class<T>) entityClass;
		this.applicationManagedTransactions = applicationManagedTransactions;
	}


	/**
	 * Constructor for simple usage.
	 * With this constructor, entities are retrieved and sorted. 
	 * 
	 * @param entityManager the entityManager that gives us access to the database and cache
	 * @param applicationManagedTransactions true unless the JPA persistence unit is defined by the container
	 * @param entityClass the class for the entity (should be the same as T.class)
	 * @param batchSize how many entities to recover at one time.
	 * @param nativeSortPropertyIds the property names to be sorted
	 * @param nativeSortPropertyAscendingStates for each property name, true means sort in ascending order, false in descending order
	 */
	public AbstractCriteriaQueryDefinition(
			EntityManager entityManager,
			boolean applicationManagedTransactions,
			Class<T> entityClass, 
			int batchSize,
			Object[] nativeSortPropertyIds,
			boolean[] nativeSortPropertyAscendingStates
			) {
		this(entityManager, applicationManagedTransactions, entityClass, batchSize);
        
        this.applicationManagedTransactions = applicationManagedTransactions;
        this.nativeSortPropertyIds = nativeSortPropertyIds;
        this.nativeSortPropertyAscendingStates = nativeSortPropertyAscendingStates;
	}


	

    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#addProperty(java.lang.Object, java.lang.Class, java.lang.Object, boolean, boolean)
     */
    @Override
	public void addProperty(final Object propertyId, Class<?> type, Object defaultValue,
            boolean readOnly, boolean sortable) {
        if (propertyIds == null) {
            propertyIds = new ArrayList<Object>();
        }
        propertyIds.add(propertyId);
        propertyTypes.put(propertyId, type);
        defaultValues.put(propertyId, defaultValue);
        readOnlyStates.put(propertyId, readOnly);
        sortableStates.put(propertyId, sortable);
    }

	/**
	 * Define FROM and WHERE part of query.
	 * 
	 * This definition is shared between the query returning items and the count. That is,
	 * the default implementations of {@link #getCountQuery()} and {@link #getSelectQuery()} both
	 * call this method in order to guarantee that they ar consistent with one another.
	 *
	 * Because of this sharing method must not call cb.select.
	 * The method should not call cb.setOrdering() because only the select method needs it and sorting
	 * has a major performance impact.
	 * 
	 * <p>Advanced usage: If the query needs to perform ordering on elements other than those returned 
	 * (for example, if it needs to sort on the columns of a join) then this method and {@link #getOrdering(Path, CriteriaBuilder)}
	 * will both need to be overridden in such a way that the {@link Join} in the query are visible to both methods.
	 * In other words, the joins should be defined as {@link Join} fields, set by defineQuery, and accessed by getOrdering.</p>
	 * 
	 * @param cb the current query builder
	 * @param cq the query as built so far
	 * @return the root for the query
	 */
	abstract protected Root<?> defineQuery(CriteriaBuilder cb, CriteriaQuery<?> cq);
 

    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#getBatchSize()
     */
    @Override
	public int getBatchSize() {
        return batchSize;
    }


	/**
	 * This method returns the number of entities.
	 * @return number of entities.
	 */
	abstract public TypedQuery<Long> getCountQuery() ;
	
	/**
	 * refresh the query.
	 */
	abstract public void refresh();

	/**
	 * Get the class of the entity being managed.
	 * 
	 * @return the Class for the Entity being returned.
	 */
	public Class<?> getEntityClass() {
		return entityClass;
	}

	/**
	 * @return the entity manager used for the definition
	 */
	public EntityManager getEntityManager() {
		return entityManager;
	}

	
	/** methods defined by interfaces, delegated to the wrapped LazyQueryD =============================*/

    /**
     * @return the whereParameters
     */
    public Map<String, Object> getNamedParameterValues() {
    	return namedParameterValues;
    }
    /**
	 * Applies the sort state.
	 * A JPA ordering is created based on the saved sort orders.
	 * 
	 * @param t the root or joins from which columns are being selected
	 * @param cb the criteria builder for the query being built
	 * @return a list of Order objects to be added to the query.
	 */
	protected List<Order> getOrdering(Path<?> t, CriteriaBuilder cb) {
        if (sortPropertyIds == null || sortPropertyIds.length == 0) {
            sortPropertyIds = nativeSortPropertyIds;
            sortPropertyAscendingStates = nativeSortPropertyAscendingStates;
        }
        
        ArrayList<Order> ordering = new ArrayList<Order>();
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

	
    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#getPropertyDefaultValue(java.lang.Object)
     */
    @Override
	public Object getPropertyDefaultValue(final Object propertyId) {
        return defaultValues.get(propertyId);
    }

    
    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#getPropertyIds()
     */
    @Override
	public Collection<?> getPropertyIds() {
        if (! initialized) {
            refresh();
        }
        return Collections.unmodifiableCollection(propertyIds);
    }

    
    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#getPropertyType(java.lang.Object)
     */
    @Override
	public Class<?> getPropertyType(final Object propertyId) {
        return (Class<?>) propertyTypes.get(propertyId);
    }
    
    
    /**
	 * This method adds the ordering and the parameters on a query definition.
	 * 
	 * {@link #defineQuery(CriteriaBuilder, CriteriaQuery)} creates the portion of
	 * the query that is shared between counting and retrieving. This method returns
	 * a runnable query by adding the ordering and setting the parameters.
	 * @return a runnable TypedQuery
	 */
	abstract public TypedQuery<T> getSelectQuery();

	
    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#getSortablePropertyIds()
     */
    @Override
	public Collection<?> getSortablePropertyIds() {
        List<Object> sortablePropertyIds = new ArrayList<Object>();
        for (Object propertyId : propertyIds) {
            if (isPropertySortable(propertyId)) {
                sortablePropertyIds.add(propertyId);
            }
        }
        return sortablePropertyIds;
    }

    /**
	 * @return false if the J2EE container manages the transactions, true if done manually
	 */
	public boolean isApplicationManagedTransactions() {
		return applicationManagedTransactions;
	}
	

    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#isPropertyReadOnly(java.lang.Object)
     */
    @Override
	public boolean isPropertyReadOnly(final Object propertyId) {
        return readOnlyStates.get(propertyId);
    }


    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#isPropertySortable(java.lang.Object)
     */
    @Override
	public boolean isPropertySortable(final Object propertyId) {
        return sortableStates.get(propertyId);
    }


    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#removeProperty(java.lang.Object)
     */
    @Override
	public void removeProperty(final Object propertyId) {
        propertyIds.remove(propertyId);
        propertyTypes.remove(propertyId);
        defaultValues.remove(propertyId);
        readOnlyStates.remove(propertyId);
        sortableStates.remove(propertyId);
    }
    

    /**
     * After this method has been called the Query has to be discarded immediately.
     * @param batchSize the batchSize to set
     */
    @Override
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }


	
    /**
	 * Sets named parameter values
	 * 
	 * @param namedParameterValues
	 *            For each pair, the key is the name of the parameter, and the value is the value to
	 *            set for that parameter.
	 */
	public void setNamedParameterValues(final Map<String, Object> namedParameterValues) {
		this.namedParameterValues = namedParameterValues;
	}

	
    /**
	 * Set parameters values.
	 * 
	 * Provide values for the named parameters defined via{@link #setNamedParameterValues(java.util.Map)}.
	 * This method is overridden by subclasses that define additional parameters.  Subclasses should
	 * call super.setParameters to ensure that setNamedParameterValues work.
	 * 
	 * @param tq the runnable query that needs to have its parameter set
	 * @return the query after its parameters have been set.
	 */
	protected TypedQuery<?> setParameters(TypedQuery<?> tq) {
        if (namedParameterValues != null) {
            for (String parameterKey : namedParameterValues.keySet()) {
            	tq.setParameter(parameterKey, namedParameterValues.get(parameterKey));
            }
        }
		return tq;
	}
	
	

    /**
	 * Sets the sort state.
	 * 
	 * @param sortPropertyIds
	 *            Properties participating in the sorting.
	 * @param sortPropertyAscendingStates
	 *            List of sort direction for the properties.
	 */
	public void setSortState(final Object[] sortPropertyIds,
			final boolean[] sortPropertyAscendingStates) {
		this.sortPropertyIds = sortPropertyIds;
		this.sortPropertyAscendingStates = sortPropertyAscendingStates;
	}}
