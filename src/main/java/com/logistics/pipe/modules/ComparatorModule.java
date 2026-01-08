package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;

public class ComparatorModule implements Module {
    @Override
    public int comparatorOutput(PipeContext ctx) {
        return ctx.blockEntity().getComparatorOutput();
    }

    @Override
    public boolean hasComparatorOutput() {
        return true;
    }
}
