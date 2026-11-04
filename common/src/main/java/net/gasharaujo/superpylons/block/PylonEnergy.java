package net.gasharaujo.superpylons.block;

final class PylonEnergy {
    record State(int amount, long tick, int received, int sent) {}

    private int amount;
    private long tick = Long.MIN_VALUE;
    private int received;
    private int sent;

    int amount() { return amount; }

    int limit(boolean input, int requested, int capacity, int rate, long now) {
        int used = rate != Integer.MAX_VALUE && now == tick ? (input ? received : sent) : 0;
        int available = input ? capacity - amount : amount;
        return Math.max(0, Math.min(requested, Math.min(available, rate - used)));
    }

    void transfer(boolean input, int moved, long now) {
        if (now != tick) {
            tick = now;
            received = 0;
            sent = 0;
        }
        if (input) {
            received += Math.min(moved, Integer.MAX_VALUE - received);
            amount += moved;
        } else {
            sent += Math.min(moved, Integer.MAX_VALUE - sent);
            amount -= moved;
        }
    }

    State snapshot() { return new State(amount, tick, received, sent); }

    void restore(State state) {
        amount = state.amount();
        tick = state.tick();
        received = state.received();
        sent = state.sent();
    }
}
