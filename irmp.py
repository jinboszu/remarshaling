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

import json


class Problem:
    def __init__(self, js):
        self.s = js['s']
        self.m = js['m']
        self.h = js['h']
        h_sorted = sorted(self.h)
        self.theta = js['theta']
        self.gamma = js['gamma']
        k = self.theta // self.gamma
        self.n = max(k * self.gamma, self.theta - h_sorted[k]) - sum(h_sorted[:k])
        self.tC = js['tC']
        self.tE = js['tE']
        self.tL = js['tL']
        self.tau = js['tau']

    def rehandles(self, x):
        s = self.s
        if x <= s:
            return 0
        elif x < 2 * s:
            return (s + 2) / (2 * s + 2) * x - s * (s + 2) / (2 * s + 2)
        else:
            return (1 / 4 / s + 1 / 16 / s / s) * x * x + (1 / 8 / s - 1 / 4) * x


def read_file(file):
    with open(file, 'r') as f:
        return Problem(json.load(f))
