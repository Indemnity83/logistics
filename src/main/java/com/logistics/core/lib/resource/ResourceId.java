package com.logistics.core.lib.resource;

import net.minecraft.resources.Identifier;

/**
 * Stable wrapper around Minecraft's resource identifier types.
 * <p>
 * This class provides a version-agnostic interface for working with resource identifiers
 * across different Minecraft versions:
 * <ul>
 *   <li>mc/1.21.1: wraps {@code net.minecraft.resources.ResourceLocation}</li>
 *   <li>mc/1.21.11 & mc/26.1: wraps {@code net.minecraft.resources.Identifier}</li>
 * </ul>
 * <p>
 * <b>Cherry-pick friendly:</b> When cherry-picking commits between branches, only this file's
 * import and internal type need to change. All usage sites remain unchanged.
 *
 * @see #of(String) Create identifier in default namespace
 * @see #in(String, String) Create identifier with explicit namespace
 * @see #parse(String) Parse from string (throws on invalid)
 * @see #tryParse(String) Parse from string (returns null on invalid)
 */
public final class ResourceId {
    private final Identifier value;

    private ResourceId(Identifier value) {
        if (value == null) {
            throw new IllegalArgumentException("Identifier cannot be null");
        }
        this.value = value;
    }

    /**
     * Creates a resource identifier in the default namespace.
     * <p>
     * Example: {@code ResourceId.of("pipe/wood")} → {@code minecraft:pipe/wood}
     *
     * @param path the resource path
     * @return a new ResourceId wrapping the identifier
     * @throws IllegalArgumentException if path is invalid
     */
    public static ResourceId of(String path) {
        return new ResourceId(Identifier.fromNamespaceAndPath(Identifier.DEFAULT_NAMESPACE, path));
    }

    /**
     * Creates a resource identifier with explicit namespace and path.
     * <p>
     * Example: {@code ResourceId.in("logistics", "pipe/wood")} → {@code logistics:pipe/wood}
     *
     * @param namespace the namespace (mod ID)
     * @param path the resource path
     * @return a new ResourceId wrapping the identifier
     * @throws IllegalArgumentException if namespace or path is invalid
     */
    public static ResourceId in(String namespace, String path) {
        return new ResourceId(Identifier.fromNamespaceAndPath(namespace, path));
    }

    /**
     * Parses a resource identifier from a string.
     * <p>
     * Accepts formats:
     * <ul>
     *   <li>{@code "namespace:path"} → {@code namespace:path}</li>
     *   <li>{@code "path"} → {@code minecraft:path}</li>
     * </ul>
     *
     * @param id the identifier string
     * @return a new ResourceId wrapping the parsed identifier
     * @throws IllegalArgumentException if the string is not a valid identifier
     */
    public static ResourceId parse(String id) {
        return new ResourceId(Identifier.parse(id));
    }

    /**
     * Attempts to parse a resource identifier from a string, returning null if invalid.
     * <p>
     * Same formats as {@link #parse(String)}, but returns null instead of throwing.
     *
     * @param id the identifier string
     * @return a new ResourceId wrapping the parsed identifier, or null if invalid
     */
    public static ResourceId tryParse(String id) {
        try {
            return new ResourceId(Identifier.parse(id));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unwraps the underlying Minecraft identifier for use with Minecraft APIs.
     * <p>
     * <b>Version-specific method name:</b>
     * <ul>
     *   <li>mc/1.21.11 & mc/26.1: {@code toIdentifier()}</li>
     *   <li>mc/1.21.1: {@code toResourceLocation()}</li>
     * </ul>
     *
     * @return the wrapped Identifier (or ResourceLocation in mc/1.21.1)
     */
    public Identifier toIdentifier() {
        return value;
    }

    /**
     * Gets the namespace part of this identifier.
     * <p>
     * Example: {@code logistics:pipe/wood} → {@code "logistics"}
     *
     * @return the namespace string
     */
    public String getNamespace() {
        return value.getNamespace();
    }

    /**
     * Gets the path part of this identifier.
     * <p>
     * Example: {@code logistics:pipe/wood} → {@code "pipe/wood"}
     *
     * @return the path string
     */
    public String getPath() {
        return value.getPath();
    }

    /**
     * Returns the string representation of this identifier.
     * <p>
     * Format: {@code "namespace:path"}
     *
     * @return the identifier as a string
     */
    @Override
    public String toString() {
        return value.toString();
    }

    /**
     * Compares this ResourceId to another object for equality.
     * <p>
     * Two ResourceId instances are equal if they wrap identical identifiers.
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
     * Returns the hash code of the wrapped identifier.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
