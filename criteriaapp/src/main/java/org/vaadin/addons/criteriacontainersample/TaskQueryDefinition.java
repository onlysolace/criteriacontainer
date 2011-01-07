package org.vaadin.addons.criteriacontainersample;

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
	 * 
	 * @param applicationManagedTransactions
	 * @param batchSize
	 */
	public TaskQueryDefinition(boolean applicationManagedTransactions, int batchSize) {
		// We pass Task.class because the parameterized type of this class is <Task>
		super(applicationManagedTransactions, Task.class, batchSize);
	}
	

	/** 
	 * Define the query to be executed.
	 * 
	 * This class creates the equivalent of
	 * SELECT * FROM Task t WHERE t.name LIKE "..."
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

