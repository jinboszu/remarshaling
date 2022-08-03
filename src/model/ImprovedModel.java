package model;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import util.Problem;
import util.Solution;
import util.Solver;
import util.Util;

import java.util.LinkedList;

public class ImprovedModel extends Solver {
    public ImprovedModel(Problem prob, boolean piecewise, int numThreads, double timeLimit, boolean feasibilityCheck) {
        super(prob, piecewise, numThreads, timeLimit, feasibilityCheck);
    }

    public Solution[] front(int K) throws IloException {
        try (Model model = build()) {
            return model.front(K);
        }
    }

    public Solution[] front() throws IloException {
        return front(Integer.MAX_VALUE);
    }

    @Override
    public Model build() throws IloException {
        return new Model();
    }

    public class Model extends Formulation {
        public IloNumVar[] x;

        public Model() throws IloException {
            int[] iScalar = new int[m];
            for (int i = 0; i < m; i++) {
                iScalar[i] = i + 1;
            }
            int[] xScalar = new int[gamma + 1];
            double[] rScalar = new double[gamma + 1];
            for (int j = 0; j <= gamma; j++) {
                xScalar[j] = j;
                rScalar[j] = computeR(j, piecewise);
            }
            int[] hSumBefore = new int[m];
            hSumBefore[0] = 0;
            for (int i = 1; i < m; i++) {
                hSumBefore[i] = hSumBefore[i - 1] + h[i - 1];
            }

            IloNumVar[][] w = new IloNumVar[m][];
            IloNumExpr[] rehandle = new IloNumExpr[m];
            x = cplex.intVarArray(m, 0, gamma);
            for (int i = 0; i < m; i++) {
                w[i] = cplex.boolVarArray(gamma + 1);
                cplex.addEq(cplex.sum(w[i]), 1);
                rehandle[i] = cplex.scalProd(rScalar, w[i]);
                cplex.addEq(x[i], cplex.scalProd(xScalar, w[i]));
            }
            cplex.addEq(cplex.sum(x), theta);
            f = cplex.sum(cplex.constant(2 * theta * tC), cplex.prod(2 * tC, cplex.sum(rehandle)), cplex.prod(tE + tL, cplex.scalProd(iScalar, x)));

            IloNumVar[] y = cplex.intVarArray(m, 0, gamma);
            IloNumVar[] z = cplex.intVarArray(m, 0, theta);
            IloNumVar[] u = cplex.boolVarArray(m);
            IloNumVar[] v = cplex.boolVarArray(m);
            for (int i = 0; i < m; i++) {
                cplex.addGe(y[i], cplex.diff(x[i], h[i]));
                cplex.addGe(y[i], cplex.diff(h[i], x[i]));
                cplex.addGe(z[i], cplex.diff(cplex.sum(x, 0, i), hSumBefore[i]));
                cplex.addGe(z[i], cplex.diff(hSumBefore[i], cplex.sum(x, 0, i)));
                cplex.addLe(y[i], cplex.prod(gamma, v[i]));
                if (i != 0) {
                    cplex.addGe(v[i - 1], v[i]);
                }
                cplex.addGe(cplex.sum(u[i], z[i]), v[i]);
            }
            g = cplex.sum(cplex.prod(tC, cplex.sum(y)), cplex.prod(tE + tL, cplex.sum(z)), cplex.prod(2 * tE, cplex.sum(u)));
        }

        @Override
        public Solution result(double time) throws IloException {
            int[] alloc = new int[m];
            for (int i = 0; i < m; i++) {
                alloc[i] = (int) Math.round(cplex.getValue(x[i]));
            }
            int[][] moves = roundTrip(alloc);
            return new Solution(cplex.getStatus() == IloCplex.Status.Optimal, time, cplex.getValue(f), computeG(moves), alloc, moves);
        }

        public Solution[] front(int K) throws IloException {
            double startTime = cplex.getCplexTime();

            cplex.add(cplex.minimize(cplex.staticLex(new IloNumExpr[]{f, g})));
            IloRange le = cplex.addLe(g, Double.POSITIVE_INFINITY);

            LinkedList<Solution> front = new LinkedList<>();

            cplex.solve();
            front.add(check(result(cplex.getCplexTime() - startTime)));

            int epsilon = Util.gcd(2 * tC, Util.gcd(tE + tL, 2 * tE));
            int num = front.getFirst().g / epsilon;

            if (2 * K < num) {
                for (int i = K - 1; i >= 0; i--) {
                    int ub = front.getFirst().g * i / K;
                    if (ub < front.getLast().g) {
                        le.setUB(ub);
                        cplex.solve();
                        front.add(check(result(cplex.getCplexTime() - startTime)));
                    }
                }
            } else {
                while (front.getLast().g - epsilon >= 0) {
                    le.setUB(front.getLast().g - epsilon);
                    cplex.solve();
                    front.add(check(result(cplex.getCplexTime() - startTime)));
                }
            }

            return front.toArray(new Solution[0]);
        }
    }
}
