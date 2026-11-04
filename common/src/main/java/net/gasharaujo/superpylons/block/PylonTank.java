package net.gasharaujo.superpylons.block;

import java.util.function.BiPredicate;

/** Stores the fluid identity separately from its amount so capacity is not limited by an API stack. */
final class PylonTank<T> {
    record State<T>(T resource, long amount) {}

    private State<T> state = new State<>(null, 0);

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
        int extracted = (int) Math.min(maximum, amount());
        if (!simulate && extracted > 0) restore(new State<>(resource(), amount() - extracted));
        return extracted;
    }

    long moveTo(PylonTank<T> target, long maximum, long capacity, BiPredicate<T, T> matches) {
        if (amount() == 0 || target.amount() >= capacity ||
            (target.amount() > 0 && !matches.test(resource(), target.resource()))) return 0;
        long moved = Math.min(maximum, Math.min(amount(), capacity - target.amount()));
        if (moved <= 0) return 0;
        target.state = new State<>(resource(), target.amount() + moved);
        restore(new State<>(resource(), amount() - moved));
        return moved;
    }

    void clear() { restore(new State<>(null, 0)); }
}
