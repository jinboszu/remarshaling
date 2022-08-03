package run;

import ilog.concert.IloException;
import model.ImprovedModel;
import util.Problem;
import util.Solution;
import util.Util;

import java.io.FileWriter;
import java.io.IOException;

public class ImpactCrane {
    public static void main(String[] args) throws IOException, IloException {
        String file = "data/large-1.txt";
        Problem prob = Problem.readInstance(file);

        Problem[] problems = new Problem[9];
        for (int i = 0; i < 9; i++) {
            problems[i] = new Problem(prob.m, prob.s, prob.gamma, prob.theta, prob.h, 5 * (i + 2), prob.tE, prob.tL, prob.tau);
        }

        boolean piecewise = true;
        int numThreads = 0;
        double timeLimit = 3600;
        boolean feasibilityCheck = true;

        for (int i = 0; i < 9; i++) {
            Solution[] front = new ImprovedModel(problems[i], piecewise, numThreads, timeLimit, feasibilityCheck).front(1000);
            try (FileWriter writer = new FileWriter(String.format("exp-front/speed-%d-front.txt", problems[i].tC))) {
                for (Solution sol : front) {
                    Util.gson.toJson(sol, writer);
                    writer.write("\n");
                }
            }
        }
    }
}
