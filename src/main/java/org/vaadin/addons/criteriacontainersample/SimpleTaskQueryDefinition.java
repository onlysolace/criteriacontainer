package org.vaadin.addons.criteriacontainersample;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
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
public class SimpleTaskQueryDefinition extends CritQueryDefinition<Task> {
	
	/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
	private String nameFilterValue;


	/**
	 * Constructor.
	 * @param applicationManagedTransactions
	 * @param batchSize
	 */
	public SimpleTaskQueryDefinition(boolean applicationManagedTransactions, int batchSize) {
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
