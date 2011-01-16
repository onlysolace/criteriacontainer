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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;

import org.vaadin.addons.beantuplecontainer.BeanTupleContainer;
import org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition;
import org.vaadin.addons.criteriacontainer.CritRestriction;
import org.vaadin.addons.criteriacontainersample.data.Person;
import org.vaadin.addons.criteriacontainersample.data.Person_;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;

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

public class BeanTupleCustomFilteringApplication extends AbstractBeanTupleApplication implements ClickListener {
	private static final long serialVersionUID = 1L;

	private CustomFilteringBeanTupleQueryDefinition cd;

	@SuppressWarnings("serial")
	class CustomFilteringBeanTupleQueryDefinition extends BeanTupleQueryDefinition {
		
		/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
		private String nameFilterValue;
		private Root<Person> person;
		private SetJoin<Person, Task> task;

		/**
		 * Constructor.
		 * @param entityManager the entityManager that gives us access to the database and cache
		 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
		 * @param batchSize how many tuples to retrieve at once.
		 */
		public CustomFilteringBeanTupleQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
			super(entityManager, applicationManagedTransactions, batchSize);
		}
		

		/** 
		 * Define the query to be executed.
		 * 
		 * This class creates the equivalent of
		 * SELECT * FROM Task t WHERE t.name LIKE "..."
		 * 
		 * More precisely, the query by this method should not call cb.select() and cb.setOrdering().
		 * The default implementations of {@link #getCountQuery()} and {@link #getSelectQuery()} both
		 * call this method in order to guarantee that they are consistent with one another.
		 * 
		 * @see org.vaadin.addons.criteriacontainer.CritQueryDefinition#defineQuery(javax.persistence.criteria.CriteriaBuilder, javax.persistence.criteria.CriteriaQuery)
		 */
		@Override
		protected Root<?> defineQuery(
				CriteriaBuilder cb,
				CriteriaQuery<?> cq,
				Map<Object, Expression<?>> sortExpressions) {
			
			// FROM task JOIN PERSON 
			person = cq.from(Person.class);
			task = person.join(Person_.tasks); 
			
			// WHERE t.name LIKE nameFilterValue
			if (nameFilterValue != null && !nameFilterValue.isEmpty()) {	
				cq.where(
						cb.like(
								task.get(Task_.name), // t.name
								nameFilterValue)  // pattern to be matched?
				);
			}
			
			// SELECT task.taskId as taskId, task.name as taskName, ...
			List<Selection<?>> selections = new ArrayList<Selection<?>>();
			// must add all the entities selected in the tuple, each with an alias
			addEntitySelection(task,selections);
			addEntitySelection(person,selections);
			// add columns for the sortable properties of the container
			addSortSelections(sortExpressions, selections);
			
			cq.multiselect(selections.toArray(new Selection[0]));
			
			return person;
		}


		/**
		 * @param selections
		 */
		private void addEntitySelection(Path<?> path,List<Selection<?>> selections) {
			//TODO do this by introspection, if getJavaType() is annotated as entity
			selections.add(path.alias(path.getJavaType().getSimpleName()));
		}


		/**
		 * @param sortExpressions
		 * @param selections
		 */
		private void addSortSelections(
				Map<Object, Expression<?>> sortExpressions,
				List<Selection<?>> selections) {
			selections.add(sortExpression(task, Task_.taskId, sortExpressions));
			selections.add(sortExpression(task, Task_.name, sortExpressions));
			selections.add(sortExpression(task, Task_.alpha, sortExpressions));
			selections.add(sortExpression(person, Person_.lastName, sortExpressions));
			selections.add(sortExpression(person, Person_.firstName, sortExpressions));
		}
		
		public String getNameFilterValue() {
			return nameFilterValue;
		}

		public void setNameFilterValue(String nameFilterValue) {
			this.nameFilterValue = nameFilterValue;
		}
	}

	/**
	 * @return
	 */
	@Override
	protected BeanTupleContainer createTupleContainer() {
		cd = new CustomFilteringBeanTupleQueryDefinition(entityManager,true,100);
		BeanTupleContainer tupleContainer = new BeanTupleContainer(cd);

		final String taskPrefix = Task.class.getSimpleName();
		final String personPrefix = Person.class.getSimpleName();
		
		logger.warn("adding container property for {}",taskPrefix);
		tupleContainer.addContainerProperty(taskPrefix, Task.class, null, true, false);
		tupleContainer.addContainerProperty(personPrefix, Person.class, null, true, false);
		
		// one line is needed for each field extracted from an entity on which sorting is needed.
		tupleContainer.addContainerSortableProperty(taskPrefix, Task_.taskId, new Long(0), true);
		tupleContainer.addContainerSortableProperty(taskPrefix, Task_.name, "", true);
		tupleContainer.addContainerSortableProperty(taskPrefix, Task_.alpha, "", true);
		tupleContainer.addContainerSortableProperty(personPrefix, Person_.lastName, "", true);
		tupleContainer.addContainerSortableProperty(personPrefix, Person_.firstName, "", false);
		
		logger.warn("created container");
		return tupleContainer;
	}


	@Override
	protected void doFiltering() {
		final String nameFilterValue = (String) nameFilterField.getValue();
		if (nameFilterValue != null && nameFilterValue.length() != 0) {
			// filtering style #1: query definition includes type safe filters.
			// the query has its own specific mechanism for setting the filters up.
			cd.setNameFilterValue(nameFilterValue);
			// do not refresh if calling "filter()" later.
			criteriaContainer.refresh();

		} else {
			cd.setNameFilterValue(null);
			criteriaContainer.filter((LinkedList<CritRestriction>)null);          
		}
	}


	@Override
	protected void defineTableColumns() {
		visibleColumnIds.add(Task.class.getSimpleName()+"."+Task_.taskId.getName());
		visibleColumnIds.add(Task.class.getSimpleName()+"."+Task_.name.getName());
		visibleColumnIds.add(Person.class.getSimpleName()+"."+Person_.firstName.getName());
		visibleColumnIds.add(Person.class.getSimpleName()+"."+Person_.lastName.getName());
		
		visibleColumnLabels.add("Task ID");
		visibleColumnLabels.add("Name");
		visibleColumnLabels.add("Assignee First Name");
		visibleColumnLabels.add("Assignee Last Name");
		
		table.setVisibleColumns(visibleColumnIds.toArray());
		table.setColumnHeaders(visibleColumnLabels.toArray(new String[0]));
	}
	
	
}
