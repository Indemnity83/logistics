package com.logistics.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Reading shipped recipe JSON in assertions, independent of how vanilla spells it on this branch. */
public final class RecipeJson {

    private RecipeJson() {}

    /**
     * An ingredient in its canonical {@code #tag} / {@code item} spelling.
     *
     * <p>Vanilla accepts a bare string for an {@code Ingredient} on 26.x but demands an object
     * ({@code {"item": …}} / {@code {"tag": …}}) on 1.21.1, so the shipped JSON differs by branch
     * while the recipe does not. Normalising here keeps both the assertions and the test files
     * themselves identical across branches — a spot-check is asserting what a recipe <em>means</em>,
     * not how vanilla happens to serialise it.
     */
    public static String ingredient(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        JsonObject object = element.getAsJsonObject();
        return object.has("tag") ? "#" + object.get("tag").getAsString() : object.get("item").getAsString();
    }
}
