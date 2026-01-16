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

package net.fabricmc.fabric.impl.recipe.ingredient.builtin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.class_10302;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1856;
import net.minecraft.class_2960;
import net.minecraft.class_6880;
import net.minecraft.class_9129;
import net.minecraft.class_9139;
import net.minecraft.class_9326;
import net.minecraft.class_9331;

public class ComponentsIngredient implements CustomIngredient {
	public static final CustomIngredientSerializer<ComponentsIngredient> SERIALIZER = new Serializer();

	private final class_1856 base;
	private final class_9326 components;

	public ComponentsIngredient(class_1856 base, class_9326 components) {
		if (components.method_57848()) {
			throw new IllegalArgumentException("ComponentIngredient must have at least one defined component");
		}

		this.base = base;
		this.components = components;
	}

	@Override
	public boolean test(class_1799 stack) {
		if (!base.method_8093(stack)) return false;

		// None strict matching
		for (Map.Entry<class_9331<?>, Optional<?>> entry : components.method_57846()) {
			final class_9331<?> type = entry.getKey();
			final Optional<?> value = entry.getValue();

			if (value.isPresent()) {
				// Expect the stack to contain a matching component
				if (!stack.method_57826(type)) {
					return false;
				}

				if (!Objects.equals(value.get(), stack.method_58694(type))) {
					return false;
				}
			} else {
				// Expect the target stack to not contain this component
				if (stack.method_57826(type)) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public Stream<class_6880<class_1792>> getMatchingItems() {
		return base.method_8105();
	}

	@Override
	public class_10302 toDisplay() {
		return new class_10302.class_10304(
			base.method_8105().map(this::createEntryDisplay).toList()
		);
	}

	private class_10302 createEntryDisplay(class_6880<class_1792> entry) {
		class_1799 stack = entry.comp_349().method_7854();
		stack.method_59692(components);
		return new class_10302.class_10307(stack);
	}

	@Override
	public boolean requiresTesting() {
		return true;
	}

	@Override
	public CustomIngredientSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	private class_1856 getBase() {
		return base;
	}

	@Nullable
	private class_9326 getComponents() {
		return components;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ComponentsIngredient that = (ComponentsIngredient) o;
		return base.equals(that.base) && components.equals(that.components);
	}

	@Override
	public int hashCode() {
		return Objects.hash(base, components);
	}

	private static class Serializer implements CustomIngredientSerializer<ComponentsIngredient> {
		private static final class_2960 ID = class_2960.method_60655("fabric", "components");
		private static final MapCodec<ComponentsIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
				instance.group(
						class_1856.field_46095.fieldOf("base").forGetter(ComponentsIngredient::getBase),
						class_9326.field_49589.fieldOf("components").forGetter(ComponentsIngredient::getComponents)
				).apply(instance, ComponentsIngredient::new)
		);
		private static final class_9139<class_9129, ComponentsIngredient> PACKET_CODEC = class_9139.method_56435(
				class_1856.field_48355, ComponentsIngredient::getBase,
				class_9326.field_49590, ComponentsIngredient::getComponents,
				ComponentsIngredient::new
		);

		@Override
		public class_2960 getIdentifier() {
			return ID;
		}

		@Override
		public MapCodec<ComponentsIngredient> getCodec() {
			return CODEC;
		}

		@Override
		public class_9139<class_9129, ComponentsIngredient> getPacketCodec() {
			return PACKET_CODEC;
		}
	}
}
