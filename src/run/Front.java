package run;

import ilog.concert.IloException;
import model.ImprovedModel;
import util.Problem;
import util.Solution;
import util.Util;

import java.io.FileWriter;
import java.io.IOException;

public class Front {
    public static void main(String[] args) throws IOException, IloException {
        String[] names = {"tiny", "small", "medium", "large"};

        for (int scale = 0; scale < 4; scale++) {
            for (int num = 1; num <= 10; num++) {
                String file = String.format("data/%s-%d.txt", names[scale], num);
                Problem prob = Problem.readInstance(file);
                System.out.printf("%s: %s\n", file, prob);

                boolean piecewise = true;
                int numThreads = 0;
                double timeLimit = 3600;
                boolean feasibilityCheck = true;

                Solution[] front = new ImprovedModel(prob, piecewise, numThreads, timeLimit, feasibilityCheck).front(1000);
                try (FileWriter writer = new FileWriter(String.format("exp-front/%s-%d-front.txt", names[scale], num))) {
                    for (Solution sol : front) {
                        Util.gson.toJson(sol, writer);
                        writer.write("\n");
                    }
                }
            }
        }
    }
}
