package util;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

import java.util.Arrays;

public abstract class Solver extends Problem {
    public final boolean piecewise;
    public final int numThreads;
    public final double timeLimit;
    public final boolean feasibilityCheck;

    public Solver(Problem prob, boolean piecewise, int numThreads, double timeLimit, boolean feasibilityCheck) {
        super(prob);
        this.piecewise = piecewise;
        this.numThreads = numThreads;
        this.timeLimit = timeLimit;
        this.feasibilityCheck = feasibilityCheck;
    }

    public abstract Formulation build() throws IloException;

    public Solution solve() throws IloException {
        try (Formulation model = build()) {
            return model.solve();
        }
    }

    public Solution check(Solution sol) {
        if (feasibilityCheck && !(Arrays.equals(sol.alloc, toAlloc(sol.moves)) &&
                Math.abs(sol.f - computeF(sol.alloc, piecewise)) <= 1e-6 &&
                sol.g == computeG(sol.moves))) {
            System.err.println(sol);
            System.err.println(Arrays.toString(sol.alloc) + " ? " + Arrays.toString(toAlloc(sol.moves)));
            System.err.println(sol.f + " ? " + computeF(sol.alloc, piecewise));
            System.err.println(sol.g + " ? " + computeG(sol.moves));
            throw new RuntimeException();
        }
        return sol;
    }

    public abstract class Formulation implements AutoCloseable {
        public IloCplex cplex;
        public IloNumExpr f;
        public IloNumExpr g;

        public Formulation() throws IloException {
            cplex = new IloCplex();
            cplex.setOut(null);
            cplex.setWarning(null);
            cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
            cplex.setParam(IloCplex.Param.MIP.Tolerances.Integrality, 0);
            cplex.setParam(IloCplex.Param.Threads, numThreads);
            cplex.setParam(IloCplex.Param.TimeLimit, timeLimit);
        }

        @Override
        public void close() {
            cplex.end();
        }

        public abstract Solution result(double time) throws IloException;

        public Solution solve() throws IloException {
            double startTime = cplex.getCplexTime();
            cplex.addMinimize(f);
            cplex.addLe(g, tau);
            if (cplex.solve()) {
                return check(result(cplex.getCplexTime() - startTime));
            } else {
                return null;
            }
        }
    }
}
