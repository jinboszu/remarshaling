# Copyright (c) 2021 Bo Jin <jinbostar@gmail.com>
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


def solve(prob, tight):
    m = prob.m
    gamma = prob.gamma
    theta = prob.theta
    tC = prob.tC
    tE = prob.tE
    tL = prob.tL
    h = prob.h
    tau = prob.tau
    n = prob.n if tight else theta

    model = mip.Model('Basic model', mip.MINIMIZE)

    a = [model.add_var(f'a_{i}', var_type=mip.BINARY) for i in range(m)]
    b = [model.add_var(f'b_{i}', var_type=mip.BINARY) for i in range(m)]
    r = [model.add_var(f'r_{j}', var_type=mip.BINARY) for j in range(n)]
    o = [model.add_var(f'o_{j}', 0, m, var_type=mip.INTEGER) for j in range(n)]
    d = [model.add_var(f'd_{j}', 0, m, var_type=mip.INTEGER) for j in range(n)]
    e = [model.add_var(f'e_{j}', 0, m, var_type=mip.INTEGER) for j in range(n - 1)]
    l = [model.add_var(f'l_{j}', 0, m, var_type=mip.INTEGER) for j in range(n)]
    w = {(i, j): model.add_var(f'w_{i}_{j}', 0, m, var_type=mip.INTEGER) for i in range(m) for j in range(gamma)}
    x = [model.add_var(f'x_{i}', 0, gamma, var_type=mip.INTEGER) for i in range(m)]
    y = {(i, j): model.add_var(f'y_{i}_{j}', var_type=mip.BINARY) for i in range(m) for j in range(n)}
    z = {(i, j): model.add_var(f'z_{i}_{j}', var_type=mip.BINARY) for i in range(m) for j in range(n)}

    model.objective = 2 * theta * tC + \
                      2 * tC * mip.xsum(prob.rehandles(j) * w[i, j] for i in range(m) for j in range(gamma)) + \
                      (tE + tL) * mip.xsum(i * x[i] for i in range(m))

    for i in range(m):
        model += x[i] == h[i] - mip.xsum(y[i, j] for j in range(n)) + mip.xsum(z[i, j] for j in range(n))

    for i in range(m):
        model += mip.xsum(w[i, j] for j in range(gamma)) == 1

    for i in range(m):
        model += mip.xsum(j * w[i, j] for j in range(gamma)) == x[i]

    for i in range(m):
        model += mip.xsum(y[i, j] for j in range(n)) <= gamma * a[i]

    for i in range(m):
        model += mip.xsum(z[i, j] for j in range(n)) <= gamma * b[i]

    for i in range(m):
        model += a[i] + b[i] <= 1

    model += 2 * tC * mip.xsum(r[j] for j in range(n)) + \
             tE * (o[0] + d[n - 1] + mip.xsum(e[j] for j in range(n - 1))) + \
             tL * mip.xsum(l[j] for j in range(n)) <= tau

    for j in range(n):
        model += r[j] == mip.xsum(y[i, j] for i in range(m))

    for j in range(n):
        model += r[j] == mip.xsum(z[i, j] for i in range(m))

    for j in range(n - 1):
        model += r[j] >= r[j + 1]

    for j in range(n):
        model += o[j] == mip.xsum(i * y[i, j] for i in range(m))

    for j in range(n):
        model += d[j] == mip.xsum(i * z[i, j] for i in range(m))

    for j in range(n - 1):
        model += e[j] >= d[j] - o[j + 1]

    for j in range(n - 1):
        model += e[j] >= o[j + 1] - d[j]

    for j in range(n):
        model += l[j] >= o[j] - d[j]

    for j in range(n):
        model += l[j] >= d[j] - o[j]

    model.max_mip_gap = 0
    model.integer_tol = 0
    model.verbose = 0

    start_time = time.time()
    status = model.optimize(3600)
    time_elapsed = time.time() - start_time

    if status == mip.OptimizationStatus.OPTIMAL:
        print(f'n = {n}')
        print(f'time elapsed = {time_elapsed}')
        print(f'objective value = {model.objective_value}')
        print(f'x = {[x[i].x for i in range(m)]}')


if __name__ == '__main__':
    prob = irmp.read_file('data/small-2.txt')

    solve(prob, False)
    solve(prob, True)
