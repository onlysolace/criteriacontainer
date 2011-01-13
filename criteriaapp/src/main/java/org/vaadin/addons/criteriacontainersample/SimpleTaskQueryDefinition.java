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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.vaadin.addons.criteriacontainer.CritQueryDefinition;
import org.vaadin.addons.criteriacontainer.CriteriaContainer;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;

/**
 * Type-safe implementation of a query definition.
 * 
 * Uses JPA2.0 Criteria mechanisms to create a safe version of the query that can be validated
 * at compile time.
 * 
 * This version integrates with the filter() mechanism.
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class SimpleTaskQueryDefinition extends CritQueryDefinition<Task> {
	
	/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
	private String nameFilterValue;


	/**
	 * Constructor.
	 * @param entityManager the entityManager that gives us access to the database and cache
	 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
	 * @param batchSize how many tuples to retrieve at once.
	 */
	public SimpleTaskQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
		super(entityManager, applicationManagedTransactions, Task.class, batchSize);
	}
	

	/**
	 * Define filtering conditions for the query.
	 * 
	 * A list of predicates is created. The container will add these predicates to those created by the
	 * {@link CriteriaContainer#filter(java.util.LinkedList)} mechanism.
	 */
	@Override
	protected List<Predicate> addPredicates(
			List<Predicate> filterExpressions, 
			CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Task> t) {
		if (nameFilterValue != null && !nameFilterValue.isEmpty()) {	
			// WHERE t.name LIKE ...
			Predicate condition = cb.like(
					t.get(Task_.name), // t.name
					nameFilterValue);  // pattern to be matched
			filterExpressions.add(condition);
		}
		return filterExpressions;
	}
	
	/* getters and setters */
	
	public String getNameFilterValue() {
		return nameFilterValue;
	}

	public void setNameFilterValue(String nameFilterValue) {
		this.nameFilterValue = nameFilterValue;
	}





}
