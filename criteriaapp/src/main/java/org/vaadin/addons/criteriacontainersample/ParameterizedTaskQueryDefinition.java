package org.vaadin.addons.criteriacontainersample;

import java.util.List;

import javax.persistence.EntityManager;
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
 * to integrate with additional filtering that can be performed by the container.
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
	 * Constructor.
	 * @param entityManager the entityManager that gives us access to the database and cache
	 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
	 * @param batchSize how many tuples to retrieve at once. 
	 */
	public ParameterizedTaskQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
		super(entityManager, applicationManagedTransactions, Task.class, batchSize);
	}
	

	/**
	 * Define the WHERE clause conditions specific to this query.
	 * 
	 * In this example, the {@link CriteriaBuilder#parameter(Class)} is used to define a parameter
	 * place holder.  In such a case, the {@link #setParameters(TypedQuery)} method must set the parameter values.
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
	 * method.  Is expected to also call super() to handle named parameters defined through {@link #setNamedParameterValues(java.util.Map)}.
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
