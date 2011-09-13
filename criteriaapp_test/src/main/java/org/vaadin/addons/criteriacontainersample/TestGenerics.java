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
package org.vaadin.addons.criteriacontainersample;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacontainer.CriteriaContainer;
import org.vaadin.addons.criteriacontainer.CriteriaQueryDefinition;
import org.vaadin.addons.criteriacontainersample.data.Category;

public class TestGenerics {
	protected static final String PERSISTENCE_UNIT = "vaadin-lazyquerycontainer-example";
	Logger logger = LoggerFactory.getLogger(TestGenerics.class);

    private class CriteriaData<T> {
        CriteriaQueryDefinition<T> cqd;
        CriteriaContainer<T> cc;

		CriteriaData(Class<T> tClass, EntityManager em) {
            
            String fn = tClass.getSimpleName();
            
			cqd = new CriteriaQueryDefinition<T>(em, true, 100, tClass);
            cc = new CriteriaContainer<T>(cqd);
            
            logger.debug("CriteriaData Constructor: container for {} has {} items.", fn, cc.size());
            Collection<?> cIds = cqd.getPropertyIds();
            logger.debug("{}: cIds is {}", fn, cIds);

            try {
                for (int x = 0; x < cc.size(); ++x) {
                    T e = cc.getEntity(x);
                    logger.debug("entity: {} = {}" + e.getClass().getSimpleName(), e.toString());
                }
            } catch (Exception e) {
                logger.error(fn + " while getting ID's: " + e.getMessage(), e);
            }
        }
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final EntityManager em = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT).createEntityManager();
		new TestGenerics().new CriteriaData<Category>(Category.class, em);
	}
}
