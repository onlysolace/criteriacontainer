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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;

import org.vaadin.addons.beantuplecontainer.BeanTupleContainer;
import org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition;
import org.vaadin.addons.criteriacontainersample.EntityCustomFilteringApplication.CustomFilteringQueryDefinition;
import org.vaadin.addons.criteriacontainersample.data.Person;
import org.vaadin.addons.criteriacontainersample.data.Person_;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;
import org.vaadin.addons.criteriacore.FilterRestriction;

import com.vaadin.ui.Button.ClickListener;

/**
 * Demonstrate use of {@link BeanTupleContainer}.
 * 
 * This container fetches a tuple containing Entities. The properties of the
 * container refer either to the fields of the entities or to additional columns
 * retrieved by the query.
 * 
 * @author Modified by Jean-François Lamy
 */

public class BeanTupleSimpleFilteringApplication extends AbstractBeanTupleApplication implements ClickListener {
	private static final long serialVersionUID = 1L;

	private SimpleFilteringBeanTupleQueryDefinition cd;

	class SimpleFilteringBeanTupleQueryDefinition extends BeanTupleQueryDefinition {
		
		/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
		private String nameFilterValue = null;
		private SetJoin<Person, Task> task;


		/**
		 * Constructor.
		 * @param entityManager the entityManager that gives us access to the database and cache
		 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
		 * @param batchSize how many tuples to retrieve at once.
		 */
		public SimpleFilteringBeanTupleQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
			super(entityManager, applicationManagedTransactions, batchSize);
		}
		

		/** 
		 * Define the query to be executed.
		 * The container will add the restrictions from container filters, and apply the ordering
		 * defined by the container.
		 * 
		 * @see org.vaadin.addons.criteriacore.AbstractCriteriaQueryDefinition#defineQuery(javax.persistence.criteria.CriteriaBuilder, javax.persistence.criteria.CriteriaQuery)
		 */
		@Override
		protected Root<?> defineQuery(
				CriteriaBuilder cb,
				CriteriaQuery<?> cq) {
			
			// FROM task JOIN PERSON 
			Root<Person> person = (Root<Person>) cq.from(Person.class);
			task = person.join(Person_.tasks); 
			
			// SELECT task as Task, person as Person, ... 
			cq.multiselect(task,person);
			
			// WHERE t.name LIKE nameFilterValue
			if (nameFilterValue != null && !nameFilterValue.isEmpty()) {	
				cq.where(
						cb.like(
								task.get(Task_.name), // t.name
								nameFilterValue)  // pattern to be matched?
				);
			}

			return person;
		}


		/**
		 * @return the filtering string currently applied to the task name
		 */
		public String getNameFilterValue() {
			return nameFilterValue;
		}

		/**
		 * Set the filtering string for restricting task names
		 * @param nameFilterValue
		 */
		public void setNameFilterValue(String nameFilterValue) {
			this.nameFilterValue = nameFilterValue;
		}
	}

	@Override
	protected void defineTableColumns() {
		visibleColumnIds.add(cd.getPropertyId(Task_.class, Task_.taskId));
		visibleColumnLabels.add("Task ID");
		
		visibleColumnIds.add(cd.getPropertyId(Task_.class, Task_.name));
		visibleColumnLabels.add("Name");
		
		visibleColumnIds.add(cd.getPropertyId(Person_.class, Person_.firstName));
		visibleColumnLabels.add("Assignee First Name");
		
		visibleColumnIds.add(cd.getPropertyId(Person_.class, Person_.lastName));
		visibleColumnLabels.add("Assignee Last Name");
		
		table.setVisibleColumns(visibleColumnIds.toArray());
		table.setColumnHeaders(visibleColumnLabels.toArray(new String[0]));
	}
	
	
	
	/**
	 * @return
	 */
	@Override
	protected BeanTupleContainer createTupleContainer() {
		cd = new SimpleFilteringBeanTupleQueryDefinition(entityManager,true,100);
		BeanTupleContainer tupleContainer = new BeanTupleContainer(cd);

		return tupleContainer;
	}

	

    /**
     * Get a query definition with filtering activated
     * 
     * In this version, we use the generic filtering mechanism provided
     * by CriteriaContainer.
     * 
     * @see {@link EntityCustomFilteringApplication} and {@link CustomFilteringQueryDefinition} for
     * an alternate approach where arbitrary complex filtering can be done through methods.
     */
    @Override
    protected void doFiltering() {
        // get filtering string from the user interface
        final String nameFilterValue = (String) nameFilterField.getValue();
        
        // if value define add the filtering conditions, else remove them.
        if (nameFilterValue != null && nameFilterValue.length() != 0) {
            // filtering style #2
            // simple conditions are added to a list and passed to the filter mechanism.
            final LinkedList<FilterRestriction> restrictions = new LinkedList<FilterRestriction>();
            restrictions.add(new FilterRestriction(Task_.name.getName(), FilterRestriction.Operation.LIKE, nameFilterValue));
            criteriaContainer.filter(restrictions);
        } else {
            criteriaContainer.filter((LinkedList<FilterRestriction>)null);          
        }
    }



	
	
}