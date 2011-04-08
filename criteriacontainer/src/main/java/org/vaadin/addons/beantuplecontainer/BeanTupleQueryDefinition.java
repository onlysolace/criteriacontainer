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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacore.AbstractCriteriaQueryDefinition;
import org.vaadin.addons.criteriacore.FilterRestriction;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Type-safe implementation of JPA 2.0 Tuple-based query definition.
 * 
 * <p>This class uses the JPA 2.0 Criteria API to automatically infer all the information necessary to
 * populate a container (the names of the properties, their sorting, etc.)</p>
 * 
 * <p>The query returns a Tuple of one or more entities and computed expressions. The query defines
 * all the attributes of each of the entities as nested propertyIds, so a container using this query will
 * consider them present.  The {@link BeanTupleItem} used in the {@link BeanTupleContainer} maps
 * the nested properties to entity fields, and in this way allows properties to be edited.</p>
 * 
 * <p>The following example shows a query that can be used to define a container:
 * it returns a tuple of entities (through the multiselect() call), and defines
 * conditions through a where() call.</p>
 * 
 * <p>The other methods in this class will examine the resulting object structure to
 * retrieve the information necessary to list the properties, define sorting, and
 * so on.</p>
 * <pre>
{@code protected Root<?> defineQuery( }
        CriteriaBuilder cb,
        CriteriaQuery<?> cq) {

    // FROM task JOIN PERSON 
    {@code Root<Person> person = (Root<Person>) cq.from(Person.class);}
    task = person.join(Person_.tasks); 

    // SELECT task as Task, person as Person, ... 
    cq.multiselect(task,person);

    // WHERE t.name LIKE "..."    
    cq.where(
        cb.like(
            task.get(Task_.name), // t.name
            "some string%")  // sql "like" pattern to be matched
        );
    }

    return person;
}
}</pre>
 * 
 * @author jflamy
 */

public abstract class BeanTupleQueryDefinition extends AbstractCriteriaQueryDefinition<Tuple> implements QueryDefinition  {

    final private static Logger logger = LoggerFactory.getLogger(BeanTupleQueryDefinition.class);

	/**
	 * Map from a property name to the expression that is used to set the property.
	 * Each item property is retrieved by an expression in a {@link CriteriaQuery#multiselect(List)}
	 * In order to be able to sort, we need to create an {@link Order} using that expression, so
	 * we must memorize the correspondence between propertyIds and the expression that fetch
	 * item properties for that id.
	 */
	protected Map<Object,Expression<?>> countingExpressionMap = new HashMap<Object, Expression<?>>();
	/**
     * The maps cannot be shared between the two queries.
     * @see #countingExpressionMap
     */
	protected Map<Object,Expression<?>> selectExpressionMap = new HashMap<Object, Expression<?>>();
	
	/** all columns and expressions returned by the where */
	protected Set<Selection<?>> selections = new HashSet<Selection<?>>();

	private Metamodel metamodel;	
	
	/** the criteria builder used to build the JPA query data structure */
	protected CriteriaBuilder criteriaBuilder;
	
	/** the executable query */
	protected CriteriaQuery<Tuple> tupleQuery;
	
	/** all columns and expressions returned by the where */
	protected CriteriaQuery<Long> countingQuery;

	/** the root for the query */
	protected Root<?> root;

    private Collection<FilterRestriction> filters;

