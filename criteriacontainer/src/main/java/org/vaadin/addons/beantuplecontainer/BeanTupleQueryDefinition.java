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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacontainer.CritQueryDefinition;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Type-safe implementation of a query definition.
 * Defined for modularity
 * 
 * @author jflamy
 *
 */

public abstract class BeanTupleQueryDefinition extends CritQueryDefinition<Tuple> implements QueryDefinition  {
	final private static Logger logger = LoggerFactory.getLogger(BeanTupleQueryDefinition.class);

	/**
	 * Map from a property name to the expression that is used to set the property.
	 * Each item property is retrieved by an expression in a {@link CriteriaQuery#multiselect(List)}
	 * In order to be able to sort, we need to create an {@link Order} using that expression, so
	 * we must memorize the correspondence between propertyIds and the expression that fetch
	 * item properties for that id.
	 */
	protected Map<Object,Expression<?>> sortExpressions = new HashMap<Object, Expression<?>>();
	
	/** all columns and expressions returned by the where */
	protected List<Selection<?>> selections = new ArrayList<Selection<?>>();

	private Metamodel metamodel;	
	private CriteriaBuilder criteriaBuilder;
	private CriteriaQuery<Tuple> tupleQuery;
	private CriteriaQuery<Long> countingQuery;

	private Root<?> root;

	private Set<Object> propertyIds;


	/**
	 * Constructor.
	 * @param entityManager the entityManager that gives us access to the database and cache
	 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
	 * @param batchSize how many tuples to retrieve at once.
	 */
	public BeanTupleQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
		super(entityManager, applicationManagedTransactions, Tuple.class, batchSize);
		metamodel = getEntityManager().getMetamodel();
		criteriaBuilder = getEntityManager().getCriteriaBuilder();
		
    	tupleQuery = criteriaBuilder.createTupleQuery();
    	defineQuery(criteriaBuilder, tupleQuery, sortExpressions, selections);

    	countingQuery = criteriaBuilder.createQuery(Long.class);
    	root = defineQuery(criteriaBuilder, countingQuery, sortExpressions, selections);
    	
