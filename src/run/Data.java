package run;

import util.Problem;
import util.Util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

public class Data {

    public static int[] uniform(int m, int gamma, int theta, Random rnd) {
        int[] indices = IntStream.range(0, m).toArray();
        int remain = m;
        int[] h = new int[m];
        for (int j = 0; j < theta; j++) {
            int i = rnd.nextInt(remain);
            h[indices[i]]++;
            if (h[indices[i]] == gamma) {
                indices[i] = indices[--remain];
            }
        }
        return h;
    }

    public static int[] seaside(int m, int gamma, int theta, Random rnd) {
        int[] indices = IntStream.range(0, m).toArray();
        int totalWeight = m * (m + 1) / 2;
        int remain = m;
        int[] h = new int[m];
        for (int j = 0; j < theta; j++) {
            double key = rnd.nextInt(totalWeight);
            int i = 0;
            while (key >= indices[i] + 1) {
                key -= indices[i] + 1;
                i++;
            }

            h[indices[i]]++;
            if (h[indices[i]] == gamma) {
                totalWeight -= indices[i] + 1;
                indices[i] = indices[--remain];
            }
        }
        return h;
    }

    public static int[] landside(int m, int gamma, int theta, Random rnd) {
        int[] indices = IntStream.range(0, m).toArray();
        int totalWeight = m * (m + 1) / 2;
        int remain = m;
        int[] h = new int[m];
        for (int j = 0; j < theta; j++) {
            double key = rnd.nextInt(totalWeight);
            int i = 0;
            while (key >= m - indices[i]) {
                key -= m - indices[i];
                i++;
            }

            h[indices[i]]++;
            if (h[indices[i]] == gamma) {
                totalWeight -= m - indices[i];
                indices[i] = indices[--remain];
            }
        }
        return h;
    }

    public static void main(String[] args) throws IOException {
        Random rnd = new Random(0);
        String[] names = {"tiny", "small", "medium", "large"};
        int[] mValues = {5, 10, 20, 50};
        int[] sValues = {4, 6, 8, 10};
        int[] heightValues = {5, 5, 6, 6};
        int[] thetaValues = {20, 200, 500, 1600};
        int[] tauValues = {200, 1000, 10000, 100000};
        for (int scale = 0; scale < 4; scale++) {
            int m = mValues[scale];
            int s = sValues[scale];
            int gamma = s * heightValues[scale];
            int theta = thetaValues[scale];
            int tC = 20;
            int tE = 3;
            int tL = 5;
            int tau = tauValues[scale];
            for (int num = 1; num <= 10; num++) {
                int[] h = uniform(m, gamma, theta, rnd);
                Problem prob = new Problem(m, s, gamma, theta, h, tC, tE, tL, tau);
                System.out.printf("data/%s-%d.txt: %s\n", names[scale], num, prob);
                try (FileWriter writer = new FileWriter(String.format("data/%s-%d.txt", names[scale], num))) {
                    Util.gson.toJson(prob, writer);
                    writer.write("\n");
                }
            }
        }
    }
}
