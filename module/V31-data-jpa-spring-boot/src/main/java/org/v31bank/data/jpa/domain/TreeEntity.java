package org.v31bank.data.jpa.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * Base class for entities organized as a tree via a parent reference.
 * <p>
 * The {@code children} collection is not persisted; it is populated when a flat
 * list of nodes is assembled into a tree, typically by
 * {@link org.v31bank.data.jpa.support.TreeUtils#build}.
 *
 * @param <T> the concrete entity type
 * @author Xander Wang
 * @since 0.2.0
 */
@MappedSuperclass
public abstract class TreeEntity<T extends TreeEntity<T>> extends BaseEntity {

    /**
     * Identifier of the parent node, or {@code null} for a root node.
     */
    @Column(name = "parent_id")
    private UUID parentId;

    /**
     * Position of this node among its siblings, lower values first.
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    @Transient
    private List<T> children = new ArrayList<>();

    public UUID getParentId() {
        return this.parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<T> getChildren() {
        return this.children;
    }

    public void setChildren(List<T> children) {
        this.children = children;
    }

}
