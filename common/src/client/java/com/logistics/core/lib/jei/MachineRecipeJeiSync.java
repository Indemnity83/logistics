package com.logistics.core.lib.jei;

import mezz.jei.api.runtime.IJeiRuntime;

/** Forwards late-synchronized recipes through the active JEI runtime. */
public final class MachineRecipeJeiSync {

    private static volatile IJeiRuntime runtime;

    private MachineRecipeJeiSync() {}

    public static void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void onRuntimeUnavailable() {
        runtime = null;
    }

    public static void pushToJei(JeiRecipeSyncAdapter adapter) {
        IJeiRuntime current = runtime;
        if (current != null) {
            adapter.pushToJei(current.getRecipeManager());
        }
    }

    public static void hideFromJei(JeiRecipeSyncAdapter adapter) {
        IJeiRuntime current = runtime;
        if (current != null) {
            adapter.hideFromJei(current.getRecipeManager());
        }
    }
}
