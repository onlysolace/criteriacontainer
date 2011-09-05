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
package org.vaadin.addons.criteriacore;

import java.sql.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.util.filter.And;
//import com.vaadin.data.util.filter.Between;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.filter.Compare.Equal;
import com.vaadin.data.util.filter.IsNull;
//import com.vaadin.data.util.filter.Like;
import com.vaadin.data.util.filter.Not;
import com.vaadin.data.util.filter.Or;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.data.util.filter.UnsupportedFilterException;

/**
 * Translate a Vaadin Filter structure to the equivalent JPA Predicate structure
 * 
 * @author jflamy
 * 
 */
@SuppressWarnings("unused")
public class FilterTranslator {


	private static Logger logger = LoggerFactory.getLogger(FilterTranslator.class);


	/**
	 * Return a JPA Predicate for a given Vaadin filter.
	 * @param f 
	 *        the filter being translated to a JPA Criteria predicate.
	 * @param cb
	 *        the criteria builder for the CriteriaQuery being built
	 * @param qd
	 *        the query definition being built
	 * @param expressionMap
	 *        where to lookup expressions by id.
	 * @return a JPA 2.0 Predicate for the restriction
	 */
	@SuppressWarnings("unchecked")
	public static Predicate getPredicate(Filter f, CriteriaBuilder cb, BeanTupleQueryDefinition qd, Map<Object, Expression<?>> expressionMap) {
		Predicate pred = null;
		if (f instanceof Compare) {
			final Compare compareFilter = (Compare) f;
			Compare.Operation operation = compareFilter.getOperation();
			Object value = compareFilter.getValue();
			String propertyId = (String) compareFilter.getPropertyId();

			switch (operation) {
			case EQUAL: {
				Equal eqFilter = (Equal)f;
				final Expression<?> expr = qd.getExpressionById(eqFilter.getPropertyId().toString(), expressionMap);
				pred = cb.equal(expr, value);
			}
			break;
			case GREATER_OR_EQUAL: {
				if (value instanceof Number) {
					final Expression<Number> expr = (Expression<Number>) qd.getExpressionById(propertyId, expressionMap);
					pred = cb.ge(expr, (Number) value);
				} else if (value instanceof String) {
					Expression<String> expr2 = qd.getExpressionById(propertyId, expressionMap).as(String.class);
					pred = cb.greaterThanOrEqualTo(expr2, (String) value);
				}
			}
			break;
			case GREATER: {
				if (value instanceof Number) {
					final Expression<Number> expr = (Expression<Number>) qd.getExpressionById(propertyId, expressionMap);
					pred = cb.gt(expr, (Number) value);
				} else if (value instanceof String) {
					Expression<String> expr2 = qd.getExpressionById(propertyId, expressionMap).as(String.class);
					pred = cb.greaterThan(expr2, (String) value);
				}
			}
			break;
			case LESS_OR_EQUAL: {
				if (value instanceof Number) {
					final Expression<Number> expr = (Expression<Number>) qd.getExpressionById(propertyId, expressionMap);
					pred = cb.le(expr, (Number) value);
				} else if (value instanceof String) {
					Expression<String> expr2 = qd.getExpressionById(propertyId, expressionMap).as(String.class);
					pred = cb.lessThanOrEqualTo(expr2, (String) value);
				}
			}
			break;
			case LESS: {
				if (value instanceof Number) {
					final Expression<Number> expr = (Expression<Number>) qd.getExpressionById(propertyId, expressionMap);
					pred = cb.lt(expr, (Number) value);
				} else if (value instanceof String) {
					Expression<String> expr2 = qd.getExpressionById(propertyId, expressionMap).as(String.class);
					pred = cb.lessThan(expr2, (String) value);
				}
			}
			break;
			}
		} 
//		else if (f instanceof Like) {
//			final Like filter = (Like) f;
//			final Expression<String> expr = (Expression<String>) qd.getExpressionById((String) filter.getPropertyId(), expressionMap);
//			pred = cb.like(expr, filter.getValue());
//		} 
		else if (f instanceof IsNull) {
			final IsNull filter = (IsNull) f;
			final Expression<?> expr = qd.getExpressionById((String) filter.getPropertyId(), expressionMap);
			pred = cb.isNull(expr);
		} else if (f instanceof Not) {
			Not notFilter = (Not)f;
			Filter negatedFilter = notFilter.getFilter();
			Predicate toBeNegated = getPredicate(negatedFilter,cb,qd,expressionMap);
			pred = cb.not(toBeNegated);
		} else if (f instanceof And) {
			And andFilter = (And)f;
			List<Predicate> andedPredicates = new LinkedList<Predicate>();
			for (Filter subFilter : andFilter.getFilters()) {
				andedPredicates.add(getPredicate(subFilter, cb, qd, expressionMap));
			}
			pred = cb.and(andedPredicates.toArray(new Predicate[]{}));
		} else if (f instanceof Or) {
			Or orFilter = (Or)f;
			List<Predicate> oredPredicates = new LinkedList<Predicate>();
			for (Filter subFilter : orFilter.getFilters()) {
				oredPredicates.add(getPredicate(subFilter, cb, qd, expressionMap));
			}
			pred = cb.or(oredPredicates.toArray(new Predicate[]{}));
		}
//		else if (f instanceof Between) {
//			Between betweenFilter = (Between)f;
//			Comparable<?> exprL = betweenFilter.getStartValue();
//			Comparable<?> exprH = betweenFilter.getEndValue();
//			String propertyId2 = (String) betweenFilter.getPropertyId();
//			if (exprL instanceof Integer && exprH instanceof Integer) {
//				Expression<Integer> nexpr = (Expression<Integer>) qd.getExpressionById(propertyId2, expressionMap);
//				Integer exprL2 = (Integer)exprL;
//				Integer exprH2 = (Integer)exprH;
//				pred = cb.between(nexpr, exprL2, exprH2);
//			} else if (exprL instanceof Long && exprH instanceof Long) {
//				Expression<Long> nexpr = (Expression<Long>) qd.getExpressionById(propertyId2, expressionMap);
//				Long exprL2 = (Long)exprL;
//				Long exprH2 = (Long)exprH;
//				pred = cb.between(nexpr, exprL2, exprH2);
//			} else if (exprL instanceof Float && exprH instanceof Float) {
//				Expression<Float> nexpr = (Expression<Float>) qd.getExpressionById(propertyId2, expressionMap);
//				Float exprL2 = (Float)exprL;
//				Float exprH2 = (Float)exprH;
//				pred = cb.between(nexpr, exprL2, exprH2);
//			} else if (exprL instanceof Double && exprH instanceof Double) {
//				Expression<Double> nexpr = (Expression<Double>) qd.getExpressionById(propertyId2, expressionMap);
//				Double exprL2 = (Double)exprL;
//				Double exprH2 = (Double)exprH;
//				pred = cb.between(nexpr, exprL2, exprH2);
//			} else if (exprL instanceof String && exprH instanceof String) {
//				final Expression<String> expr = (Expression<String>) qd.getExpressionById(propertyId2, expressionMap);
//				pred = cb.between(expr,(String)exprL,(String)exprH);					
//			} else if (exprL instanceof Date && exprH instanceof Date) {
//				final Expression<Date> expr = (Expression<Date>) qd.getExpressionById(propertyId2, expressionMap);
//				pred = cb.between(expr,(Date)exprL,(Date)exprH);					
//			} else {
//				throw new UnsupportedFilterException(f.getClass().getName()+" with arguments of mismatched or unsupported types");
//			}
//		}
		else if (f instanceof SimpleStringFilter) {
			final SimpleStringFilter filter = (SimpleStringFilter)f;
			Expression<?> expr = qd.getExpressionById((String) filter.getPropertyId(), expressionMap);
			String value = filter.getFilterString();
			if (filter.isIgnoreCase()) {
				expr = cb.upper((Expression<String>) expr);
				value = value.toUpperCase();
			}
			if (filter.isOnlyMatchPrefix()) {
				pred = cb.like((Expression<String>) expr, value+"%");
			} else {
				pred = cb.like((Expression<String>) expr, "%"+value+"%");
			}
		} else {
			throw new UnsupportedFilterException(f.getClass().getName());
		}
		return pred;
	}  

}
