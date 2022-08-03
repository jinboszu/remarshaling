package model;

import util.Problem;

public class BasicModelLoose extends BasicModel {
    public BasicModelLoose(Problem prob, boolean piecewise, int numThreads, double timeLimit, boolean feasibilityCheck) {
        super(prob, piecewise, false, numThreads, timeLimit, feasibilityCheck);
    }
}
