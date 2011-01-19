/**
 * Copyright 2011 Tommi S.E. Laukkanen
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
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;

import org.vaadin.addons.criteriacontainer.CritRestriction;
import org.vaadin.addons.criteriacontainersample.data.Person;
import org.vaadin.addons.criteriacontainersample.data.Person_;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;
import org.vaadin.addons.tuplecontainer.TupleContainer;
import org.vaadin.addons.tuplecontainer.TupleQueryDefinition;

import com.vaadin.ui.Button.ClickListener;

/**
 * Example application demonstrating the Lazy Query Container features.
 * @author Tommi S.E. Laukkanen
 * @author Modified by Jean-François Lamy
 */

public class TupleCustomFilteringApplication extends AbstractTupleApplication implements ClickListener {
	private static final long serialVersionUID = 1L;

	private CustomFilteringTupleQueryDefinition cd;


	class CustomFilteringTupleQueryDefinition extends TupleQueryDefinition {
		
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
		public CustomFilteringTupleQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
			// We pass Task.class because the parameterized type of this class is <Task>
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
				CriteriaQuery<Tuple> cq,
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
			// propertyExpression is used to ensure that the query and the item
			// are consistent (the propertyId is used as alias)
			cq.multiselect(
					propertyExpression(task, Task_.taskId, sortExpressions),
					propertyExpression("taskName", task, Task_.name, sortExpressions),
					propertyExpression(person, Person_.lastName, sortExpressions),
					propertyExpression(person, Person_.firstName, sortExpressions)
					);
			return person;
		}
		

		/* getters and setters */
		
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
	protected TupleContainer createTupleContainer() {
		cd = new CustomFilteringTupleQueryDefinition(entityManager,true,100);
		TupleContainer tupleContainer = new TupleContainer(cd);

		tupleContainer.addContainerProperty(Task_.taskId.getName(), Long.class, new Long(0), true, true);
		tupleContainer.addContainerProperty("taskName", String.class, "", true, true);
		tupleContainer.addContainerProperty(Task_.alpha.getName(), String.class, "", true, true);
		tupleContainer.addContainerProperty(Person_.lastName.getName(), String.class, "", true, true);
		tupleContainer.addContainerProperty(Person_.firstName.getName(), String.class, "", false, true);

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
}
