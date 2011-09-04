package org.vaadin.addons.criteriacontainersample.data;

import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Task.class)
public abstract class Task_ {

	public static volatile SingularAttribute<Task, Long> taskId;
	public static volatile SingularAttribute<Task, String> reporter;
	public static volatile SingularAttribute<Task, String> gamma;
	public static volatile SingularAttribute<Task, String> name;
	public static volatile SingularAttribute<Task, String> delta;
	public static volatile SingularAttribute<Task, String> alpha;
	public static volatile SetAttribute<Task, Person> assignedTo;
	public static volatile SingularAttribute<Task, String> assignee;
	public static volatile SingularAttribute<Task, String> beta;

}