    	//TODO: need to call addProperty to define the type and sortable status of the properties
	}


	/**
	 * @return a query with the sorting options.
	 */
	@Override
	public TypedQuery<Tuple> getSelectQuery() {
		// apply the selections
    	tupleQuery.multiselect(selections.toArray(new Selection[0]));
		
		// apply the ordering defined by the container on the returned entity.
		final List<Order> ordering = getOrdering();
		if (ordering != null) {
			tupleQuery.orderBy(ordering);
		}		
		
		final TypedQuery<Tuple> tq = getEntityManager().createQuery(tupleQuery);
		// the container deals with the parameter values set through the filter() method
		// so we only handle those that we add ourselves
		setParameters(tq);
		logger.warn("end getSelectQuery {} {}", selections, sortExpressions);
		return tq;
	}
	
	/**
	 * This method returns the number of entities.
	 * @return number of entities.
	 */
	@Override
	public TypedQuery<Long> getCountQuery() {
		countingQuery.select(criteriaBuilder.count(root));
		
		final TypedQuery<Long> countQuery = getEntityManager().createQuery(countingQuery);
		setParameters(countQuery);
		logger.warn("end getCountQuery {} {}", selections, sortExpressions);
		return countQuery;
	}


	/**
	 * Define the common part of the Query to be used for counting and selecting.
	 * Also memorizes the parts of the query that can be used for sorting.
	 * 
	 * @param criteriaBuilder the CriteriaBuilder used to define the from, where and select
	 * @param tupleQuery the CriteriaQuery to build the tuples,
	 * @param sortExpressions a map from a propertyId to the expression used by the CriteriaQuery to set its value
	 * @param selections the expressions for the select statement (including those necessary for sorting)
	 * @return the root of the query (used for counting)
	 */
	protected abstract Root<?> defineQuery(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> tupleQuery, Map<Object,Expression<?>> sortExpressions, List<Selection<?>> selections);
	

	/**
	 * @param javaType the type
	 * @return the corresponding class for which newInstance can be called.
	 */
	protected Class<?> instantatiableType(Class<?> javaType) {
		if (javaType == long.class) {
			javaType = Long.class;
		} else if (javaType == int.class) {
			javaType = Integer.class;
		} else if (javaType == boolean.class) {
			javaType = Boolean.class;
		}
		return javaType;
	}
	
	
	/**
	 * Applies the sort state.
	 * A JPA ordering is created based on the saved sort orders.
	 * 
	 * @return a list of Order objects to be added to the query.
	 */
	protected List<Order> getOrdering() {
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
				ordering.add(criteriaBuilder.asc(sortExpression));
			} else {
				ordering.add(criteriaBuilder.desc(sortExpression));
			}
		}
		return ordering;
	}
	
	
	/**
	 * Add entries to the where clause in order to support sorting.
	 */
	protected void addSortingSelections() {
        if (sortPropertyIds == null || sortPropertyIds.length == 0) {
            sortPropertyIds = nativeSortPropertyIds;
            sortPropertyAscendingStates = nativeSortPropertyAscendingStates;
        }
  
    	if (sortPropertyIds == null || sortPropertyIds.length == 0) return;

		for (int curItem = 0; curItem < sortPropertyIds.length; curItem++ ) {
	    	final String id = (String)sortPropertyIds[curItem];
			final Expression<?> sortExpression = sortExpressions.get(id);
			selections.add(sortExpression);
		}
	}

	
	/**
	 * Define the expression required to retrieve a column.
	 * The entity field will be retrieved as a tuple element. In order to enable sorting,
	 * we must memorize the query expression used to retrieve each field. An alias
	 * is defined on the expression.
	 * 
	 * @param propertyId the desired property id. An alias will be defined.
	 * @param selection the root or join from which the desired column will be fetched
	 * @param sortExpressions a map to enable sorting on 
	 * @return an expression that can be used in JPA order()
	 */
	protected Expression<?> computedExpression(
			final String propertyId,
			final Selection<?> selection,
			Map<Object, Expression<?>> sortExpressions) {
		final Expression<?> expression = (Expression<?>) selection;
		sortExpressions.put(propertyId,expression);
		return expression;
	}

	/**
	 * Define the expression required to retrieve an item property. 
	 * The entity field will be retrieved as a tuple element. In order to enable sorting,
	 * we must memorize the query expression used to retrieve each field.
	 * 
	 * This version is used when the propertyId must differ from the field name in the Entity
	 * (for example, when two fields have the same name, and aliases must be used, this method
	 * ensures that the aliases are defined consistently with the propertyId).
	 * 
	 * @param propertyId the property identifier in the items that will be constructed
	 * @param path the root or join from which the desired column will be fetched
	 * @param column the expression used to get the desired column in the query results
	 * @param sortExpressions a map to enable sorting on 
	 * @return an expression that can be used in JPA order()
	 */
	protected Expression<?> propertyExpression(
			final String propertyId,
			final Path<?> path,
			final SingularAttribute<?, ?> column,
			Map<Object, Expression<?>> sortExpressions) {
		final Expression<?> expression = path.get(column.getName());
		sortExpressions.put(propertyId,expression);
		expression.alias(propertyId);
		return expression;
	}
	
	
	/**
	 * @see org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition#getPropertyIds()
	 */
	@Override
	public Collection<?> getPropertyIds() {
		if (propertyIds == null) {
			Set<Object> propSet = new HashSet<Object>(sortExpressions.keySet());
			for (Selection<?> selection: selections){
				propSet.add(selection.getAlias());
			}
			propertyIds = Collections.unmodifiableSet(propSet);
			logger.warn("getPropertyIds={}",propSet);
		}
		return propertyIds;
	}


	/**
	 * @return the metamodel
	 */
	public Metamodel getMetamodel() {
		return metamodel;
	}


	/**
	 * Add an entity to the list of selections.
	 * Create Expression objects for each of the attributes of the entity, so
	 * that sorting is possible on the attributes.  Ensure that an alias is
	 * present on the selection, if not, use the type as alias
	 * 
	 * @param entityPath path (Root or Join) that designates an entity
	 */
	protected void addEntitySelection(Path<?> entityPath) {
		Class<?> instantatiableType = instantatiableType(entityPath.getJavaType());
		
		// make sure there is an alias
		String alias = entityPath.getAlias();
		if (alias == null) {
			entityPath.alias(instantatiableType.getSimpleName());
		}
		
		// add the entity to the tuple
		selections.add(entityPath);
		
		// add all the attributes to the sortable items.  we do not actually
		// the attributes separately -- the item will fetch them from the entity.
		Set<?> attributes = getMetamodel().entity(instantatiableType).getSingularAttributes();
		for (Object attributeObject : attributes) {
			SingularAttribute<?, ?> column = (SingularAttribute<?, ?>)attributeObject;
			propertyExpression(entityPath.getAlias()+"."+column.getName(), entityPath, column, sortExpressions);
		}
	}


	/**
	 * Add a computed expression to the list of selections.
	 * Remember the Expression so that sorting is possible on the attributes.
	 * Ensure that an alias is present on the selection.
	 * 
	 * @param selection alias for an Expression for a computed value
	 */
	protected void addComputedSelection(Selection<Long> selection) {
		// make sure there is an alias
		String alias = selection.getAlias();
		if (alias == null) {
			throw new IllegalArgumentException("All computed values must have an alias defined : missing alias on "+selection);
		}
		
		// add the computed value to the tuple
		selections.add(selection);
		
		// remember the expression so we can sort on the computed value
		computedExpression(selection.getAlias(), selection, sortExpressions);
	}
}

