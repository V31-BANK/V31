/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.v31bank.data.jpa.util;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.v31bank.data.jpa.domain.TreeEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Trees}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class TreesTests {

	@Test
	void hangsEachNodeUnderItsParent() {
		Category root = category("root", null, 1);
		Category child = category("child", root.getId(), 1);
		Category grandchild = category("grandchild", child.getId(), 1);

		List<Category> roots = Trees.build(List.of(grandchild, child, root));

		assertThat(roots).containsExactly(root);
		assertThat(root.getChildren()).containsExactly(child);
		assertThat(child.getChildren()).containsExactly(grandchild);
		assertThat(grandchild.getChildren()).isEmpty();
	}

	@Test
	void ordersSiblingsByTheirPosition() {
		Category root = category("root", null, 1);
		Category third = category("third", root.getId(), 30);
		Category first = category("first", root.getId(), 10);
		Category second = category("second", root.getId(), 20);

		Trees.build(List.of(third, first, second, root));

		assertThat(root.getChildren()).containsExactly(first, second, third);
	}

	/**
	 * A node with no position given goes after the ones that have one, rather than
	 * sorting as if it were at the top.
	 */
	@Test
	void putsAnUnpositionedNodeLast() {
		Category root = category("root", null, 1);
		Category unpositioned = category("unpositioned", root.getId(), null);
		Category positioned = category("positioned", root.getId(), 50);

		Trees.build(List.of(unpositioned, positioned, root));

		assertThat(root.getChildren()).containsExactly(positioned, unpositioned);
	}

	/**
	 * Two siblings can share a position — the categories in the platform are ordered by a
	 * hand-entered number. Falling back to the identifier keeps the order the same from
	 * one request to the next, which is what a paged listing needs.
	 */
	@Test
	void ordersEquallyPositionedSiblingsTheSameWayEveryTime() {
		Category root = category("root", null, 1);
		Category a = category("a", root.getId(), 10);
		Category b = category("b", root.getId(), 10);
		Category earlier = (a.getId().compareTo(b.getId()) < 0) ? a : b;

		Trees.build(List.of(b, a, root));

		assertThat(root.getChildren()).first().isSameAs(earlier);
	}

	/**
	 * A page of a table is a slice, so a node whose parent is on another page has nothing
	 * to hang under. Dropping it would lose a record from a listing.
	 */
	@Test
	void treatsANodeWhoseParentIsAbsentAsARoot() {
		Category orphan = category("orphan", UUID.randomUUID(), 1);

		assertThat(Trees.build(List.of(orphan))).containsExactly(orphan);
	}

	@Test
	void treatsANodeThatIsItsOwnParentAsARoot() {
		Category self = category("self", null, 1);
		self.setParentId(self.getId());

		List<Category> roots = Trees.build(List.of(self));

		assertThat(roots).containsExactly(self);
		assertThat(self.getChildren()).isEmpty();
	}

	/**
	 * Building twice from the same objects has to give the same answer: the entities come
	 * from a persistence context that hands back the identical instance.
	 */
	@Test
	void doesNotAccumulateChildrenWhenBuiltAgain() {
		Category root = category("root", null, 1);
		Category child = category("child", root.getId(), 1);

		Trees.build(List.of(child, root));
		Trees.build(List.of(child, root));

		assertThat(root.getChildren()).containsExactly(child);
	}

	@Test
	void returnsNothingForNothing() {
		assertThat(Trees.build(List.<Category>of())).isEmpty();
	}

	private static Category category(String name, UUID parentId, Integer sortOrder) {
		Category category = new Category(name);
		category.setId(UUID.randomUUID());
		category.setParentId(parentId);
		category.setSortOrder(sortOrder);
		return category;
	}

	/**
	 * Stands in for a tree-shaped entity such as a customer category.
	 */
	static class Category extends TreeEntity<Category> {

		private final String name;

		Category(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

}
