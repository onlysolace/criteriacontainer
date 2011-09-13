/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vaadin.addons.criteriacontainersample.data;

import java.io.Serializable;
import java.math.BigInteger;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlRootElement;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 
 * @author pbelb
 */
@Entity
@Table(name = "category", uniqueConstraints = { @UniqueConstraint(columnNames = { "category" }) })
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "Category.findAll", query = "SELECT c FROM Category c"),
        @NamedQuery(name = "Category.findByUid", query = "SELECT c FROM Category c WHERE c.uid = :uid"),
        @NamedQuery(name = "Category.findByCategory", query = "SELECT c FROM Category c WHERE c.category = :category") })
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "uid", nullable = false)
    private BigInteger uid;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 60)
    @Column(name = "category", nullable = false, length = 60)
    private String category;

    public Category() {
    }

    public Category(BigInteger uid) {
        this.uid = uid;
    }

    public Category(BigInteger uid, String category) {
        this.uid = uid;
        this.category = category;
    }

    public BigInteger getUid() {
        return uid;
    }

    public void setUid(BigInteger uid) {
        this.uid = uid;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (uid != null ? uid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are
        // not set
        if (!(object instanceof Category)) {
            return false;
        }
        Category other = (Category) object;
        if ((this.uid == null && other.uid != null)
                || (this.uid != null && !this.uid.equals(other.uid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getName()+ "[ uid=" + uid + ", " + category +" ]";
    }

}
