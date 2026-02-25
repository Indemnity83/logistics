package com.logistics.core.lib.resource;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResourceId")
class ResourceIdTest extends MinecraftTestEnvironment {

    // ==================== Creation Tests ====================

    @Test
    @DisplayName("should create identifier in default namespace")
    void createWithDefaultNamespace() {
        ResourceId id = ResourceId.of("pipe/wood");

        assertThat(id.getNamespace()).isEqualTo("minecraft");
        assertThat(id.getPath()).isEqualTo("pipe/wood");
        assertThat(id.toString()).isEqualTo("minecraft:pipe/wood");
    }

    @Test
    @DisplayName("should create identifier with explicit namespace")
    void createWithExplicitNamespace() {
        ResourceId id = ResourceId.in("logistics", "pipe/wood");

        assertThat(id.getNamespace()).isEqualTo("logistics");
        assertThat(id.getPath()).isEqualTo("pipe/wood");
        assertThat(id.toString()).isEqualTo("logistics:pipe/wood");
    }

    // ==================== Parsing Tests ====================

    @Test
    @DisplayName("should parse namespace:path format")
    void parseNamespacedFormat() {
        ResourceId id = ResourceId.parse("logistics:pipe/wood");

        assertThat(id.getNamespace()).isEqualTo("logistics");
        assertThat(id.getPath()).isEqualTo("pipe/wood");
    }

    @Test
    @DisplayName("should parse path-only format with default namespace")
    void parsePathOnlyFormat() {
        ResourceId id = ResourceId.parse("pipe/wood");

        assertThat(id.getNamespace()).isEqualTo("minecraft");
        assertThat(id.getPath()).isEqualTo("pipe/wood");
    }

    @Test
    @DisplayName("should throw exception for invalid identifier in parse")
    void parseInvalidThrows() {
        assertThatThrownBy(() -> ResourceId.parse("invalid::id"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("should return null for invalid identifier in tryParse")
    void tryParseInvalidReturnsNull() {
        ResourceId id = ResourceId.tryParse("invalid::id");

        assertThat(id).isNull();
    }

    @Test
    @DisplayName("should parse valid identifier in tryParse")
    void tryParseValidReturnsId() {
        ResourceId id = ResourceId.tryParse("logistics:pipe/wood");

        assertThat(id).isNotNull();
        assertThat(id.getNamespace()).isEqualTo("logistics");
        assertThat(id.getPath()).isEqualTo("pipe/wood");
    }

    // ==================== Wrapping Tests ====================

    @Test
    @DisplayName("should wrap existing Identifier")
    void wrapIdentifier() {
        Identifier mc = Identifier.fromNamespaceAndPath("logistics", "pipe/wood");
        ResourceId id = ResourceId.wrap(mc);

        assertThat(id.getNamespace()).isEqualTo("logistics");
        assertThat(id.getPath()).isEqualTo("pipe/wood");
    }

    @Test
    @DisplayName("should throw exception when wrapping null")
    void wrapNullThrows() {
        assertThatThrownBy(() -> ResourceId.wrap(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("should unwrap to Identifier")
    void unwrapToIdentifier() {
        ResourceId id = ResourceId.in("logistics", "pipe/wood");
        Identifier mc = id.toIdentifier();

        assertThat(mc.getNamespace()).isEqualTo("logistics");
        assertThat(mc.getPath()).isEqualTo("pipe/wood");
    }

    // ==================== Equality Tests ====================

    @Test
    @DisplayName("should be equal when namespace and path match")
    void equalityWithSameValues() {
        ResourceId id1 = ResourceId.in("logistics", "pipe/wood");
        ResourceId id2 = ResourceId.in("logistics", "pipe/wood");

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when namespace differs")
    void inequalityWithDifferentNamespace() {
        ResourceId id1 = ResourceId.in("logistics", "pipe/wood");
        ResourceId id2 = ResourceId.in("minecraft", "pipe/wood");

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("should not be equal when path differs")
    void inequalityWithDifferentPath() {
        ResourceId id1 = ResourceId.in("logistics", "pipe/wood");
        ResourceId id2 = ResourceId.in("logistics", "pipe/stone");

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("should handle reflexive equality")
    void reflexiveEquality() {
        ResourceId id = ResourceId.in("logistics", "pipe/wood");

        assertThat(id).isEqualTo(id);
    }

    @Test
    @DisplayName("should not be equal to null")
    void notEqualToNull() {
        ResourceId id = ResourceId.in("logistics", "pipe/wood");

        assertThat(id).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not be equal to different type")
    void notEqualToDifferentType() {
        ResourceId id = ResourceId.in("logistics", "pipe/wood");

        assertThat(id).isNotEqualTo("logistics:pipe/wood");
    }

    // ==================== String Representation Tests ====================

    @Test
    @DisplayName("should produce correct string representation")
    void toStringFormat() {
        ResourceId id = ResourceId.in("logistics", "pipe/wood");

        assertThat(id.toString()).isEqualTo("logistics:pipe/wood");
    }

    @Test
    @DisplayName("should roundtrip through string parsing")
    void stringRoundtrip() {
        ResourceId original = ResourceId.in("logistics", "pipe/wood");
        String str = original.toString();
        ResourceId parsed = ResourceId.parse(str);

        assertThat(parsed).isEqualTo(original);
    }
}
