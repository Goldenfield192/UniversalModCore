package cam72cam.mod.util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.Test;
import util.Matrix4;
import util.NativeMatrix4;

import java.util.Arrays;

public class MatrixTest {
    //6000000 for JIT
    @Test
    public void test() {
        Matrix4 matrix4 = new Matrix4(1,5,6,1,
                                      2,2,2,2,
                                      3,3,4,3,
                                      0,0,4,4);
        long time = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            matrix4.invert();
        }
        System.out.println(matrix4);
        System.out.println(System.nanoTime() - time);
        time = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            matrix4.invert();
        }
        System.out.println(matrix4);
        System.out.println(System.nanoTime() - time);
        time = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            matrix4.invert();
        }
        System.out.println(matrix4);
        System.out.println(System.nanoTime() - time);
    }

    //17206800 7206500
    @Test
    public void test2() {
        System.load("D:\\JavaStudy\\UniversalModCore\\UMC-1.12.2\\src\\main\\java\\util\\mydll.dll");

        LongList longs =new LongArrayList();
        double[] matrix = new double[]{1,5,6,1,
                                      2,2,2,2,
                                      3,3,4,3,
                                      0,0,4,4};
        for (int j = 0; j < 10; j++) {
            long time = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                matrix = NativeMatrix4.invert(matrix);
            }
            System.out.println(matrix);
            longs.add(System.nanoTime() - time);
            time = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                matrix = NativeMatrix4.invert(matrix);
            }
            System.out.println(matrix);
            longs.add(System.nanoTime() - time);
        }
        System.out.println(matrix);
        System.out.println(Arrays.toString(longs.toArray()));
    }

    @Test
    public void test3() {
        System.load("D:\\JavaStudy\\UniversalModCore\\UMC-1.12.2\\src\\main\\java\\util\\mydll.dll");

        LongList longs =new LongArrayList();
        double[] matrix = new double[]{1,5,6,1,
                2,2,2,2,
                3,3,4,3,
                0,0,4,4};
        for (int j = 0; j < 10; j++) {
            long time = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                matrix = NativeMatrix4.invertJ(matrix);
            }
            System.out.println(matrix);
            longs.add(System.nanoTime() - time);
            time = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                matrix = NativeMatrix4.invertJ(matrix);
            }
            System.out.println(matrix);
            longs.add(System.nanoTime() - time);
        }
        System.out.println(matrix);
        System.out.println(Arrays.toString(longs.toArray()));
    }
}
