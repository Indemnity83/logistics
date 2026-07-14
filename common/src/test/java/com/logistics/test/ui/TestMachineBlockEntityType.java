package com.logistics.test.ui;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class TestMachineBlockEntityType {
    public static final BlockEntityType<TestMachineBlockEntity> TEST_MACHINE = 
        BlockEntityType.Builder.create(TestMachineBlockEntity::new, 
        com.logistics.test.ui.TestMachineBlock.class).build(null);
}
