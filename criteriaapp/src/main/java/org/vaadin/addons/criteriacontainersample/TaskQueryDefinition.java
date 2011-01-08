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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.vaadin.addons.criteriacontainer.CritQueryDefinition;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;

/**
 * Type-safe implementation of a query definition.
 * 
 * Uses JPA2.0 Criteria mechanisms to create a safe version of the query that can be validated
 * at compile time.
 * 
 * This version is the most straightforward usage, and is useful if the filter() method is not used.
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class TaskQueryDefinition extends CritQueryDefinition<Task> {
	
	/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
	private String nameFilterValue;

	/**
	 * Constructor.
	 * @param entityManager the entityManager that gives us access to the database and cache
	 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
	 * @param batchSize how many tuples to retrieve at once.
	 */
	public TaskQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
		// We pass Task.class because the parameterized type of this class is <Task>
		super(entityManager, applicationManagedTransactions, Task.class, batchSize);
	}
	

	/** 
	 * Define the query to be executed.
	 * 
	 * This class creates the equivalent of
	 * SELECT * FROM Task t WHERE t.name LIKE "..."
	 * 
	 * More precisely, the query by this method should not call cb.select() and cb.setOrdering().
	 * The default implementations of {@link #getCountQuery()} and {@link #getSelectQuery()} both
	 * call this method in order to guarantee that they ar consistent with one another.
	 * 
	 * @see org.vaadin.addons.criteriacontainer.CritQueryDefinition#defineQuery(javax.persistence.criteria.CriteriaBuilder, javax.persistence.criteria.CriteriaQuery)
	 */
	@Override
	protected Root<Task> defineQuery(CriteriaBuilder cb, CriteriaQuery<?> cq) {
		// select * FROM t 
		Root<Task> t = cq.from(Task.class);
		if (nameFilterValue != null && !nameFilterValue.isEmpty()) {	
			// WHERE t.name LIKE ...
			cq.where(
					cb.like(
							t.get(Task_.name), // t.name
							nameFilterValue)  // pattern to be matched
			);
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

