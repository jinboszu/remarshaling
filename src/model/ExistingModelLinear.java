package model;

import util.Problem;

public class ExistingModelLinear extends ExistingModel {
    public ExistingModelLinear(Problem prob, boolean piecewise, int numThreads, double timeLimit, boolean feasibilityCheck) {
        super(prob, piecewise, true, numThreads, timeLimit, feasibilityCheck);
    }
}
