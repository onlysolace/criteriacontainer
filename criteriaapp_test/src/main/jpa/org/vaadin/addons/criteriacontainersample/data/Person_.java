package org.vaadin.addons.criteriacontainersample.data;

import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Person.class)
public abstract class Person_ {

	public static volatile SingularAttribute<Person, String> lastName;
	public static volatile SingularAttribute<Person, Long> personId;
	public static volatile SetAttribute<Person, Task> tasks;
	public static volatile SingularAttribute<Person, String> firstName;

}

