package run;

import ilog.concert.IloException;
import model.*;
import util.Problem;
import util.Solution;

import java.io.IOException;

public class Solve {
    public static void main(String[] args) throws IOException, IloException {
        String[] names = {"tiny", "small", "medium", "large"};

        for (int scale = 0; scale <= 3; scale++) {
            for (int num = 1; num <= 10; num++) {
                String file = String.format("data/%s-%d.txt", names[scale], num);
                Problem prob = Problem.readInstance(file);
                System.out.printf("\n%s: %s\n", file, prob);

                boolean piecewise = true;
                int numThreads = 0;
                double timeLimit = 3600;
                boolean feasibilityCheck = true;

                Solution[] ends = new ImprovedModel(prob, piecewise, numThreads, timeLimit, feasibilityCheck).front(1);
                System.out.println("initial: " + ends[1]);
                System.out.println("perfect: " + ends[0]);

                Solution s5 = new ImprovedModel(prob, piecewise, numThreads, timeLimit, feasibilityCheck).solve();
                System.out.printf("Improved model: %s\n", s5);

                Solution s4 = new BasicModelTight(prob, piecewise, numThreads, timeLimit, feasibilityCheck).solve();
                System.out.printf("Basic model (tight): %s\n", s4);
                Solution s3 = new BasicModelLoose(prob, piecewise, numThreads, timeLimit, feasibilityCheck).solve();
                System.out.printf("Basic model (loose): %s\n", s3);

                Solution s2 = new ExistingModelLinear(prob, piecewise, numThreads, timeLimit, feasibilityCheck).solve();
                System.out.printf("Existing model (linear): %s\n", s2);
                Solution s1 = new ExistingModelQuadratic(prob, numThreads, timeLimit, feasibilityCheck).solve();
                System.out.printf("Existing model (quadratic): %s\n", s1);
            }
        }
    }
}
