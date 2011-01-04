package org.vaadin.addons.criteriacontainersample;

import java.util.List;

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
import org.vaadin.addons.criteriacontainersample.data.Task_;

/**
 * Type-safe implementation of a query definition.
 * 
 * More complex example that shows how to use parameters.  
 * 
 * The method {@link #setParameters(TypedQuery)} shows how to integrate with the parameter
 * management done by the container.
 * 
 * The method {@link #addPredicates(List, CriteriaBuilder, CriteriaQuery, Root)} shows how
 * to integrated with additional filtering that can be performed by the container.
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class ParameterizedTaskQueryDefinition extends CritQueryDefinition<Task> {

	/** parameter to be set at query time */
	private ParameterExpression<String> nameFilter;
	
	/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
	private String nameFilterValue;

	@SuppressWarnings("unused")
	final static private Logger logger = LoggerFactory.getLogger(ParameterizedTaskQueryDefinition.class);

	/**
	 * 
	 * @param applicationManagedTransactions
	 * @param batchSize
	 */
	public ParameterizedTaskQueryDefinition(boolean applicationManagedTransactions, int batchSize) {
		super(applicationManagedTransactions, Task.class, batchSize);
	}
	

	/**
	 * Define filtering conditions for the query.
	 * 
	 * If the {@link CriteriaBuilder#parameter(Class)} is used in this method, then
	 * the {@link #setParameters(TypedQuery)} method must set the parameter values.
	 * Note that using parameters is currently not very useful, because the refresh() method
	 * throws out the query.
	 */
	@Override
	protected List<Predicate> addPredicates(List<Predicate> filterExpressions, CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Task> t) {
		if (nameFilterValue != null && !nameFilterValue.isEmpty()) {

			// This example shows how to add a parameter to the query so the query does not have to be changed.
			Expression<String> nameField = t.get(Task_.name);
			nameFilter = cb.parameter(String.class);
			filterExpressions.add(cb.like(nameField,nameFilter));
		}
		return filterExpressions;

	}
	
	/**
	 * Set values for the parameters used in the predicates.
	 * 
	 * Should provide a value for all the parameter objects added in the {@link #addPredicates(List, CriteriaBuilder, CriteriaQuery, Root)}
	 * method.  Should call super() to handle named parameters defined through {@link #setNamedParameterValues(java.util.Map)}.
	 */
	@Override
	public TypedQuery<?> setParameters(final TypedQuery<?> tq) {
		super.setParameters(tq);
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
