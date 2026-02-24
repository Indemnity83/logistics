package com.logistics.core.lib.resource;

import net.minecraft.resources.ResourceLocation;

/**
 * Stable wrapper around Minecraft's resource ResourceLocation types.
 * <p>
 * This class provides a version-agnostic interface for working with resource ResourceLocations
 * across different Minecraft versions:
 * <ul>
 *   <li>mc/1.21.1: wraps {@code net.minecraft.resources.ResourceLocation}</li>
 *   <li>mc/1.21.11 & mc/26.1: wraps {@code net.minecraft.resources.Identifier}</li>
 * </ul>
 * <p>
 * <b>Cherry-pick friendly:</b> When cherry-picking commits between branches, only this file's
 * import and internal type need to change. All usage sites remain unchanged.
 *
 * @see #of(String) Create ResourceLocation in default namespace
 * @see #in(String, String) Create ResourceLocation with explicit namespace
 * @see #parse(String) Parse from string (throws on invalid)
 * @see #tryParse(String) Parse from string (returns null on invalid)
 */
public final class ResourceId {
    private final ResourceLocation value;

    private ResourceId(ResourceLocation value) {
        if (value == null) {
            throw new IllegalArgumentException("ResourceLocation cannot be null");
        }
        this.value = value;
    }

    /**
     * Creates a resource ResourceLocation in the default namespace.
     * <p>
     * Example: {@code ResourceId.of("pipe/wood")} → {@code minecraft:pipe/wood}
     *
     * @param path the resource path
     * @return a new ResourceId wrapping the ResourceLocation
     * @throws IllegalArgumentException if path is invalid
     */
    public static ResourceId of(String path) {
        return new ResourceId(ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, path));
    }

    /**
     * Creates a resource ResourceLocation with explicit namespace and path.
     * <p>
     * Example: {@code ResourceId.in("logistics", "pipe/wood")} → {@code logistics:pipe/wood}
     *
     * @param namespace the namespace (mod ID)
     * @param path the resource path
     * @return a new ResourceId wrapping the ResourceLocation
     * @throws IllegalArgumentException if namespace or path is invalid
     */
    public static ResourceId in(String namespace, String path) {
        return new ResourceId(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /**
     * Parses a resource ResourceLocation from a string.
     * <p>
     * Accepts formats:
     * <ul>
     *   <li>{@code "namespace:path"} → {@code namespace:path}</li>
     *   <li>{@code "path"} → {@code minecraft:path}</li>
     * </ul>
     *
     * @param id the ResourceLocation string
     * @return a new ResourceId wrapping the parsed ResourceLocation
     * @throws IllegalArgumentException if the string is not a valid ResourceLocation
     */
    public static ResourceId parse(String id) {
        return new ResourceId(ResourceLocation.parse(id));
    }

    /**
     * Attempts to parse a resource ResourceLocation from a string, returning null if invalid.
     * <p>
     * Same formats as {@link #parse(String)}, but returns null instead of throwing.
     *
     * @param id the ResourceLocation string
     * @return a new ResourceId wrapping the parsed ResourceLocation, or null if invalid
     */
    public static ResourceId tryParse(String id) {
        try {
            return new ResourceId(ResourceLocation.parse(id));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unwraps the underlying Minecraft ResourceLocation for use with Minecraft APIs.
     *
     * @return the wrapped ResourceLocation
     */
    public ResourceLocation toIdentifier() {
        return value;
    }

    /**
     * Wraps an existing Minecraft ResourceLocation in a ResourceId.
     *
     * @param resourceLocation the Minecraft ResourceLocation to wrap
     * @return a new ResourceId wrapping the given ResourceLocation
     * @throws IllegalArgumentException if ResourceLocation is null
     */
    public static ResourceId wrap(ResourceLocation resourceLocation) {
        return new ResourceId(resourceLocation);
    }

    /**
     * Gets the namespace part of this ResourceLocation.
     * <p>
     * Example: {@code logistics:pipe/wood} → {@code "logistics"}
     *
     * @return the namespace string
     */
    public String getNamespace() {
        return value.getNamespace();
    }

    /**
     * Gets the path part of this ResourceLocation.
     * <p>
     * Example: {@code logistics:pipe/wood} → {@code "pipe/wood"}
     *
     * @return the path string
     */
    public String getPath() {
        return value.getPath();
    }

    /**
     * Returns the string representation of this ResourceLocation.
     * <p>
     * Format: {@code "namespace:path"}
     *
     * @return the ResourceLocation as a string
     */
    @Override
    public String toString() {
        return value.toString();
    }

    /**
     * Compares this ResourceId to another object for equality.
     * <p>
     * Two ResourceId instances are equal if they wrap identical ResourceLocations.
     *
     * @param obj the object to compare
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ResourceId that = (ResourceId) obj;
        return value.equals(that.value);
    }

    /**
     * Returns the hash code of the wrapped ResourceLocation.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
