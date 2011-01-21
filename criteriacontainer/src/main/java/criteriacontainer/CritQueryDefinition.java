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
package criteriacontainer;

import javax.persistence.EntityManager;

import org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition;

/**
 * Definition of a JPA 2.0 Criteria Query.
 * 
 * Extends LazyQueryDefinition instead of EntityQueryDefinition to avoid passing the Entity class.
 * 
 * @author jflamy
 * 
 * @param <T> the Entity type returned by the query being defined
 */
abstract public class CritQueryDefinition<T> extends BeanTupleQueryDefinition {

    /**
     * @param entityManager the EntityManager to reach the database that contains T objects
     * @param applicationManagedTransactions true unless using J2EE container-managed transactions
     * @param batchSize how many items are retrieved at once
     */
    public CritQueryDefinition(EntityManager entityManager,
            boolean applicationManagedTransactions, int batchSize) {
        super(entityManager, applicationManagedTransactions, batchSize);
    }

}
