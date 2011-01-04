/**
 * Copyright 2010 Jean-François Lamy
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

import javax.persistence.EntityManager;

import org.vaadin.addons.lazyquerycontainer.Query;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;
import org.vaadin.addons.lazyquerycontainer.QueryFactory;

public class CritQueryFactory<T> implements QueryFactory {

	private CritQueryDefinition<T> queryDefinition;
	private EntityManager entityManager;
	

	public CritQueryFactory() {
	}
	
	public CritQueryFactory(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setQueryDefinition(QueryDefinition queryDefinition) {
		this.queryDefinition = (CritQueryDefinition<T>) queryDefinition;
	}

	@Override
	public Query constructQuery(Object[] sortPropertyIds, boolean[] sortStates) {
		return new CritQuery<T>(
				entityManager, 
				queryDefinition);
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

}
