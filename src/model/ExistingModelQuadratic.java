package model;

import util.Problem;

public class ExistingModelQuadratic extends ExistingModel {
    public ExistingModelQuadratic(Problem prob, int numThreads, double timeLimit, boolean feasibilityCheck) {
        super(prob, false, false, numThreads, timeLimit, feasibilityCheck);
    }
}
