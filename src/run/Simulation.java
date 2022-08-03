package run;

import util.Problem;

import java.io.IOException;

public class Simulation {
    public static void main(String[] args) throws IOException {
        String file = "data/tiny-1.txt";
        Problem prob = Problem.readInstance(file);
        for (int x = 0; x <= prob.gamma; x++) {
            System.out.printf("x = %d, Piecewise = %f, Simulation = %f\n", x, prob.computeR(x, true), prob.simulateR(x, 1000000));
        }
    }
}
