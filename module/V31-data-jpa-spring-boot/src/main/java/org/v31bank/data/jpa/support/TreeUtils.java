package org.v31bank.data.jpa.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.v31bank.data.jpa.domain.BaseEntity;
import org.v31bank.data.jpa.domain.TreeEntity;

/**
 * Utility for assembling flat lists of {@link TreeEntity} nodes into trees.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class TreeUtils {

    @SuppressWarnings("java:S1612")
    private static final Comparator<TreeEntity<?>> SIBLING_ORDER = Comparator
        .comparing((TreeEntity<?> node) -> node.getSortOrder(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(BaseEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private TreeUtils() {
    }

    /**
     * Assemble the given flat collection of nodes into a tree by linking each node
     * to its parent's {@code children} collection. A node is a root if its parent
     * identifier is {@code null} or refers to a node not present in the collection.
     * Roots and children are ordered by {@code sortOrder} (nulls last), then id.
     * @param nodes the flat collection of nodes
     * @param <T> the node type
     * @return the root nodes, with descendants attached
     */
    public static <T extends TreeEntity<T>> List<T> build(Collection<T> nodes) {
        Map<UUID, T> byId = new HashMap<>();
        for (T node : nodes) {
            node.setChildren(new ArrayList<>());
            if (node.getId() != null) {
                byId.put(node.getId(), node);
            }
        }
        List<T> roots = new ArrayList<>();
        for (T node : nodes) {
            T parent = (node.getParentId() != null) ? byId.get(node.getParentId()) : null;
            if (parent != null && parent != node) {
                parent.getChildren().add(node);
            }
            else {
                roots.add(node);
            }
        }
        roots.sort(SIBLING_ORDER);
        for (T node : byId.values()) {
            node.getChildren().sort(SIBLING_ORDER);
        }
        return roots;
    }

}
