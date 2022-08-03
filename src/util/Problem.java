package util;

import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class Problem {
    public final int m; // number of bays
    public final int s; // number of stacks in each bay
    public final int gamma; // capacity of each bay
    public final int theta; // total number of containers
    public final int[] h; // initial number of containers in each bay
    public final int tC; // time for lifting or dropping a container
    public final int tE; // time for an empty drive between two adjacent bays
    public final int tL; // time for a loaded drive between two adjacent bays
    public final int tau; // remarshaling time limit

    public Problem(int m, int s, int gamma, int theta, int[] h, int tC, int tE, int tL, int tau) {
        this.m = m;
        this.s = s;
        this.gamma = gamma;
        this.theta = theta;
        this.h = h;
        this.tC = tC;
        this.tE = tE;
        this.tL = tL;
        this.tau = tau;
    }

    public Problem(Problem prob) {
        this(prob.m, prob.s, prob.gamma, prob.theta, prob.h, prob.tC, prob.tE, prob.tL, prob.tau);
    }

    public static Problem readInstance(String file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return Util.gson.fromJson(reader, Problem.class);
        }
    }

    public double computeR1(int x) {
        return 0.5 * (s + 2) * (x - s) / (s + 1);
    }

    public double computeR2(int x) {
        return (0.25 / s + 0.0625 / s / s) * x * x + (0.125 / s - 0.25) * x;
    }

    public double computeR(int x, boolean piecewise) {
        if (piecewise) {
            return x <= s ? 0 : x < 2 * s ? computeR1(x) : computeR2(x);
        } else {
            return computeR2(x);
        }
    }

    public int simulateR(int x, int[] h, Random rnd) {
        int r = 0;
        while (x > 0) {
            int next = rnd.nextInt(x);
            int stack = 0;
            while (next >= h[stack]) {
                next -= h[stack];
                stack++;
            }
            h[stack] -= next + 1;
            r += next;
            x--;

            int i = s;
            while (next > 0) {
                if (i == s) {
                    while (i == s || (i - 1 != stack ? i - 1 : i - 2) >= 0 && h[i] == h[i - 1 != stack ? i - 1 : i - 2]) {
                        i = i - 1 != stack ? i - 1 : i - 2;
                    }
                }
                h[i]++;
                next--;
                i = i + 1 != stack ? i + 1 : i + 2;
            }

            while (stack > 0 && h[stack] > h[stack - 1]) {
                int temp = h[stack];
                h[stack] = h[stack - 1];
                h[stack - 1] = temp;
                stack--;
            }
            while (stack < s - 1 && h[stack] < h[stack + 1]) {
                int temp = h[stack];
                h[stack] = h[stack + 1];
                h[stack + 1] = temp;
                stack++;
            }
        }
        return r;
    }

    public double simulateR(int x, int sample) {
        if (x <= s) {
            return 0.0;
        }

        int[] h = new int[s];
        for (int i = 0; i < s; i++) {
            h[i] = x / s + (i < x % s ? 1 : 0);
        }
        Random rnd = new Random(0);
        int sum = 0;
        for (int k = 0; k < sample; k++) {
            sum += simulateR(x, h.clone(), rnd);
        }
        return 1.0 * sum / sample;
    }

    public double computeF(int[] alloc, boolean piecewise) {
        double f = 2 * theta * tC;
        for (int i = 0; i < m; i++) {
            f += 2 * tC * computeR(alloc[i], piecewise);
            f += (tE + tL) * (i + 1) * alloc[i];
        }
        return f;
    }

    public int[][] roundTrip(int[] alloc) {
        int n = 0;
        for (int i = 0; i < m; i++) {
            if (alloc[i] - h[i] > 0) {
                n += alloc[i] - h[i];
            }
        }
        int[][] match = new int[n][2];
        for (int i = 0, o = 0, d = 0; i < m; i++) {
            if (alloc[i] - h[i] < 0) {
                for (int l = 0; l < h[i] - alloc[i]; l++) {
                    match[o++][0] = i + 1;
                }
            }
            if (alloc[i] - h[i] > 0) {
                for (int l = 0; l < alloc[i] - h[i]; l++) {
                    match[d++][1] = i + 1;
                }
            }
        }
        int[][] moves = new int[n][2];
        for (int j = 0, f = 0, b = n - 1; j < n; j++) {
            if (match[j][0] < match[j][1]) {
                moves[f][0] = match[j][0];
                moves[f][1] = match[j][1];
                f++;
            } else {
                moves[b][0] = match[j][0];
                moves[b][1] = match[j][1];
                b--;
            }
        }
        return moves;
    }

    public int computeG(int[][] moves) {
        int g = 2 * tC * moves.length;
        for (int j = 0; j < moves.length; j++) {
            if (j == 0) {
                g += tE * moves[j][0];
            } else {
                g += tE * Math.abs(moves[j - 1][1] - moves[j][0]);
            }
            g += tL * Math.abs(moves[j][0] - moves[j][1]);
            if (j == moves.length - 1) {
                g += tE * moves[j][1];
            }
        }
        return g;
    }

    public int[] toAlloc(int[][] moves) {
        int[] alloc = h.clone();
        for (int[] move : moves) {
            alloc[move[0] - 1]--;
            alloc[move[1] - 1]++;
        }
        return alloc;
    }

    @Override
    public String toString() {
        return Util.gson.toJson(this);
    }
}
