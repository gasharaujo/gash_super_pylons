package net.gasharaujo.superpylons.block;

import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** Stores the fluid identity separately from its amount so capacity is not limited by an API stack. */
final class PylonTank<T> {
    record State<T>(T resource, long amount) {}

    private State<T> state = new State<>(null, 0);
    private final BooleanSupplier creative;
    private final LongSupplier capacity;

    PylonTank() { this(() -> false, () -> 0); }
    PylonTank(BooleanSupplier creative, LongSupplier capacity) {
        this.creative = creative;
        this.capacity = capacity;
    }

    long available() { return amount() > 0 && creative.getAsBoolean() ? Math.max(amount(), capacity.getAsLong()) : amount(); }

    T resource() { return state.resource(); }
    long amount() { return state.amount(); }
    State<T> snapshot() { return state; }

    void restore(State<T> state) {
        this.state = state.resource() == null || state.amount() <= 0 ? new State<>(null, 0) : state;
    }

    int insert(T resource, int maximum, long capacity, BiPredicate<T, T> matches, boolean simulate) {
        if (resource == null || maximum <= 0 ||
            (amount() > 0 && !matches.test(resource(), resource))) return 0;
        int inserted = (int) Math.min(maximum, Math.max(0, capacity - amount()));
        if (!simulate && inserted > 0) state = new State<>(resource, amount() + inserted);
        return inserted;
    }

    int extract(int maximum, boolean simulate) {
        if (maximum <= 0) return 0;
        int extracted = (int) Math.min(maximum, available());
        if (!simulate && extracted > 0 && !creative.getAsBoolean()) restore(new State<>(resource(), amount() - extracted));
        return extracted;
    }

    long moveTo(PylonTank<T> target, long maximum, long capacity, BiPredicate<T, T> matches) {
        if (amount() == 0 || target.amount() >= capacity ||
            (target.amount() > 0 && !matches.test(resource(), target.resource()))) return 0;
        long moved = Math.min(maximum, Math.min(available(), capacity - target.amount()));
        if (moved <= 0) return 0;
        target.state = new State<>(resource(), target.amount() + moved);
        if (!creative.getAsBoolean()) restore(new State<>(resource(), amount() - moved));
        return moved;
    }

    long discard(long maximum) {
        long removed = Math.min(maximum, available());
        if (!creative.getAsBoolean()) restore(new State<>(resource(), amount() - removed));
        return removed;
    }

    void clear() { restore(new State<>(null, 0)); }
}
