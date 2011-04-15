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

import java.util.Collection;
import java.util.Map;

import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.Attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition;

import com.vaadin.data.Property;

/**
 * Define simple restrictions to be added to the WHERE clause
 * 
 * Useful because we cannot defined JPA2.0 Expressions independently of an EntityManager. This class creates JPA2.0
 * predicates based on a list of simple criteria.
 * 
 * This reduces the need to create subclasses of {@link AbstractCriteriaQueryDefinition}.
 * 
 * 
 * 
 * @author jflamy
 * 
 */
public class FilterRestriction implements Property {

    /**
     * 
     */
    private static final long serialVersionUID = -4404152428238721179L;

    @SuppressWarnings("unused")
    private static Logger logger = LoggerFactory.getLogger(FilterRestriction.class);

    
    private boolean active;
    private String propertyId;
    private Operation operator;
    private Object value;

    private boolean readOnly;

    private String caption;

    /**
     * List of supported operators
     * 
     * Operators GE, GT, LE and LT work on both numbers and strings. Operators IS_TRUE and IS_FALSE work on booleans.
     * 
     */
    public enum Operation {
        /** = */
        EQ,
        /** >= */
        GE,
        /** > */
        GT,
        /** <= */
        LE,
        /** < */
        LT,
        /** LIKE */
        LIKE,
        /** IS NULL */
        IS_NULL,
        /** IS NOT NULL */
        IS_NOT_NULL,
        /** = TRUE */
        IS_TRUE,
        /** = FALSE */
        IS_FALSE,
        /** = IN */
        IN,
        /** = No filtering */
        NONE;
    }


    /**
     * A restriction to be added to the WHERE clause.
     * 
     * The type of the property is assumed to be the same as the comparison value. The comparison value is ignored for
     * the "IS_" operations where it is meaningless.
     * 
     * @param propertyId
     *        Note that in order to be type-safe, the restrictions should be created using the
     *        {@link Attribute#getName()} method, never with a hard-coded string. For example
     *        Customer_.telephone.getName() instead of "telephone", to prevent errors if the field telephone is renamed.
     * @param operator
     *        the constant that corresponds to the desired comparison
     * @param comparisonValue
     *        for binary operators, the value being compared to, else null.
     */
    public FilterRestriction(String propertyId, Operation operator, Object comparisonValue) {
        this.propertyId = propertyId;
        this.operator = operator;
        this.value = comparisonValue;
    }


    /**
     * Return a predicate for a given restriction.
     * 
     * @param cb
     *        the criteria builder for the CriteriaQuery in which the predicate will be used
     * @param expressionMap
     *        where to lookup expressions by id.
     * @return a JPA 2.0 Predicate for the restriction
     */
    @SuppressWarnings("unchecked")
    Predicate getPredicate(CriteriaBuilder cb, BeanTupleQueryDefinition qd, Map<Object, Expression<?>> expressionMap) {
        try {
            Predicate pred = null;
            switch (operator) {
                case EQ: {
                    final Expression<?> expr = qd.getExpressionById(propertyId, expressionMap);
                    pred = cb.equal(expr, value);
                }
                break;
                case GE: {
                    if (value instanceof Number) {
                        final Expression<Number> expr = (Expression<Number>) qd.getExpressionById(propertyId, expressionMap);
                        pred = cb.ge(expr, (Number) value);
                    } else if (value instanceof String) {
                        Expression<String> expr2 = qd.getExpressionById(propertyId, expressionMap).as(String.class);
                        pred = cb.greaterThanOrEqualTo(expr2, (String) value);
                    }
                }
                break;
                case GT: {
                    if (value instanceof Number) {
                        final Expression<Number> expr = (Expression<Number>) qd.getExpressionById(propertyId, expressionMap);
                        pred = cb.gt(expr, (Number) value);
                    } else if (value instanceof String) {
                        Expression<String> expr2 = qd.getExpressionById(propertyId, expressionMap).as(String.class);
                        pred = cb.greaterThan(expr2, (String) value);
                    }
                }
                break;
                case LE: {
                    if (value instanceof Number) {
                        final Expression<Number> expr = (Expression<Number>) qd.getExpressionById(propertyId, expressionMap);
                        pred = cb.le(expr, (Number) value);
                    } else if (value instanceof String) {
                        Expression<String> expr2 = qd.getExpressionById(propertyId, expressionMap).as(String.class);
                        pred = cb.lessThanOrEqualTo(expr2, (String) value);
                    }
                }
                break;
                case LT: {
                    if (value instanceof Number) {
                        final Expression<Number> expr = (Expression<Number>) qd.getExpressionById(propertyId, expressionMap);
                        pred = cb.lt(expr, (Number) value);
                    } else if (value instanceof String) {
                        Expression<String> expr2 = qd.getExpressionById(propertyId, expressionMap).as(String.class);
                        pred = cb.lessThan(expr2, (String) value);
                    }
                }
                break;
                case LIKE: {
                    final Expression<String> expr = (Expression<String>) qd.getExpressionById(propertyId, expressionMap);
                    final String val = (String) value;
                    pred = cb.like(expr, val);
                }
                break;
                case IS_NULL: {
                    final Expression<?> expr = qd.getExpressionById(propertyId, expressionMap);
                    pred = cb.isNull(expr);
                }
                break;
                case IS_NOT_NULL: {
                    final Expression<?> expr = qd.getExpressionById(propertyId, expressionMap);
                    pred = cb.isNotNull(expr);
                }
                break;
                case IS_TRUE: {
                    final Expression<Boolean> expr = (Expression<Boolean>) qd.getExpressionById(propertyId, expressionMap);
                    pred = cb.isTrue(expr);
                }
                break;
                case IS_FALSE: {
                    final Expression<Boolean> expr = (Expression<Boolean>) qd.getExpressionById(propertyId, expressionMap);
                    pred = cb.isFalse(expr);
                }
                break;
                case IN: {
                    final Expression<?> expr = qd.getExpressionById(propertyId, expressionMap);
                    pred = expr.in((Collection<?>)value);
                }
                case NONE: {
                    pred = cb.equal(cb.literal(1),cb.literal(1));
                }
                break;
            }
            return pred;
        } catch (Exception e) {
            throw new PersistenceException("Unknown property: " + propertyId, e);
        }
    }


