package util;

public class NativeMatrix4 {
    public static native double[] invert(double[] matrix4);
        private static final double EPSILON = 1e-12;

        /**
         * 使用LU分解方法求4x4矩阵的逆矩阵
         * @param matrix 输入矩阵，按行优先顺序存储的16个元素数组
         * @return 逆矩阵，如果矩阵不可逆则返回null
         */
        public static double[] invertJ(double[] matrix) {
            if (matrix == null || matrix.length != 16) {
                throw new IllegalArgumentException("输入必须是16个元素的数组");
            }

            // 进行LU分解
            double[][] luResult = luDecomposition(matrix);
            if (luResult == null) {
                return null; // 矩阵奇异，不可逆
            }

            double[] L = luResult[0];
            double[] U = luResult[1];
            int[] pivot = new int[4];
            // 将float数组转换回int数组
            for (int i = 0; i < 4; i++) {
                pivot[i] = (int) luResult[2][i];
            }

            double[] inv = new double[16];
            double[] b = new double[4];
            double[] y = new double[4];
            double[] x = new double[4];

            // 对每一列求解 A * inv_col = e_col
            for (int col = 0; col < 4; col++) {
                // 构造单位向量（考虑行置换）
                for (int i = 0; i < 4; i++) {
                    b[i] = (pivot[i] == col) ? 1.0 : 0.0;
                }

                // 前向替换：L * y = b
                forwardSubstitution(L, b, y);

                // 后向替换：U * x = y
                backwardSubstitution(U, y, x);

                // 将解存入逆矩阵的对应列
                for (int i = 0; i < 4; i++) {
                    inv[i * 4 + col] = x[i];
                }
            }

            return inv;
        }

