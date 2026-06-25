package com.logistics.core.machine.upgrade;

/**
 * Upgrade-driven modifiers applied to common machine behaviors, keyed by {@link OperationKey}. A
 * machine reads cost/speed/range per operation at the use site, and capacity/receive-rate transforms
 * at component-build time, so upgrades can affect shared behaviors without bespoke code in each
 * machine. The {@link #identity()} instance leaves every behavior unchanged.
 */
public interface MachineModifiers {

    /** Multiplier on the energy cost of an operation (1.0 = unchanged). */
    double cost(OperationKey op);

    /** Multiplier on the work speed of an operation (1.0 = unchanged). */
    double speed(OperationKey op);

    /** Additive range bonus for an operation (0 = unchanged). */
    int range(OperationKey op);

    /** Transform applied to a base energy capacity (identity returns the base). */
    long capacity(long base);

    /** Transform applied to a base energy receive rate (identity returns the base). */
    long receiveRate(long base);

    MachineModifiers IDENTITY = new MachineModifiers() {
        @Override
        public double cost(OperationKey op) {
            return 1.0;
        }

        @Override
        public double speed(OperationKey op) {
            return 1.0;
        }

        @Override
        public int range(OperationKey op) {
            return 0;
        }

        @Override
        public long capacity(long base) {
            return base;
        }

        @Override
        public long receiveRate(long base) {
            return base;
        }
    };

    /** Modifiers that leave every behavior unchanged. */
    static MachineModifiers identity() {
        return IDENTITY;
    }
}
