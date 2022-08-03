package model;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import util.Problem;
import util.Solution;
import util.Solver;

public abstract class ExistingModel extends Solver {
    public final boolean isLinear;

    public ExistingModel(Problem prob, boolean piecewise, boolean isLinear, int numThreads, double timeLimit, boolean feasibilityCheck) {
        super(prob, piecewise, numThreads, timeLimit, feasibilityCheck);
        this.isLinear = isLinear;
    }

    @Override
    public Model build() throws IloException {
        return new Model();
    }

    public class Model extends Formulation {
        public IloNumVar[] x;
        public IloNumVar[][] z;
        public IloNumVar[] p;
        public IloNumVar[] q;
        public int[] o;
        public IloNumExpr[] d;

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
            o = new int[theta];
            for (int i = 0, k = 0; i < m; i++) {
                for (int j = 0; j < h[i]; j++, k++) {
                    o[k] = i + 1;
                }
            }
            double C = 2 * tC * theta + tE * m * (theta + 1) + tL * m * theta;

            x = cplex.intVarArray(m, 0, gamma);
            IloNumExpr[] rehandle = new IloNumExpr[m];
            if (isLinear) {
                IloNumVar[][] w = new IloNumVar[m][];
                for (int i = 0; i < m; i++) {
                    w[i] = cplex.boolVarArray(gamma + 1);
                    cplex.addEq(cplex.sum(w[i]), 1);
                    cplex.addEq(x[i], cplex.scalProd(xScalar, w[i]));
                    rehandle[i] = cplex.scalProd(rScalar, w[i]);
                }
            } else {
                for (int i = 0; i < m; i++) {
                    rehandle[i] = cplex.sum(cplex.prod(0.25 / s + 0.0625 / s / s, cplex.square(x[i])), cplex.prod(0.125 / s - 0.25, x[i]));
                }
            }
            f = cplex.sum(cplex.constant(2 * theta * tC), cplex.prod(2 * tC, cplex.sum(rehandle)), cplex.prod(tE + tL, cplex.scalProd(iScalar, x)));

            IloNumVar[][] y = new IloNumVar[theta][];
            d = new IloNumExpr[theta];
            IloNumExpr[] degree = new IloNumExpr[theta];
            for (int ij = 0; ij < theta; ij++) {
                y[ij] = cplex.boolVarArray(m);
                d[ij] = cplex.scalProd(iScalar, y[ij]);
                degree[ij] = cplex.sum(y[ij]);
                cplex.addLe(degree[ij], 1);
                if (ij != 0 && o[ij - 1] == o[ij]) {
                    cplex.addGe(degree[ij - 1], degree[ij]);
                }
            }
            IloNumVar[][] yT = new IloNumVar[m][theta];
            IloNumVar[] a = cplex.boolVarArray(m);
            IloNumVar[] b = cplex.boolVarArray(m);
            for (int k = 0; k < m; k++) {
                for (int ij = 0; ij < theta; ij++) {
                    yT[k][ij] = y[ij][k];
                }
                cplex.addEq(cplex.sum(h[k], cplex.sum(yT[k])), cplex.sum(x[k], cplex.sum(degree, hSumBefore[k], h[k])));
                cplex.addLe(cplex.sum(degree, hSumBefore[k], h[k]), cplex.prod(gamma, a[k]));
                cplex.addLe(cplex.sum(yT[k]), cplex.prod(gamma, b[k]));
                cplex.addLe(cplex.sum(a[k], b[k]), 1);
            }
            p = cplex.boolVarArray(theta);
            cplex.addLe(cplex.sum(p), 1);
            q = cplex.boolVarArray(theta);
            cplex.addLe(cplex.sum(q), 1);
            z = new IloNumVar[theta][];
            for (int ij = 0; ij < theta; ij++) {
                z[ij] = cplex.boolVarArray(theta);
                cplex.addEq(cplex.sum(z[ij]), cplex.diff(degree[ij], q[ij]));
            }
            IloNumVar[][] zT = new IloNumVar[theta][theta];
            for (int ij = 0; ij < theta; ij++) {
                for (int kl = 0; kl < theta; kl++) {
                    zT[ij][kl] = z[kl][ij];
                }
                cplex.addEq(cplex.sum(zT[ij]), cplex.diff(degree[ij], p[ij]));
            }
            IloNumVar[][] r = new IloNumVar[theta][];
            for (int ij = 0; ij < theta; ij++) {
                r[ij] = cplex.intVarArray(m, 0, m);
                for (int k = 0; k < m; k++) {
                    cplex.addGe(r[ij][k], cplex.diff(d[ij], k + 1));
                    cplex.addGe(r[ij][k], cplex.diff(k + 1, d[ij]));
                }
            }
            IloNumVar[] c = cplex.numVarArray(theta, 0, Double.POSITIVE_INFINITY);
            IloNumExpr[] cLast = new IloNumExpr[theta];
            for (int ij = 0; ij < theta; ij++) {
                cplex.addLe(cplex.sum(cplex.constant(tE * o[ij] + 2 * tC), cplex.prod(tL, r[ij][o[ij] - 1]), cplex.prod(-C, cplex.diff(1, p[ij]))), c[ij]);
                for (int kl = 0; kl < theta; kl++) {
                    cplex.addLe(cplex.sum(c[ij], cplex.prod(tE, r[ij][o[kl] - 1]), cplex.constant(2 * tC), cplex.prod(tL, r[kl][o[kl] - 1]), cplex.prod(-C, cplex.diff(1, z[ij][kl]))), c[kl]);
                }
                cLast[ij] = cplex.sum(c[ij], cplex.prod(tE, d[ij]), cplex.prod(-C, cplex.diff(1, q[ij])));
                cplex.addLe(cLast[ij], tau);
            }

            g = cplex.max(cLast);
        }

        public int first() throws IloException {
            for (int ij = 0; ij < theta; ij++) {
                if ((int) Math.round(cplex.getValue(p[ij])) == 1) {
                    return ij;
                }
            }
            return -1;
        }

        public int advance(int ij) throws IloException {
            for (int kl = 0; kl < theta; kl++) {
                if ((int) Math.round(cplex.getValue(z[ij][kl])) == 1) {
                    return kl;
                }
            }
            return -1;
        }

        @Override
        public Solution result(double time) throws IloException {
            int[] alloc = new int[m];
            for (int i = 0; i < m; i++) {
                alloc[i] = (int) Math.round(cplex.getValue(x[i]));
            }
            int n = 0;
            for (int ij = first(); ij != -1; ij = advance(ij)) {
                n++;
            }
            int[][] moves = new int[n][2];
            for (int ij = first(), k = 0; ij != -1; ij = advance(ij), k++) {
                moves[k][0] = o[ij];
                moves[k][1] = (int) Math.round(cplex.getValue(d[ij]));
            }
            return new Solution(cplex.getStatus() == IloCplex.Status.Optimal, time, cplex.getValue(f), computeG(moves), alloc, moves);
        }
    }
}