    /**
     * Create a condition that can be added to the where clause of a query. The condition is the conjunction of all the
     * restrictions in the list (AND).
     * 
     * @param restrictions
     *        a list of FilterRestriction objects that denotes conditions to be added
     * @param cb
     *        the current CriteriaBuilder
     * @param qd
     *        the QueryDefinition on which we are acting
     * @param expressionMap
     *        where to locate the expressions
     * @return the predicate to be applied
     */
    public static Predicate getConjoinedPredicate(Collection<FilterRestriction> restrictions, CriteriaBuilder cb,
            BeanTupleQueryDefinition qd, Map<Object, Expression<?>> expressionMap) {
        if (restrictions == null)
            return null;

        Predicate[] workList = new Predicate[restrictions.size()];
        int i = 0;
        for (FilterRestriction curRestr : restrictions) {
            workList[i] = curRestr.getPredicate(cb, qd, expressionMap);
            i++;
        }
        Predicate andPredicate = cb.and(workList);
        return andPredicate;
    }


    /* (non-Javadoc)
     * @see com.vaadin.data.Property#getValue()
     */
    @Override
    public Object getValue() {
        return value;
    }


    /* (non-Javadoc)
     * @see com.vaadin.data.Property#setValue(java.lang.Object)
     */
    @Override
    public void setValue(Object newValue) throws ReadOnlyException, ConversionException {
        value = newValue;
    }


    /* (non-Javadoc)
     * @see com.vaadin.data.Property#getType()
     */
    @Override
    public Class<?> getType() {
        return value.getClass();
    }


    /* (non-Javadoc)
     * @see com.vaadin.data.Property#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return readOnly;
    }


    /* (non-Javadoc)
     * @see com.vaadin.data.Property#setReadOnly(boolean)
     */
    @Override
    public void setReadOnly(boolean newStatus) {
        readOnly = newStatus;
    }


    /**
     * @return the operator
     */
    public Operation getOperator() {
        return operator;
    }


    /**
     * @param operator the operator to set
     */
    public void setOperator(Operation operator) {
        this.operator = operator;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }
    
    /**
     * @return pretty name
     */
    public String getName() {
        return getCaption()+": "+getValue();
    }


    /**
     * @param filterCaption the caption for the restriction
     */
    public void setCaption(String filterCaption) {
        this.caption = filterCaption;
    }


    /**
     * @return the caption
     */
    public String getCaption() {
        return caption;
    }


    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }


    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }
    
    

}
