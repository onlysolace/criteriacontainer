package org.vaadin.addons.criteriacontainersample;

import java.util.ArrayList;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacontainer.CritQueryDefinition;
import org.vaadin.addons.criteriacontainersample.data.Task;

import com.xwave.container.data.Task_;

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

	/** Placeholder to filter on name */
	private ParameterExpression<String> nameFilter;
	
	/** Value that will restrict the name (SQL like expression) */
	private String nameFilterValue;

	@SuppressWarnings("unused")
	final static private Logger logger = LoggerFactory.getLogger(TaskQueryDefinition.class);

	/**
	 * 
	 * @param applicationManagedTransactions
	 * @param batchSize
	 */
	public TaskQueryDefinition(boolean applicationManagedTransactions, int batchSize) {
		super(applicationManagedTransactions, Task.class, batchSize);
	}
	

	/* Define filtering conditions for the query.
	 * 
	 * This example shows how to add a parameterized to the query so the query does not have to be changed.
	 * The current implementation of the factory always gives a new query on refresh(), so this does not
	 * do much, but this proves the concept. 
	 * 
	 * @see org.vaadin.addons.criteriacontainer.CritQueryDefinition#addParameters(javax.persistence.criteria.CriteriaBuilder, javax.persistence.criteria.CriteriaQuery, javax.persistence.criteria.Root)
	 */
	@Override
	protected ArrayList<Predicate> addPredicates(ArrayList<Predicate> filterExpressions, CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Task> t) {
		if (nameFilterValue != null && !nameFilterValue.isEmpty()) {
			Expression<String> nameField = t.get(Task_.name);
			nameFilter = cb.parameter(String.class);
			filterExpressions.add(cb.like(nameField,nameFilter));
		}
		return filterExpressions;

	}
	
	/* Set values for the type-safe parameters
	 * @see org.vaadin.addons.criteriacontainer.CritQueryDefinition#setParameters(javax.persistence.TypedQuery)
	 */
	@Override
	public TypedQuery<?> setParameters(final TypedQuery<?> tq) {
		if (nameFilterValue != null && !nameFilterValue.isEmpty()) {
			tq.setParameter(nameFilter, nameFilterValue);
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