    private boolean detachedEntities = false;



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
	}

    /**
     * Constructor.
     * @param entityManager the entityManager that gives us access to the database and cache
     * @param detachedEntities if true, entities will be detached from the persistence context and merged as needed.
     * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
     * @param batchSize how many tuples to retrieve at once.
     */
    public BeanTupleQueryDefinition(
            EntityManager entityManager,
            boolean detachedEntities,
            boolean applicationManagedTransactions,
            int batchSize) {
        super(entityManager, applicationManagedTransactions, Tuple.class, batchSize);
        metamodel = getEntityManager().getMetamodel();
        criteriaBuilder = getEntityManager().getCriteriaBuilder();
        setDetachedEntities(detachedEntities);
    }

	/**
	 * Reset the query definitions.
	 */
	@Override
    public void refresh() {
    	countingQuery = criteriaBuilder.createQuery(Long.class);
    	root = defineQuery(criteriaBuilder, countingQuery);
    	mapProperties(countingQuery, countingExpressionMap, false);
        addFilteringConditions(criteriaBuilder, countingQuery, countingExpressionMap);
        
        tupleQuery = criteriaBuilder.createTupleQuery();
        defineQuery(criteriaBuilder, tupleQuery);
        mapProperties(tupleQuery, selectExpressionMap, true);
        addFilteringConditions(criteriaBuilder, tupleQuery, selectExpressionMap);
        
        initialized = true;
	}

	/**
	 * Initial setup, once only.
	 */
	public void init() {
	    if (!initialized) {
	        refresh();
	        initialized = true;
	    }
	}

	/**
	 * @return a query with the applicable sorting options applied
	 */
	@Override
	public TypedQuery<Tuple> getSelectQuery() {
	    init();
	    
		// apply the ordering defined by the container on the returned entity.
		final List<Order> ordering = getOrdering(selectExpressionMap);
		if (ordering != null && ordering.size() > 0) {
			tupleQuery.orderBy(ordering);
		} else {
		    tupleQuery.orderBy();
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
	    init();

	    // we only want the count so we override the selection in the query
	    countingQuery.orderBy();
	    Selection<Long> selection = countingQuery.getSelection();
	    if (selection.isCompoundSelection()) {
	        List<Selection<?>> items = selection.getCompoundSelectionItems();
	        if (items.size() == 1) {
	            if (countingQuery.isDistinct()) {
	                // distinct query, use countDistinct
	                countingQuery.distinct(false);
	                countingQuery.select(criteriaBuilder.countDistinct((Expression<?>) items.get(0)));
	            } else {
	                countingQuery.select(criteriaBuilder.count((Expression<?>) items.get(0)));
	            }
	        } else {
	            countingQuery.select(criteriaBuilder.count(root));
	        }
	    } else {
	        if (countingQuery.isDistinct()) {
	            countingQuery.distinct(false);
	            countingQuery.select(criteriaBuilder.countDistinct((Expression<?>) countingQuery.getSelection()));
	        } else {
	            countingQuery.select(criteriaBuilder.count((Expression<?>) countingQuery.getSelection()));
	        }
	    }


	    // create the executable query
	    final TypedQuery<Long> countQuery = getEntityManager().createQuery(countingQuery);
	    setParameters(countQuery);
	    return countQuery;
	}


	/**
	 * Define a query that fetches a tuple of one or more entities.
	 * 
	 * <p>Note that defineQuery must always use {@link CriteriaQuery#multiselect(Selection...)}
	 * because a tuple is expected.
	 * 
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
			return 0L;
		} else if (javaType == int.class) {
			return 0;
		} else if (javaType == boolean.class) {
			return false;
		} else if (javaType == char.class) {
			return ' ';
		} else if (javaType == Long.class) {
            return 0L;
        } else if (javaType == Integer.class) {
            return 0;
        } else if (javaType == Boolean.class) {
            return new Boolean(false);
        } else if (javaType == Character.class) {
            return new Character(' ');
        } else if (javaType  == String.class) {
			return "";
		} else {
		    return null;
		}
	}
	
	
	/**
	 * Applies the sort state.
	 * A JPA ordering is created based on the saved sort orders.
	 * @param expressionMap where to lookup expressions by id 
	 * @return a list of Order objects to be added to the query.
	 */
	protected List<Order> getOrdering(Map<Object, Expression<?>> expressionMap) {
        if (sortPropertyIds == null || sortPropertyIds.length == 0) {
            sortPropertyIds = nativeSortPropertyIds;
            sortPropertyAscendingStates = nativeSortPropertyAscendingStates;
        }
  
        ArrayList<Order> ordering = new ArrayList<Order>();
    	if (sortPropertyIds == null || sortPropertyIds.length == 0) return ordering;

		for (int curItem = 0; curItem < sortPropertyIds.length; curItem++ ) {
	    	final String id = (String)sortPropertyIds[curItem];
			final Expression<?> sortExpression = getExpressionById(id, expressionMap);
			if (sortExpression != null && sortPropertyAscendingStates[curItem]) {
				ordering.add(criteriaBuilder.asc(sortExpression));
			} else {
				ordering.add(criteriaBuilder.desc(sortExpression));
			}
		}
		return ordering;
	}


    /**
     * Return the expression used to access a given property
     * @param id the property id
     * @param expressionMap where to lookup expressions by id.
     * @return the expression
     */
    public Expression<?> getExpressionById(final String id, Map<Object, Expression<?>> expressionMap) {
        final Expression<?> sortExpression = expressionMap.get(id);
        if (sortExpression == null) {
            throw new PersistenceException("Property "+id+" cannot be mapped to a selection from the query.");
        }
        return sortExpression;
    }
	

	
	/**
	 * Define the expression required to retrieve a column.
	 * The entity field will be retrieved as a tuple element. In order to enable sorting,
	 * we must memorize the query expression used to retrieve each field. An alias
	 * is defined on the expression.
	 * 
	 * @param expressionMap where to remember the mapping
	 * @param propertyId the desired property id. An alias will be defined.
	 * @param selection the root or join from which the desired column will be fetched
	 * @param defineProperties define properties for the container
	 * @return an expression that can be used in JPA order()
	 */
	protected Expression<?> addPropertyForComputedValue(
	        Map<Object, Expression<?>> expressionMap,
			final String propertyId,
			final Selection<?> selection,
			boolean defineProperties) {
		final Expression<?> expression = (Expression<?>) selection;
		addPropertyForExpression(expressionMap,propertyId,expression, defineProperties);
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
     * @param expressionMap where to remember the mapping
	 * @param propertyId the property identifier in the items that will be constructed
	 * @param path the root or join from which the desired column will be fetched
	 * @param column the expression used to get the desired column in the query results
	 * @param defineProperties define the property for the container
	 * @return an expression that can be used in JPA order()
	 */
	protected Expression<?> addPropertyForAttribute(
	        Map<Object, Expression<?>> expressionMap,
			final String propertyId,
			final Path<?> path,
			final SingularAttribute<?, ?> column, 
			boolean defineProperties) {
		final Expression<?> expression = path.get(column.getName());
		expression.alias(propertyId);
		addPropertyForExpression(expressionMap,propertyId,expression,defineProperties);
		return expression;
	}
	
	
	/**
	 * Helper routine to add a property.
	 * @param expressionMap where to remember the mapping
	 * @param propertyId the property Id
	 * @param expression the expression that fetches the value for propertyId
	 * @param defineProperties define the property for the container
	 */
	protected void addPropertyForExpression(
	        Map<Object, Expression<?>> expressionMap,
	        Object propertyId,
			Expression<?> expression, boolean defineProperties) {
		Class<?> propertyType = instantatiableType(expression.getJavaType());
		
		boolean isEntity = propertyType.getClass().isAnnotationPresent(Entity.class);
		boolean sortable = Comparable.class.isAssignableFrom(propertyType);
		boolean readOnly = !isEntity; // entities are read-only, attributes and expressions readable
		
		//logger.debug("adding property {} {}",propertyId,defineProperties);
		if (defineProperties) {
		    addProperty(propertyId, propertyType, defaultValue(propertyType), readOnly, sortable);
		}
		if (sortable){
		    logger.trace("adding to property map: {}",propertyId);
		    expressionMap.put(propertyId, expression);
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
	 * @param expressionMap where to remember the mappings
	 * @param entityPath path (Root or Join) that designates an entity
	 * @param defineProperties define properties for the container
	 */
	protected void addEntityProperties(
	        Map<Object, Expression<?>> expressionMap,
	        Path<?> entityPath, boolean defineProperties) {
		Class<?> instantatiableType = instantatiableType(entityPath.getJavaType());
		
		// make sure there is an alias
		ensureAlias(entityPath);
		
		// define a property for the entity
		addPropertyForExpression(expressionMap,entityPath.getAlias(), entityPath, defineProperties);

		// add properties for all the attributes to the sortable items the container knows about
		// (the BeanTupleItem is smart about this and does not actually duplicate info)
		Set<?> attributes = getMetamodel().entity(instantatiableType).getSingularAttributes();
		for (Object attributeObject : attributes) {
			SingularAttribute<?, ?> column = (SingularAttribute<?, ?>)attributeObject;
			addPropertyForAttribute(expressionMap,
			        entityPath.getAlias()+"."+column.getName(), 
			        entityPath, 
			        column, 
			        defineProperties);
		}
	}


	/**
	 * Add a computed expression to the list of selections.
	 * Remember the Expression so that sorting is possible on the attributes.
	 * Ensure that an alias is present on the selection.
	 * 
	 * @param expressionMap where to remember the mapping
	 * @param selection alias for an Expression for a computed value
	 * @param defineProperties where to remember the mapping
	 */
	protected void addComputedProperty(
	        Map<Object, Expression<?>> expressionMap,
	        Selection<?> selection,
	        boolean defineProperties) {
		// make sure there is an alias
		String alias = selection.getAlias();
		if (alias == null) {
			throw new IllegalArgumentException("All non-entity selections in multiselect() must have an alias defined : missing alias on "+selection);
		}
		
		// remember the expression so we can sort on the computed value
		addPropertyForComputedValue(expressionMap, selection.getAlias(), selection, defineProperties);
	}


	/**
	 * Create property definitions for the container.
	 * The wrapped container actually calls this method to get its properties.
	 * @param query the query for which the mapping is being performed
	 * @param expressionMap where to remember the mappings
	 * @param defineProperties define properties for the container
	 */
	protected void mapProperties(
	        CriteriaQuery<?> query,
	        Map<Object, Expression<?>> expressionMap,
	        boolean defineProperties) {
		Object propertyId ;
		for ( Entry<Object, Expression<?>>  entry : expressionMap.entrySet()){
			propertyId = entry.getKey();
			if (logger.isDebugEnabled() && defineProperties) {logger.debug("expression: {}",propertyId);}
			Expression<?> expression = entry.getValue();
			if (propertyId != null){
				addPropertyForExpression(expressionMap, propertyId, expression, defineProperties);
			}
		}
		// add the other items in the selection that are not meant to be sortable
		Selection<?> selection = query.getSelection();
		if (selection == null) {
		    throw new PersistenceException("No selection defined on query.");
		}
        List<Selection<?>> compoundSelectionItems = selection.getCompoundSelectionItems();
		for (Selection<?> compoundSelection: compoundSelectionItems){
		    
			if (compoundSelection.getJavaType().isAnnotationPresent(Entity.class)) {
			    if (logger.isDebugEnabled() && defineProperties) {logger.debug("entity: {}",compoundSelection.getJavaType());}
				addEntityProperties(expressionMap,(Path<?>) compoundSelection, defineProperties);
			} else {
			    if (logger.isDebugEnabled() && defineProperties) {logger.debug("computed: {}",compoundSelection.getJavaType());}
				addComputedProperty(expressionMap, compoundSelection, defineProperties);
			}
		}
	}


	/**
	 * If the selection has no alias, add one based on the type name.
	 * @param selection the selection for which the alias is sought.
	 * @return the alias string
	 */
	protected String ensureAlias(Selection<?> selection) {
		String alias = selection.getAlias();
		if (alias == null || alias.isEmpty()) {
			selection.alias(instantatiableType(selection.getJavaType()).getSimpleName());
		}
		return selection.getAlias();
	}

	
    /**
     * Store a list of filters.
     * Each restriction will be transformed into a predicate added to the WHERE clause.
     * 
     * @param restrictions a list of objects that each define a condition to be added
     */
    public void setFilters(Collection<FilterRestriction> restrictions) {
        this.filters = restrictions;
    }
	
    
    /**
     * @return the filters
     */
    public Collection<FilterRestriction> getFilters() {
        return filters;
    }
    
    
    /**
     * @param cb the criteria builder
     * @param cq the query constructed so far
     * @param expressionMap where to locate the expressions
     */
    protected void addFilteringConditions(
            CriteriaBuilder cb,
            CriteriaQuery<?> cq,
            Map<Object, Expression<?>> expressionMap) {
        List<Predicate> filterExpressions = new ArrayList<Predicate>();
        
        // get the conditions already in the query
        Predicate currentRestriction = cq.getRestriction();
        if (currentRestriction != null){
           filterExpressions.add(currentRestriction);
        }
        
        // predicates created from CriteriaContainer.filter()
        if (filters != null && filters.size() > 0) {
            Predicate predicate = FilterRestriction.getConjoinedPredicate(filters, cb, this, expressionMap);
            filterExpressions.add(predicate);
        }
        
        // build array, casting all the elements to Predicate
        final Predicate[] array = filterExpressions.toArray(new Predicate[0]);
        
        // overwrite the conditions that were in the query
        if (filterExpressions.size() > 0){
            cq.where(array);
        } else {   
            cq.where();
        }
    }
    
   
    
    /**
     * Prepare the query so that the container filter() works.
     * 
     * For each field named in the whereParameters map, create a parameter place holder
     * in the query.  There is no setFilterParameters method, the container does the
     * processing in the filter() method.
     * 
     * @param filterExpressions the predicates created by filtering mechanisms so far.
     * @param cb the current query builder
     * @param cq the query as built so far
     * @param expressionMap where to lookup expressions by id
     * @return a list of predicates to be added to the query 
     */
    protected List<Predicate> addToFilterRestrictions(
            List<Predicate> filterExpressions,
            CriteriaBuilder cb,
            CriteriaQuery<?> cq,
            Map<Object, Expression<?>> expressionMap) {
        if (filters != null) {
            filterExpressions.add(FilterRestriction.getConjoinedPredicate(filters,cb,this,expressionMap));
        }
        return filterExpressions;
    }

	/**
	 * Compute the property id from information available in the static metamodel.
	 * @param metamodelType the static metamodel in which the attribute is found.
	 * @param attr a singular attribute defined in the model
	 * @return a propertyId if it has been found in the defined properties, null if not.
	 */
	public String getPropertyId(Class<?> metamodelType, SingularAttribute<?, ?> attr) {
	    init();
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
	    init();
		String propertyId = alias+"."+attr.getName();
		if (propertyIds.contains(propertyId)) {
			return propertyId;
		} else {
			return null;
		}
	}



    
    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#isCompositeItems()
     */
    @Override
    public boolean isCompositeItems() {
        throw new UnsupportedOperationException();
    }


    /* (non-Javadoc)
     * @see org.vaadin.addons.lazyquerycontainer.QueryDefinition#setCompositeItems(boolean)
     */
    @Override
    public void setCompositeItems(boolean arg0) {
        throw new UnsupportedOperationException();
    }


    /**
     * @return whether the query manages detached entities.
     */
    public boolean isDetachedEntities() {
        return detachedEntities;
    }
    

    /**
     * @param detachedEntities the detachedEntities to set
     */
    public void setDetachedEntities(boolean detachedEntities) {
        this.detachedEntities = detachedEntities;
    }

}