        /**
         * LU分解，返回包含[L, U, pivot]的数组
         */
        private static double[][] luDecomposition(double[] A) {
            double[] L = new double[16];
            double[] U = new double[16];
            int[] pivot = new int[4];

            // 初始化L为单位下三角矩阵，U为A的副本
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (i == j) {
                        L[i * 4 + j] = 1.0;
                    } else {
                        L[i * 4 + j] = 0.0;
                    }
                    U[i * 4 + j] = A[i * 4 + j];
                }
                pivot[i] = i;
            }

            // 进行LU分解
            for (int k = 0; k < 3; k++) { // k从0到2（4x4矩阵需要3步消元）
                // 部分主元选择
                int maxRow = k;
                double maxVal = Math.abs(U[k * 4 + k]);

                for (int i = k + 1; i < 4; i++) {
                    double val = Math.abs(U[i * 4 + k]);
                    if (val > maxVal) {
                        maxVal = val;
                        maxRow = i;
                    }
                }

                // 如果最大主元为0，矩阵奇异
                if (maxVal < EPSILON) {
                    return null; // 失败
                }

                // 交换行
                if (maxRow != k) {
                    swapRows(U, k, maxRow);
                    swapRows(L, k, maxRow);

                    // 更新置换索引
                    int temp = pivot[k];
                    pivot[k] = pivot[maxRow];
                    pivot[maxRow] = temp;
                }

                // 消元
                for (int i = k + 1; i < 4; i++) {
                    L[i * 4 + k] = U[i * 4 + k] / U[k * 4 + k];
                    for (int j = k; j < 4; j++) {
                        U[i * 4 + j] -= L[i * 4 + k] * U[k * 4 + j];
                    }
                }
            }

            // 检查最后一个主元
            if (Math.abs(U[3 * 4 + 3]) < EPSILON) {
                return null;
            }

            // 将pivot数组转换为double数组以便返回
            double[] pivotDouble = new double[4];
            for (int i = 0; i < 4; i++) {
                pivotDouble[i] = pivot[i];
            }

            return new double[][]{L, U, pivotDouble};
        }

        /**
         * 解下三角方程组 L * y = b
         */
        private static void forwardSubstitution(double[] L, double[] b, double[] y) {
            for (int i = 0; i < 4; i++) {
                y[i] = b[i];
                for (int j = 0; j < i; j++) {
                    y[i] -= L[i * 4 + j] * y[j];
                }
                y[i] /= L[i * 4 + i]; // L[i][i] = 1
            }
        }

        /**
         * 解上三角方程组 U * x = y
         */
        private static void backwardSubstitution(double[] U, double[] y, double[] x) {
            for (int i = 3; i >= 0; i--) {
                x[i] = y[i];
                for (int j = i + 1; j < 4; j++) {
                    x[i] -= U[i * 4 + j] * x[j];
                }
                x[i] /= U[i * 4 + i];
            }
        }

        /**
         * 交换矩阵中的两行
         */
        private static void swapRows(double[] matrix, int i, int j) {
            double[] temp = new double[4];
            System.arraycopy(matrix, i * 4, temp, 0, 4);
            System.arraycopy(matrix, j * 4, matrix, i * 4, 4);
            System.arraycopy(temp, 0, matrix, j * 4, 4);
        }

        /**
         * 矩阵乘法，用于验证结果
         */
        public static double[] multiply(double[] a, double[] b) {
            double[] result = new double[16];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    double sum = 0;
                    for (int k = 0; k < 4; k++) {
                        sum += a[i * 4 + k] * b[k * 4 + j];
                    }
                    result[i * 4 + j] = sum;
                }
            }
            return result;
        }

        /**
         * 创建单位矩阵
         */
        public static double[] identityMatrix() {
            double[] identity = new double[16];
            for (int i = 0; i < 4; i++) {
                identity[i * 4 + i] = 1.0;
            }
            return identity;
        }

        /**
         * 计算矩阵与向量的乘积
         */
        public static double[] multiplyMatrixVector(double[] matrix, double[] vector) {
            double[] result = new double[4];
            for (int i = 0; i < 4; i++) {
                result[i] = 0;
                for (int j = 0; j < 4; j++) {
                    result[i] += matrix[i * 4 + j] * vector[j];
                }
            }
            return result;
        }

        /**
         * 打印矩阵（调试用）
         */
        public static void printMatrix(double[] matrix) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    System.out.printf("%12.8f ", matrix[i * 4 + j]);
                }
                System.out.println();
            }
            System.out.println();
        }

        /**
         * 计算矩阵的误差（与单位矩阵的差异）
         */
        public static double computeError(double[] matrix) {
            double[] identity = identityMatrix();
            double error = 0;
            for (int i = 0; i < 16; i++) {
                double diff = matrix[i] - identity[i];
                error += diff * diff;
            }
            return Math.sqrt(error);
        }

        // 测试示例
        public static void main(String[] args) {
            // 测试矩阵（可逆）
            double[] testMatrix = {
                    4, 3, 2, 1,
                    3, 4, 3, 2,
                    2, 3, 4, 3,
                    1, 2, 3, 4
            };

            System.out.println("原始矩阵:");
            printMatrix(testMatrix);

            double[] inverse = invert(testMatrix);

            if (inverse != null) {
                System.out.println("逆矩阵:");
                printMatrix(inverse);

                // 验证：原始矩阵 × 逆矩阵 = 单位矩阵
                double[] identity = multiply(testMatrix, inverse);
                System.out.println("验证 (原始矩阵 × 逆矩阵):");
                printMatrix(identity);

                double error = computeError(identity);
                System.out.printf("与单位矩阵的误差: %.12e\n", error);
            } else {
                System.out.println("矩阵不可逆");
            }

            // 测试奇异矩阵
            double[] singularMatrix = {
                    1, 2, 3, 4,
                    2, 4, 6, 8,  // 这行是第1行的2倍
                    3, 6, 9, 12, // 这行是第1行的3倍
                    4, 8, 12, 16 // 这行是第1行的4倍
            };

            System.out.println("奇异矩阵:");
            printMatrix(singularMatrix);

            double[] inverseSingular = invert(singularMatrix);
            if (inverseSingular == null) {
                System.out.println("正确检测到奇异矩阵");
            }

            // 测试更复杂的矩阵
            double[] complexMatrix = {
                    1.23456789, 2.34567890, 3.45678901, 4.56789012,
                    0.12345678, 1.23456789, 4.56789012, 2.34567890,
                    2.34567890, 4.56789012, 1.23456789, 1.12345678,
                    3.45678901, 1.23456789, 2.34567890, 5.67890123
            };

            System.out.println("复杂矩阵:");
            printMatrix(complexMatrix);

            double[] inverseComplex = invert(complexMatrix);
            if (inverseComplex != null) {
                System.out.println("复杂矩阵的逆矩阵:");
                printMatrix(inverseComplex);

                // 验证
                double[] identityComplex = multiply(complexMatrix, inverseComplex);
                double errorComplex = computeError(identityComplex);
                System.out.printf("复杂矩阵验证误差: %.12e\n", errorComplex);
            }
        }

}
