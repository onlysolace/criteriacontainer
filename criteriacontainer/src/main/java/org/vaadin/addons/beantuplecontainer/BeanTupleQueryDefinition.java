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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.Entity;
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
import javax.persistence.metamodel.StaticMetamodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacore.CritQueryDefinition;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Type-safe implementation of JPA 2.0 Tuple-based query definition.
 * 
 * This class uses the JPA 2.0 Criteria API to automatically infer all the information necessary to
 * populate a container (the names of the properties, their sorting, etc.)
 * 
 * The query returns a Tuple of one or more entities and computed expressions. The query defines
 * all the attributes of each of the entities as nested propertyIds, so a container using this query will
 * consider them present.  The {@link BeanTupleItem} used in the {@link BeanTupleContainer} maps
 * the nested properties to entity fields, and in this way allows properties to be edited.
 * 
 * @author jflamy
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
	protected Set<Selection<?>> selections = new HashSet<Selection<?>>();

	private Metamodel metamodel;	
	private CriteriaBuilder criteriaBuilder;
	private CriteriaQuery<Tuple> tupleQuery;
	private CriteriaQuery<Long> countingQuery;

	private Root<?> root;

	private boolean propertiesDefined;



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
		
		refresh();
		propertiesDefined = true;
	}


	/**
	 * reset the query definitions.
	 */
	public void refresh() {
    	countingQuery = criteriaBuilder.createQuery(Long.class);
    	root = defineQuery(criteriaBuilder, countingQuery);
    	countingQuery.orderBy();
    	
		tupleQuery = criteriaBuilder.createTupleQuery();
    	defineQuery(criteriaBuilder, tupleQuery);

    	// define the type and sortable status of the properties
    	defineProperties();
	}


	/**
	 * @return a query with the sorting options.
	 */
	@Override
	public TypedQuery<Tuple> getSelectQuery() {

		// apply the ordering defined by the container on the returned entity.
		final List<Order> ordering = getOrdering();
		if (ordering != null) {
			tupleQuery.orderBy(ordering);
		}		
		
		final TypedQuery<Tuple> tq = getEntityManager().createQuery(tupleQuery);
		// the container deals with the parameter values set through the filter() method
		// so we only handle those that we add ourselves
		setParameters(tq);
		return tq;
	}
	
	/**
	 * This method returns the number of entities.
	 * @return number of entities.
	 */
	@Override
	public TypedQuery<Long> getCountQuery() {
    	
    	// cancel sorting defined by the query
    	countingQuery.orderBy();
    	
		// override the select added in the query definition, we want a count.
		countingQuery.select(criteriaBuilder.count(root));
		
		final TypedQuery<Long> countQuery = getEntityManager().createQuery(countingQuery);
		setParameters(countQuery);
		return countQuery;
	}


	/**
	 * Define a query that fetches a tuple of one or more entities.
	 * 
     * The following example shows a query that can be used to define a container:
     * it returns a tuple of entities (through the multiselect() call), and defines
     * conditions through a where() call.
	 * 
	 * The other methods in this class will examine the resulting object structure to
	 * retrieve the information necessary to list the properties, define sorting, and
	 * so on.
	 * <pre>
{@code protected Root<?> defineQuery( }
        CriteriaBuilder cb,
        CriteriaQuery<?> cq) {

    // FROM task JOIN PERSON 
    {@code Root<Person> person = (Root<Person>) cq.from(Person.class);}
    task = person.join(Person_.tasks); 

    // SELECT task as Task, person as Person, ... 
    cq.multiselect(task,person);

    // WHERE t.name LIKE nameFilterValue    
    cq.where(
        cb.like(
            task.get(Task_.name), // t.name
            "test%")  // sql "like" pattern to be matched
        );
    }

    return person;
}
}</pre>
	 * @param criteriaBuilder the CriteriaBuilder used to define the from, where and select
	 * @param tupleQuery the CriteriaQuery to build the tuples,
	 * @return a root of the query (used for counting the lines returned)
	 */
	@Override
	protected abstract Root<?> defineQuery(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> tupleQuery);
	

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
		} else if (javaType == char.class) {
			javaType = Character.class;
		}
		return javaType;
	}
	

	/**
	 * @param javaType the type
	 * @return a meaningful default value
	 */
	protected Object defaultValue(Class<?> javaType) {
		if (javaType == long.class) {
			return new Long(0L);
		} else if (javaType == int.class) {
			return new Integer(0);
		} else if (javaType == boolean.class) {
			return new Boolean(false);
		} else if (javaType == char.class) {
			return new Character(' ');
		} else {
			return null;
		}
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
	 * Define the expression required to retrieve a column.
	 * The entity field will be retrieved as a tuple element. In order to enable sorting,
	 * we must memorize the query expression used to retrieve each field. An alias
	 * is defined on the expression.
	 * 
	 * @param propertyId the desired property id. An alias will be defined.
	 * @param selection the root or join from which the desired column will be fetched
	 * @return an expression that can be used in JPA order()
	 */
	protected Expression<?> addPropertyForComputedValue(
			final String propertyId,
			final Selection<?> selection) {
		final Expression<?> expression = (Expression<?>) selection;
		addPropertyForExpression(propertyId,expression);
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
	 * @return an expression that can be used in JPA order()
	 */
	protected Expression<?> addPropertyForAttribute(
			final String propertyId,
			final Path<?> path,
			final SingularAttribute<?, ?> column) {
		final Expression<?> expression = path.get(column.getName());
		expression.alias(propertyId);
		addPropertyForExpression(propertyId,expression);
		return expression;
	}
	
	/**
	 * Helper routine to add a property.
	 * @param propertyId
	 * @param expression
	 */
	private void addPropertyForExpression(Object propertyId,
			Expression<?> expression) {
		Class<?> propertyType = instantatiableType(expression.getJavaType());
		
		boolean isEntity = propertyType.getClass().isAnnotationPresent(Entity.class);
		boolean sortable = Comparable.class.isAssignableFrom(propertyType);
		boolean readOnly = !isEntity; // entities are read-only, attributes and expressions readable
		if (!propertiesDefined) addProperty(propertyId, propertyType, defaultValue(propertyType), readOnly, sortable);
		if (sortable){
			sortExpressions.put(propertyId, expression);
		}
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
	protected void addEntityProperties(Path<?> entityPath) {
		Class<?> instantatiableType = instantatiableType(entityPath.getJavaType());
		
		// make sure there is an alias
		ensureAlias(entityPath);
		
		// define a property for the entity
		addPropertyForExpression(entityPath.getAlias(), entityPath);

		// add properties for all the attributes to the sortable items the container knows about
		// (the BeanTupleItem is smart about this and does not actually duplicate info)
		Set<?> attributes = getMetamodel().entity(instantatiableType).getSingularAttributes();
		for (Object attributeObject : attributes) {
			SingularAttribute<?, ?> column = (SingularAttribute<?, ?>)attributeObject;
			addPropertyForAttribute(entityPath.getAlias()+"."+column.getName(), entityPath, column);
		}
	}


	/**
	 * Add a computed expression to the list of selections.
	 * Remember the Expression so that sorting is possible on the attributes.
	 * Ensure that an alias is present on the selection.
	 * 
	 * @param selection alias for an Expression for a computed value
	 */
	protected void addComputedProperty(Selection<?> selection) {
		// make sure there is an alias
		String alias = selection.getAlias();
		if (alias == null) {
			throw new IllegalArgumentException("All computed values must have an alias defined : missing alias on "+selection);
		}
		
		// remember the expression so we can sort on the computed value
		addPropertyForComputedValue(selection.getAlias(), selection);
	}


	/**
	 * Create property definitions for the container.
	 * The wrapped container actually calls this method to get its properties.
	 */
	private void defineProperties() {
		Object propertyId ;
		for ( Entry<Object, Expression<?>>  entry : sortExpressions.entrySet()){
			propertyId = entry.getKey();
			Expression<?> expression = entry.getValue();
			if (propertyId != null){
				addPropertyForExpression(propertyId, expression);
			}
		}
		// add the other items in the selection that are not meant to be sortable
		List<Selection<?>> compoundSelectionItems = tupleQuery.getSelection().getCompoundSelectionItems();
		for (Selection<?> selection: compoundSelectionItems){
			if (selection.getJavaType().isAnnotationPresent(Entity.class)) {
				addEntityProperties((Path<?>) selection);
			} else {
				addComputedProperty(selection);
			}
		}
		logger.warn("after defineProperties: {}", getPropertyIds());
	}


	/**
	 * @param selection
	 * @return
	 */
	private String ensureAlias(Selection<?> selection) {
		String alias = selection.getAlias();
		if (alias == null || alias.isEmpty()) {
			selection.alias(instantatiableType(selection.getJavaType()).getSimpleName());
		}
		return selection.getAlias();
	}


	/**
	 * Compute the property id from information available in the static metamodel.
	 * @param metamodelType the static metamodel in which the attribute is found.
	 * @param attr a singular attribute defined in the model
	 * @return a propertyId if it has been found in the defined properties, null if not.
	 */
	public String getPropertyId(Class<?> metamodelType, SingularAttribute<?, ?> attr) {
		Class<?> entityType = metamodelType.getAnnotation(StaticMetamodel.class).value();
		String propertyId = entityType.getSimpleName()+"."+attr.getName();
		if (propertyIds.contains(propertyId)) {
			return propertyId;
		} else {
			return null;
		}
	}

	
	/**
	 * Compute the property id from information available in the static metamodel.
	 * @param alias alias used in the query definition.
	 * @param attr a singular attribute defined in the model
	 * @return a propertyId if it has been found in the defined properties, null if not.
	 */
	public String getPropertyId(String alias, SingularAttribute<?, ?> attr) {
		String propertyId = alias+"."+attr.getName();
		if (propertyIds.contains(propertyId)) {
			return propertyId;
		} else {
			return null;
		}
	}
}

