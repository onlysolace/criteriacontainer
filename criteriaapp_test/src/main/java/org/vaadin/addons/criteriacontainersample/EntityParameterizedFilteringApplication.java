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
package org.vaadin.addons.criteriacontainersample;

import java.util.LinkedList;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacontainer.CriteriaContainer;
import org.vaadin.addons.criteriacontainer.CriteriaQueryDefinition;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;
import org.vaadin.addons.criteriacore.FilterRestriction;

/**
 * Example application demonstrating how to implement
 * queries and filtering by extending a query definition.
 * The filtering is managed in type-safe fashion by the query definition. 
 * 
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
public class EntityParameterizedFilteringApplication extends AbstractEntityApplication {
	
	public class ParameterizedTaskQueryDefinition extends CriteriaQueryDefinition<Task> {

		/** parameter to be set at query time */
		private ParameterExpression<String> nameFilter;
		
		/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
		private String nameFilterValue;

		@SuppressWarnings("unused")
		final private Logger logger = LoggerFactory.getLogger(ParameterizedTaskQueryDefinition.class);

		/**
		 * Constructor.
		 * @param entityManager the entityManager that gives us access to the database and cache
		 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
		 * @param batchSize how many tuples to retrieve at once. 
		 */
		public ParameterizedTaskQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
			super(entityManager, applicationManagedTransactions, batchSize);
		}
		

		/**
		 * Define SELECT, FROM and WHERE clauses
		 * 
		 * In this example, a {@link CriteriaBuilder#parameter(Class)} is used to define a parameter
		 * place holder.  This requires that the {@link #setParameters(TypedQuery)} be overridden to
		 * define the parameters.
		 */
		@Override
		protected Root<?> defineQuery(CriteriaBuilder cb, CriteriaQuery<?> cq) {
		    
            Root<Task> t = cq.from(Task.class);
            
            cq.multiselect(t);
            
			if (nameFilterValue != null && !nameFilterValue.isEmpty()) {
				// This example shows how to create a parameterized query.
				Expression<String> nameField = t.get(Task_.name);
				nameFilter = cb.parameter(String.class);
				cq.where(
				        (cb.like(nameField,nameFilter)));
			}
			return t;

		}
		
		/**
		 * Set values for the parameters used in the predicates.
		 * 
		 * Should provide a value for all the parameter objects.
		 * Is expected to also call super() to handle named parameters defined through {@link #setNamedParameterValues(java.util.Map)}.
		 * It should also define all Parameter objects defined in {@link #defineQuery(CriteriaBuilder, CriteriaQuery)}
         * (in the current application, the field "nameFilter" is such a parameter.)
		 */
		@Override
		public TypedQuery<?> setParameters(final TypedQuery<?> tq) {
		    // set named parameters
			super.setParameters(tq);
			
			// set parameters defined as JPA 2.0 Parameter objects
			if (nameFilterValue != null && !nameFilterValue.isEmpty()) {
				tq.setParameter(nameFilter, nameFilterValue);
			}
			return tq;
		}
		
		
		public String getNameFilterValue() {
			return nameFilterValue;
		}

		public void setNameFilterValue(String nameFilterValue) {
			this.nameFilterValue = nameFilterValue;
		}

	}

	
	private ParameterizedTaskQueryDefinition cd;

	/**
	 * @return the container used to feed the table
	 */
	@Override
	protected CriteriaContainer<Task> createTaskContainer() {
		cd = new ParameterizedTaskQueryDefinition(entityManager,true,100);
		
		CriteriaContainer<Task> taskContainer = new CriteriaContainer<Task>(cd);
		addContainerProperties(taskContainer);
		return taskContainer;
	}

	/**
	 * Call the query definition to activate the filter.
	 * The resulting query definition includes conditions to the WHERE clause.
	 */
	@Override
	protected void doFiltering() {
		final String nameFilterValue = (String) nameFilterField.getValue();
		if (nameFilterValue != null && nameFilterValue.length() != 0) {
			// filtering style #1: query definition includes type safe filters.
			// the query has its own specific mechanism for setting the filters up.
			cd.setNameFilterValue(nameFilterValue);
			criteriaContainer.refresh();
		} else {
			cd.setNameFilterValue(null);
			criteriaContainer.filter((LinkedList<FilterRestriction>)null);    
		}
	}
	

}
