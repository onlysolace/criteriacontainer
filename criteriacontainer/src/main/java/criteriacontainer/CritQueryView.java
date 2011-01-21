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
import org.vaadin.addons.beantuplecontainer.BeanTupleQueryView;


/**
 * Wrapper around the LazyQueryView.
 * 
 * For some reason, LazyQueryView was defined as final.
 * 
 * @author jflamy
 * 
 * @param <T> the type of entity (class annotated by @Entity returned by the Query
 *
 */
@SuppressWarnings("serial")
public class CritQueryView<T> extends BeanTupleQueryView {


    /**
     * Constructs LazyQueryView with given QueryDefinition and QueryFactory. The
     * role of this constructor is to enable use of custom QueryDefinition
     * implementations.
     * @param queryDefinition The QueryDefinition to be used.
     * @param queryFactory The QueryFactory to be used.
     */
    public CritQueryView(final CritQueryDefinition<T> queryDefinition, final CritQueryFactory<T> queryFactory) {
        super(queryDefinition,queryFactory);
    }

	

}
