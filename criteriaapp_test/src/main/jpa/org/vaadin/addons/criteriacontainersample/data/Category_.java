package org.vaadin.addons.criteriacontainersample.data;

import java.math.BigInteger;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Category.class)
public abstract class Category_ {

	public static volatile SingularAttribute<Category, BigInteger> uid;
	public static volatile SingularAttribute<Category, String> category;

}

