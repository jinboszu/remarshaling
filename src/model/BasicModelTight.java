package model;

import util.Problem;

public class BasicModelTight extends BasicModel {
    public BasicModelTight(Problem prob, boolean piecewise, int numThreads, double timeLimit, boolean feasibilityCheck) {
        super(prob, piecewise, true, numThreads, timeLimit, feasibilityCheck);
    }
}
