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

import org.vaadin.addons.beantuplecontainer.BeanTupleQueryFactory;
import org.vaadin.addons.lazyquerycontainer.Query;

/**
 * Create a query based on a query definition.
 * 
 * The query object returned contains the actual JPA 2.0 query and the context
 * necessary to run it (e.g., the entity manager)
 *
 * @param <T> the type of the entity that the query will return
 */
public class CriteriaQueryFactory<T> extends BeanTupleQueryFactory {
    
    @Override
    public Query constructQuery(Object[] sortPropertyIds, boolean[] sortStates) {
        if (beanTupleQueryView == null) {
            throw new RuntimeException("BeanTupleQueryFactory not initialized: setKeyToIdMapper() was not called.");
        }
        return new CriteriaItemHelper<T>(queryDefinition,beanTupleQueryView);
    }
}
