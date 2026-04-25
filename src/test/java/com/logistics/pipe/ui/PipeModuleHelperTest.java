package com.logistics.pipe.ui;

import com.logistics.core.lib.pipe.Module;
import com.logistics.pipe.modules.PassiveSupplierModule;
import com.logistics.pipe.modules.SupplierModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PipeModuleHelper")
class PipeModuleHelperTest {

    @Test
    @DisplayName("findModule resolves supplier subclasses by exact state key")
    void findModule_resolvesSupplierSubclassesByExactStateKey() {
        PassiveSupplierModule passive = new PassiveSupplierModule(8);
        SupplierModule active = new SupplierModule();
        List<Module> modules = List.of(passive, active);

        assertThat(PipeModuleHelper.findModule(modules, SupplierModule.class, active.getStateKey()))
                .isSameAs(active);
        assertThat(PipeModuleHelper.findModule(modules, SupplierModule.class, passive.getStateKey()))
                .isSameAs(passive);
    }

    @Test
    @DisplayName("findModule returns null when no module has the requested state key")
    void findModule_returnsNullForMissingStateKey() {
        List<Module> modules = List.of(new PassiveSupplierModule(8), new SupplierModule());

        assertThat(PipeModuleHelper.findModule(modules, SupplierModule.class, "missingmodule"))
                .isNull();
    }
}
