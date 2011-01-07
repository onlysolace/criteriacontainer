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
package org.vaadin.addons.criteriacontainer;

import java.util.Collection;

import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.Attribute;


/**
 * Define simple restrictions to be added to the WHERE clause
 * 
 * Useful because we cannot defined JPA2.0 Expressions independently of an EntityManager.
 * This class creates JPA2.0 predicates based on a list of simple criteria.
 * 
 * This reduces the need to create subclasses of {@link CritQueryDefinition}.
 * 

 * 
 * @author jflamy
 *
 */
public class CritRestriction {
	
	private String propertyId;
	private Operation operator;
	private Object value;
	
	
	/**
	 * List of supported operators
	 * 
	 * Operators GE, GT, LE and LT work on both numbers and strings.
	 * Operators IS_TRUE and IS_FALSE work on booleans.
	 *
	 */
	public enum Operation {
		EQ, GE, GT, LE, LT, LIKE, IS_NULL, IS_NOT_NULL, IS_TRUE, IS_FALSE;
	}
	
	
	/**
	 * A restriction to be added to the WHERE clause.
	 * 
	 * The type of the property is assumed to be the same as the comparison value.
	 * The comparison value is ignored for the "IS_" operations where it is meaningless.
	 * 
	 * @param propertyId
	 * 			 Note that in order to be type-safe, the restrictions should be created using the 
	 * 			 {@link Attribute#getName()} method, never with a hard-coded string. For example
	 * 			 Customer_.telephone.getName()  instead of "telephone", to prevent errors if the
	 * 			 field telephone is renamed.
	 * @param operator
	 * @param comparisonValue  for binary operators, the value being compared to, else null.
	 */
	public CritRestriction(String propertyId, Operation operator, Object comparisonValue) {
		this.propertyId = propertyId;
		this.operator = operator;
		this.value = comparisonValue;
	}

	/**
	 * Return a predicate for a given restriction.
	 *  
	 * @param cb the criteria builder for the CriteriaQuery in which the predicate will be used
	 * @param r	typically, the root of the Query, or a join.
	 * @return a JPA 2.0 Predicate for the restriction
	 */
	Predicate getPredicate(CriteriaBuilder cb, Path<?> r) {
		try {
			Predicate pred = null;
			switch (operator) {
			case EQ: {
				final Expression<?> expr = r.get(propertyId);
				pred = cb.equal(expr,value);
			}
			break;
			case GE: {
				if (value instanceof Number) {
					final Expression<Number> expr = r.get(propertyId);
					pred = cb.ge(expr,(Number)value);
				} else if (value instanceof String) {
					Expression<String> expr2 = r.get(propertyId).as(String.class);
					pred = cb.greaterThanOrEqualTo(expr2,(String)value);
				}
			}
			break;
			case GT: {
				if (value instanceof Number) {
					final Expression<Number> expr = r.get(propertyId);
					pred = cb.gt(expr,(Number)value);
				} else if (value instanceof String) {
					Expression<String> expr2 = r.get(propertyId).as(String.class);
					pred = cb.greaterThan(expr2,(String)value);
				}
			}
			break;
			case LE: {
				if (value instanceof Number) {
					final Expression<Number> expr = r.get(propertyId);
					pred = cb.le(expr,(Number)value);
				} else if (value instanceof String) {
					Expression<String> expr2 = r.get(propertyId).as(String.class);
					pred = cb.lessThanOrEqualTo(expr2,(String)value);
				}
			}
			break;
			case LT: {
				if (value instanceof Number) {
					final Expression<Number> expr = r.get(propertyId);
					pred = cb.lt(expr,(Number)value);
				} else if (value instanceof String) {
					Expression<String> expr2 = r.get(propertyId).as(String.class);
					pred = cb.lessThan(expr2,(String)value);
				}
			}
			break;
			case LIKE: {
				final Expression<String> expr = r.get(propertyId);
				final String val = (String)value;
				pred = cb.like(expr,val);
			}
			break;
			case IS_NULL: {
				final Expression<?> expr = r.get(propertyId);
				pred = cb.isNull(expr);
			}
			break;
			case IS_NOT_NULL: {
				final Expression<?> expr = r.get(propertyId);
				pred = cb.isNotNull(expr);
			}
			break;
			case IS_TRUE: {
				final Expression<Boolean> expr = r.get(propertyId);
				pred = cb.isTrue(expr);
			}
			break;
			case IS_FALSE: {
				final Expression<Boolean> expr = r.get(propertyId);
				pred = cb.isFalse(expr);
			}
			break;
			}
			return pred;
		} catch (Exception e) {
			throw new PersistenceException("Unknown property: "+propertyId,e);
		}
	}
	
	/**
	 * Create a condition that can be added to the where clause of a query.
	 * The condition is the conjunction of all the restrictions in the list (AND).
	 * 
	 * @param restrictions
	 * @param cb
	 * @param r
	 * @return
	 */
	static Predicate getPredicate(Collection<CritRestriction> restrictions, CriteriaBuilder cb, Path<?> r) {
		Predicate[] workList = new Predicate[restrictions.size()];
		int i = 0;
		for (CritRestriction curRestr : restrictions) {
			workList[i] = curRestr.getPredicate(cb, r);
			i++;
		}
		return cb.and(workList);
	}

}
