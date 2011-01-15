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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.SingularAttribute;

import org.vaadin.addons.tuplecontainer.TupleQueryDefinition;

/**
 * Type-safe implementation of a query definition.
 * Defined for modularity
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public abstract class BeanTupleQueryDefinition extends TupleQueryDefinition {

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
	public BeanTupleQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
		// We pass Task.class because the parameterized type of this class is <Task>
		super(entityManager, applicationManagedTransactions, batchSize);
	}


	/**
	 * Define the expression required to retrieve a column.
	 * This version is used when the propertyId is the same as the field name in the entity.
	 * 
	 * The entity field will be retrieved as a tuple element, and copied to the container item.
	 * In order to enable sorting, we must memorize the query expression used to retrieve each 
	 * field.
	 * 
	 * @param path the root or join from which the desired column will be fetched
	 * @param column the expression used to get the desired column in the query results
	 * @param sortExpressions a map to enable sorting on 
	 * @return an expression that can be used in JPA order()
	 */
	@Override
	protected Expression<?> propertyExpression(final Path<?> path, final SingularAttribute<?, ?> column, Map<Object, Expression<?>> sortExpressions) {
		return propertyExpression(path.getAlias()+"."+column.getName(), path, column, sortExpressions);
	}


}

