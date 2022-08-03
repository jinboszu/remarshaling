package util;

public class Solution {
    public final boolean optimal;
    public final double time;
    public final double f;
    public final int g;
    public final int[] alloc;
    public final int[][] moves;

    public Solution(boolean optimal, double time, double f, int g, int[] alloc, int[][] moves) {
        this.optimal = optimal;
        this.time = time;
        this.f = f;
        this.g = g;
        this.alloc = alloc;
        this.moves = moves;
    }

    @Override
    public String toString() {
        return Util.gson.toJson(this);
    }
}
