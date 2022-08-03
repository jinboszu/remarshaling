package run;

import ilog.concert.IloException;
import model.ImprovedModel;
import util.Problem;
import util.Solution;
import util.Util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class ImpactInitial {
    public static void main(String[] args) throws IOException, IloException {
        String file = "data/large-1.txt";
        Problem prob = Problem.readInstance(file);

        Random rnd = new Random(0);
        Problem[] problems = new Problem[3];
        problems[0] = prob;
        problems[1] = new Problem(prob.m, prob.s, prob.gamma, prob.theta, Data.seaside(prob.m, prob.gamma, prob.theta, rnd), prob.tC, prob.tE, prob.tL, prob.tau);
        problems[2] = new Problem(prob.m, prob.s, prob.gamma, prob.theta, Data.landside(prob.m, prob.gamma, prob.theta, rnd), prob.tC, prob.tE, prob.tL, prob.tau);

        boolean piecewise = true;
        int numThreads = 0;
        double timeLimit = 3600;
        boolean feasibilityCheck = true;

        String[] names = new String[]{"uniform", "seaside", "landside"};

        for (int i = 0; i < 3; i++) {
            Solution[] front = new ImprovedModel(problems[i], piecewise, numThreads, timeLimit, feasibilityCheck).front(1000);
            try (FileWriter writer = new FileWriter(String.format("exp-front/%s-front.txt", names[i]))) {
                for (Solution sol : front) {
                    Util.gson.toJson(sol, writer);
                    writer.write("\n");
                }
            }
        }
    }
}
