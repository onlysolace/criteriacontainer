package org.vaadin.addons.criteriacontainersample;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
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
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class TaskQueryDefinition extends CritQueryDefinition<Task> {
	
	/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
	private String nameFilterValue;

	/**
	 * 
	 * @param applicationManagedTransactions
	 * @param batchSize
	 */
	public TaskQueryDefinition(boolean applicationManagedTransactions, int batchSize) {
		super(applicationManagedTransactions, Task.class, batchSize);
	}
	

	/**
	 * Define filtering conditions for the query.
	 */
	@Override
	protected List<Predicate> addPredicates(
			List<Predicate> filterExpressions, 
			CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Task> t) {
		if (nameFilterValue != null && !nameFilterValue.isEmpty()) {
			
			// the code above does the same as the following.
			Expression<String> nameField2 = t.get(Task_.name);
			filterExpressions.add(cb.like(nameField2,nameFilterValue));
		}
		return filterExpressions;

	}
	
	
	public String getNameFilterValue() {
		return nameFilterValue;
	}

	public void setNameFilterValue(String nameFilterValue) {
		this.nameFilterValue = nameFilterValue;
	}





}
