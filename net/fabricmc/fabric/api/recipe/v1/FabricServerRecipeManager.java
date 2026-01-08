/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.api.recipe.v1;

import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.class_1860;
import net.minecraft.class_1863;
import net.minecraft.class_1937;
import net.minecraft.class_3956;
import net.minecraft.class_8786;
import net.minecraft.class_9695;

/**
 * General-purpose Fabric-provided extensions for {@link class_1863} class.
 */
public interface FabricServerRecipeManager {
	/**
	 * Creates a stream of all recipe entries of the given {@code type} that match the
	 * given {@code input} and {@code world}.
	 *
	 * <p>If {@code input.isEmpty()} returns true, the returned stream will be always empty.
	 *
	 * @return the stream of matching recipes
	 */
	default <I extends class_9695, T extends class_1860<I>> Stream<class_8786<T>> getAllMatches(class_3956<T> type, I input, class_1937 world) {
		throw new AssertionError("Implemented in Mixin");
	}

	/**
	 * @return the collection of recipe entries of given type
	 */
	default <I extends class_9695, T extends class_1860<I>> Collection<class_8786<T>> getAllOfType(class_3956<T> type) {
		throw new AssertionError("Implemented in Mixin");
	}
}
