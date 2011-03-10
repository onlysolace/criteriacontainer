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
package org.vaadin.addons.beantuplecontainer;

import java.util.Map;

/**
 * @author Xwave-Demo
 *
 */
public interface KeyToIdMapper {
    
    /**
     * @return the propertyId to be used as key
     */
    public Object getKeyPropertyId();
    
    /**
     * @return the map between keys in the entity and the index in the container
     */
    public Map<Object, Integer> getKeyToId();

}