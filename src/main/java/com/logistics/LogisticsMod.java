package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.bootstrap.DomainBootstraps;
import com.logistics.core.util.TimingLog;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogisticsMod implements ModInitializer {
    public static final String MOD_ID = "logistics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Logistics mod");

        for (DomainBootstrap bootstrap : DomainBootstraps.all()) {
            String name = bootstrap.getClass().getSimpleName();
            TimingLog.time(LOGGER, name + ".initCommon", bootstrap::initCommon);
        }
    }
}
