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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.beantuplecontainer.BeanTupleContainer;
import org.vaadin.addons.criteriacontainer.CriteriaContainer;
import org.vaadin.addons.criteriacontainer.CriteriaQueryDefinition;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;

/**
 * Example application demonstrating how to implement
 * queries and filtering by extending a query definition.
 * 
 * The filtering is managed in type-safe fashion by the query definition. 
 * 
 * @author Jean-François Lamy
 */

@SuppressWarnings("serial")
public class EntityParameterizedFilteringApplication extends AbstractEntityApplication {
    
    @SuppressWarnings("unused")
    final private Logger logger = LoggerFactory.getLogger(ParameterizedTaskQueryDefinition.class);
	
	class ParameterizedTaskQueryDefinition extends CriteriaQueryDefinition<Task> {
		
		/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
		private String nameFilterValue;


		/**
		 * Constructor.
		 * @param entityManager the entityManager that gives us access to the database and cache
		 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
		 * @param batchSize how many tuples to retrieve at once. 
		 */
		public ParameterizedTaskQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
			super(entityManager, applicationManagedTransactions, batchSize, Task.class);
		}
		

		/**
		 * Define SELECT, FROM and WHERE clauses
		 * 
		 * In this example, a {@link CriteriaBuilder#parameter(Class)} is used to define a parameter
		 * place holder.  The method {@link #setParameters(TypedQuery)} is overridden to provide
		 * the values.
		 */
		@Override
		protected Root<?> defineQuery(CriteriaBuilder cb, CriteriaQuery<?> cq) {
            Root<Task> t = cq.from(Task.class);
            
            cq.multiselect(t);
            
			if (nameFilterValue != null && !nameFilterValue.isEmpty()) {
				// add a filtering condition to the query. In this example,
			    // we use a parameter.
				Expression<String> nameField = t.get(Task_.name);
				cq.where(
				        (cb.like(nameField,
				                cb.parameter(String.class,"nameFilterParameter"))));
			}
			return t;
		}


	    
		/**
		 * Set values for all parameters used in the predicates.
		 * 
		 * Is expected to call super() to handle named parameters defined through {@link #setNamedParameterValues(java.util.Map)}.
         * 
         * In this example, there is only one parameter used in defineQuery and we set it directly.
		 */
		@Override
		public TypedQuery<?> setParameters(final TypedQuery<?> tq) {
		    // set named parameters from the {@link #setNamedParameterValues(java.util.Map)} method
			super.setParameters(tq);
			
			// set parameters from values defined in this class
			if (nameFilterValue != null && !nameFilterValue.isEmpty()) {
				tq.setParameter("nameFilterParameter", nameFilterValue);
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
		addSpecialProperties(taskContainer);
		return taskContainer;
	}
	

	/**
	 * React to the "Refresh" button.
	 * The resulting query definition includes conditions to the WHERE clause.
	 */
	@Override
	protected void doFiltering() {
		final String nameFilterValue = (String) nameFilterField.getValue();
		if (nameFilterValue != null && nameFilterValue.length() != 0) {
		    // explicit filter.
			cd.setNameFilterValue(nameFilterValue);
			criteriaContainer.refresh();
		} else {
			cd.setNameFilterValue(null);
			criteriaContainer.refresh();
		}
	}
	
	
	/* Define visible columns, in accordance with the properties defined by the query definition.
     * @see org.vaadin.addons.criteriacontainersample.AbstractBeanTupleApplication#defineTableColumns()
     */
    @Override
    protected void defineTableColumns() {
        visibleColumnIds.add(cd.getPropertyId(Task_.class, Task_.taskId));
        visibleColumnLabels.add("Task ID");
        
        visibleColumnIds.add(cd.getPropertyId(Task_.class, Task_.name));
        visibleColumnLabels.add("Name");
        
        visibleColumnIds.add(cd.getPropertyId(Task_.class, Task_.alpha));
        visibleColumnLabels.add("Name");
        
        table.setVisibleColumns(visibleColumnIds.toArray());
        table.setColumnHeaders(visibleColumnLabels.toArray(new String[0]));
    }


	/* (non-Javadoc)
	 * @see org.vaadin.addons.criteriacontainersample.AbstractBeanTupleApplication#createTupleContainer()
	 */
	@Override
	protected BeanTupleContainer createTupleContainer() {
		// unused for this demo
		return null;
	}

}
