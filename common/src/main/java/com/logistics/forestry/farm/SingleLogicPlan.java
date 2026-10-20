package com.logistics.forestry.farm;

import com.logistics.core.machine.area.PlotLayout;
import com.logistics.core.machine.area.RingArea;

/** One logic over the whole ring: the ring is its only region. */
public record SingleLogicPlan(FarmLogic logic, RingArea ring) implements FarmPlan {

    @Override
    public FarmLogic logicFor(int plotIndex) {
        return logic;
    }

    @Override
    public PlotLayout regionFor(int plotIndex) {
        return ring;
    }
}
