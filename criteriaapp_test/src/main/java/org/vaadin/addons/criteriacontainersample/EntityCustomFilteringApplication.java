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

import java.text.MessageFormat;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.vaadin.addons.beantuplecontainer.BeanTupleContainer;
import org.vaadin.addons.criteriacontainer.CriteriaContainer;
import org.vaadin.addons.criteriacontainer.CriteriaQueryDefinition;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;

/**
 * Example application demonstrating how to implement
 * queries and filtering by extending a query definition.
 * The filtering is managed in type-safe fashion by the query definition. 
 * 
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
public class EntityCustomFilteringApplication extends AbstractEntityApplication {
	
	class CustomFilteringQueryDefinition extends CriteriaQueryDefinition<Task> {
		
		/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
		private String nameFilterValue;


		/**
		 * Constructor.
		 * @param entityManager the entityManager that gives us access to the database and cache
		 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
		 * @param batchSize how many tuples to retrieve at once.
		 */
		public CustomFilteringQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
			super(entityManager, applicationManagedTransactions, batchSize, Task.class);
		}
		

		/**
		 * Define  the query.
		 */
        @Override
		protected Root<?> defineQuery (
				CriteriaBuilder criteriaBuilder,
				CriteriaQuery<?> cq) {
           
            
		    Root<Task> t = cq.from(Task.class);
		    cq.multiselect(t);
		    
			if (nameFilterValue != null && !nameFilterValue.isEmpty()) {

				// WHERE t.name LIKE ...
				Predicate condition = criteriaBuilder.like(
						t.get(Task_.name), // t.name
						nameFilterValue);  // pattern to be matched
				cq.where(condition);
			}
			return t;
		}
		
		/* getters and setters */
		
		public String getNameFilterValue() {
			return nameFilterValue;
		}

		public void setNameFilterValue(String nameFilterValue) {
			this.nameFilterValue = nameFilterValue;
		}

	}
	
	private CustomFilteringQueryDefinition cd;

	/**
	 * @return the container used to feed the table
	 */
	@Override
	protected CriteriaContainer<Task> createTaskContainer() {
		cd = new CustomFilteringQueryDefinition(entityManager,true,10);
		CriteriaContainer<Task> taskContainer = new CriteriaContainer<Task>(cd);
		
		// define the key so that getValue() returns the taskId column (a "Long")
		// and not the index.  This allows using the selected value as a foreign key.
        String keyName = Task_.taskId.getName();
        taskContainer.setKeyPropertyId(keyName);
		addSpecialProperties(taskContainer);
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
			cd.refresh(); // recompute the query
			criteriaContainer.refresh();
		} else {
			cd.setNameFilterValue(null);
			cd.refresh(); // recompute the query
			criteriaContainer.refresh();   
		}
	}
	
	/* Define visible columns, in accordance with the properties defined by the query definition.
     * 
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
        
        table.setMultiSelect(false);
        table.addListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                // the value of a table is the set of selected items.
                // the container is set to return the taskId (Long) for each row
                Property property = event.getProperty();
                if (property != null){
                    Object selectedId = property.getValue();
                    getMainWindow().showNotification(MessageFormat.format("selected value {0} {1}",selectedId,selectedId.getClass()));
                }

            }
        });
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
