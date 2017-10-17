/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.demetra.maths.matrices;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
public class SubMatrixTest {

    public SubMatrixTest() {
    }

    @Test
    @Ignore
    public void testOldMethod() {
        ec.tstoolkit.maths.matrices.Matrix Q = ec.tstoolkit.maths.matrices.Matrix.square(0);
        long t0 = System.currentTimeMillis();
        ec.tstoolkit.maths.matrices.Matrix M1 = ec.tstoolkit.maths.matrices.Matrix.square(300);
        ec.tstoolkit.maths.matrices.Matrix M2 = ec.tstoolkit.maths.matrices.Matrix.square(300);
        M1.randomize();
        M2.randomize();
        for (int i = 0; i < 100000; ++i) {
            M1.subMatrix(1, 299, 1, 299).add(M2.subMatrix(1, 299, 1, 299).transpose());
        }
        long t1 = System.currentTimeMillis();
        System.out.println("Old");
        System.out.println(t1 - t0);
    }

    @Test
    @Ignore
    public void testNewMethod() {
        Matrix Q = Matrix.square(0);
        long t0 = System.currentTimeMillis();
        Matrix M1 = Matrix.square(300);
        Matrix M2 = Matrix.square(300);
        Random rnd = new Random(0);
        M1.all().set((r, c) -> rnd.nextDouble());
        M2.all().set((r, c) -> rnd.nextDouble());
        for (int i = 0; i < 100000; ++i) {
            M1.subMatrix(1, 299, 1, 299).add(M2.subMatrix(1, 299, 1, 299).transpose());
        }
        long t1 = System.currentTimeMillis();
        System.out.println("New");
        System.out.println(t1 - t0);
    }

    @Test
    //@Ignore
    public void testProduct() {
        int L = 50, M = 30, N = 100, K = 100000;
        double[] a = new double[L * M];
        double[] b = new double[M * N];
        double[] c1 = new double[L * N];
        double[] c2 = new double[L * N];
        Random rnd = new Random();
        for (int i = 0; i < a.length; ++i) {
            a[i] = rnd.nextDouble();
        }
        for (int i = 0; i < b.length; ++i) {
            b[i] = rnd.nextDouble();
        }
        {
            ec.tstoolkit.maths.matrices.Matrix A = new ec.tstoolkit.maths.matrices.Matrix(a, L, M);
            ec.tstoolkit.maths.matrices.Matrix B = new ec.tstoolkit.maths.matrices.Matrix(b, M, N);
            ec.tstoolkit.maths.matrices.Matrix C = new ec.tstoolkit.maths.matrices.Matrix(c1, L, N);

            A.randomize();
            B.randomize();
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < K; ++i) {
                C.all().product(A.all(), B.all());
            }
            long t1 = System.currentTimeMillis();
            System.out.println("Old");
            System.out.println(t1 - t0);
        }
        {
            Matrix A = Matrix.of(a, L);
            Matrix B = Matrix.of(b, M);
            Matrix C = Matrix.of(c2, L);

            long t0 = System.currentTimeMillis();
            for (int i = 0; i < K; ++i) {
                C.all().product(A.all(), B.all());
            }
            long t1 = System.currentTimeMillis();
            System.out.println("New");
            System.out.println(t1 - t0);
        }

        for (int i = 0; i < L * N; ++i) {
            assertEquals(c1[i], c2[i], 1e-15);
        }
    }

}
