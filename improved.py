# This program is part of the paper "Inbound container remarshaling
# problem in an automated container terminal".
#
# Copyright (c) 2022 Bo Jin <jinbostar@gmail.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

import mip
import time

import irmp


def solve(prob):
    m = prob.m
    gamma = prob.gamma
    theta = prob.theta
    tC = prob.tC
    tE = prob.tE
    tL = prob.tL
    h = prob.h
    tau = prob.tau

    model = mip.Model('Improved model', mip.MINIMIZE)

    u = [model.add_var(f'u_{i}', var_type=mip.BINARY) for i in range(m)]
    v = [model.add_var(f'v_{i}', var_type=mip.BINARY) for i in range(m)]
    w = {(i, j): model.add_var(f'w_{i}_{j}', 0, m, var_type=mip.INTEGER) for i in range(m) for j in range(gamma)}
    x = [model.add_var(f'x_{i}', 0, gamma, var_type=mip.INTEGER) for i in range(m)]
    y = [model.add_var(f'y_{i}', 0, gamma, var_type=mip.INTEGER) for i in range(m)]
    z = [model.add_var(f'z_{i}', 0, gamma, var_type=mip.INTEGER) for i in range(m)]

    model.objective = 2 * theta * tC + \
                      2 * tC * mip.xsum(prob.rehandles(j) * w[i, j] for i in range(m) for j in range(gamma)) + \
                      (tE + tL) * mip.xsum(i * x[i] for i in range(m))

    for i in range(m):
        model += mip.xsum(x[i] for i in range(m)) == theta

    for i in range(m):
        model += mip.xsum(w[i, j] for j in range(gamma)) == 1

    for i in range(m):
        model += mip.xsum(j * w[i, j] for j in range(gamma)) == x[i]

    model += tC * mip.xsum(y[i] for i in range(m)) + \
             (tE + tL) * mip.xsum(z[i] for i in range(m)) + \
             2 * tE * mip.xsum(u[i] for i in range(m)) <= tau

    for i in range(m):
        model += y[i] >= h[i] - x[i]

    for i in range(m):
        model += y[i] >= x[i] - h[i]

    for i in range(m):
        model += z[i] >= mip.xsum(h[ii] - x[ii] for ii in range(i))

    for i in range(m):
        model += z[i] >= mip.xsum(x[ii] - h[ii] for ii in range(i))

    for i in range(m):
        model += y[i] <= gamma * v[i]

    for i in range(m - 1):
        model += v[i] >= v[i + 1]

    for i in range(m):
        model += u[i] + z[i] >= v[i]

    model.max_mip_gap = 0
    model.integer_tol = 0
    model.verbose = 0

    start_time = time.time()
    status = model.optimize(3600)
    time_elapsed = time.time() - start_time

    if status == mip.OptimizationStatus.OPTIMAL:
        print(f'time elapsed = {time_elapsed}')
        print(f'objective value = {model.objective_value}')
        print(f'x = {[x[i].x for i in range(m)]}')


if __name__ == '__main__':
    prob = irmp.read_file('data/small-2.txt')

    solve(prob)
