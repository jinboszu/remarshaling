package model;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import util.Problem;
import util.Solution;
import util.Solver;

import java.util.Arrays;

public abstract class BasicModel extends Solver {
    public final boolean tight;

    public BasicModel(Problem prob, boolean piecewise, boolean tight, int numThreads, double timeLimit, boolean feasibilityCheck) {
        super(prob, piecewise, numThreads, timeLimit, feasibilityCheck);
        this.tight = tight;
    }

    @Override
    public Model build() throws IloException {
        return new Model();
    }

    public class Model extends Formulation {
        public IloNumVar[] x;
        public IloNumVar[] r;
        public IloNumVar[] o;
        public IloNumVar[] d;

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
            int n = theta;
            if (tight) {
                int[] hStar = Arrays.stream(h).sorted().toArray();
                int pi = theta / gamma;
                n = Math.max(pi * gamma, theta - hStar[pi]) - Arrays.stream(hStar).limit(pi).sum();
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
            f = cplex.sum(cplex.constant(2 * theta * tC), cplex.prod(2 * tC, cplex.sum(rehandle)), cplex.prod(tE + tL, cplex.scalProd(iScalar, x)));

            IloNumVar[][] y = new IloNumVar[m][];
            IloNumVar[][] z = new IloNumVar[m][];
            IloNumVar[] a = cplex.boolVarArray(m);
            IloNumVar[] b = cplex.boolVarArray(m);
            for (int i = 0; i < m; i++) {
                y[i] = cplex.boolVarArray(n);
                z[i] = cplex.boolVarArray(n);
                cplex.addEq(cplex.sum(x[i], cplex.sum(y[i])), cplex.sum(h[i], cplex.sum(z[i])));
                cplex.addLe(cplex.sum(y[i]), cplex.prod(gamma, a[i]));
                cplex.addLe(cplex.sum(z[i]), cplex.prod(gamma, b[i]));
                cplex.addLe(cplex.sum(a[i], b[i]), 1);
            }
            IloNumVar[][] yT = new IloNumVar[n][m];
            IloNumVar[][] zT = new IloNumVar[n][m];
            r = cplex.boolVarArray(n);
            o = cplex.intVarArray(n, 0, m); // origin
            d = cplex.intVarArray(n, 0, m); // destination
            IloNumVar[] e = cplex.intVarArray(n - 1, 0, m); // empty drive
            IloNumVar[] l = cplex.intVarArray(n, 0, m); // loaded drive
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < m; i++) {
                    yT[j][i] = y[i][j];
                    zT[j][i] = z[i][j];
                }
                cplex.addEq(r[j], cplex.sum(yT[j]));
                cplex.addEq(r[j], cplex.sum(zT[j]));
                if (j != 0) {
                    cplex.addGe(r[j - 1], r[j]);
                }
                cplex.addEq(o[j], cplex.scalProd(iScalar, yT[j]));
                cplex.addEq(d[j], cplex.scalProd(iScalar, zT[j]));
                if (j != n - 1) {
                    cplex.addGe(e[j], cplex.diff(d[j], o[j + 1]));
                    cplex.addGe(e[j], cplex.diff(o[j + 1], d[j]));
                }
                cplex.addGe(l[j], cplex.diff(o[j], d[j]));
                cplex.addGe(l[j], cplex.diff(d[j], o[j]));
            }
            g = cplex.sum(cplex.prod(2 * tC, cplex.sum(r)), cplex.prod(tE, o[0]), cplex.prod(tE, d[n - 1]), cplex.prod(tE, cplex.sum(e)), cplex.prod(tL, cplex.sum(l)));
        }

        @Override
        public Solution result(double time) throws IloException {
            int[] alloc = new int[m];
            for (int i = 0; i < m; i++) {
                alloc[i] = (int) Math.round(cplex.getValue(x[i]));
            }
            int n = (int) Math.round(cplex.getValue(cplex.sum(r)));
            int[][] moves = new int[n][2];
            for (int j = 0; j < n; j++) {
                moves[j][0] = (int) Math.round(cplex.getValue(o[j]));
                moves[j][1] = (int) Math.round(cplex.getValue(d[j]));
            }
            return new Solution(cplex.getStatus() == IloCplex.Status.Optimal, time, cplex.getValue(f), computeG(moves), alloc, moves);
        }
    }
}
