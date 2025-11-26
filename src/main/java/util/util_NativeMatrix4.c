#include "jni.h"
#include <stdlib.h>
#include <string.h>
#include <math.h>
int main() {

}

// 交换两行
static void swap_rows(jdouble *matrix, int i, int j) {
    jdouble temp[4];
    memcpy(temp, &matrix[i*4], 4 * sizeof(jdouble));
    memcpy(&matrix[i*4], &matrix[j*4], 4 * sizeof(jdouble));
    memcpy(&matrix[j*4], temp, 4 * sizeof(jdouble));
}

// LU分解函数，返回置换矩阵的索引
static int lu_decomposition(jdouble *A, jdouble *L, jdouble *U, int *pivot) {
    const int n = 4;
    int i, j, k;

    // 初始化L为单位下三角矩阵，U为A的副本
    for (i = 0; i < n; i++) {
        for (j = 0; j < n; j++) {
            if (i == j) {
                L[i*n + j] = 1.0f;
            } else {
                L[i*n + j] = 0.0f;
            }
            U[i*n + j] = A[i*n + j];
        }
        pivot[i] = i;
    }

    // 进行LU分解
    for (k = 0; k < n - 1; k++) {
        // 部分主元选择
        int max_row = k;
        jdouble max_val = fabsf(U[k*n + k]);

        for (i = k + 1; i < n; i++) {
            jdouble val = fabsf(U[i*n + k]);
            if (val > max_val) {
                max_val = val;
                max_row = i;
            }
        }

        // 如果最大主元为0，矩阵奇异
        if (max_val < 1e-8f) {
            return 0; // 失败
        }

        // 交换行
        if (max_row != k) {
            swap_rows(U, k, max_row);
            swap_rows(L, k, max_row);

            // 更新置换索引
            int temp = pivot[k];
            pivot[k] = pivot[max_row];
            pivot[max_row] = temp;
        }

        // 消元
        for (i = k + 1; i < n; i++) {
            L[i*n + k] = U[i*n + k] / U[k*n + k];
            for (j = k; j < n; j++) {
                U[i*n + j] -= L[i*n + k] * U[k*n + j];
            }
        }
    }

    return 1; // 成功
}

// 解下三角方程组 L * y = b
static void forward_substitution(jdouble *L, jdouble *b, jdouble *y) {
    const int n = 4;
    int i, j;

    for (i = 0; i < n; i++) {
        y[i] = b[i];
        for (j = 0; j < i; j++) {
            y[i] -= L[i*n + j] * y[j];
        }
        y[i] /= L[i*n + i]; // L[i][i] = 1
    }
}

// 解上三角方程组 U * x = y
static void backward_substitution(jdouble *U, jdouble *y, jdouble *x) {
    const int n = 4;
    int i, j;

    for (i = n - 1; i >= 0; i--) {
        x[i] = y[i];
        for (j = i + 1; j < n; j++) {
            x[i] -= U[i*n + j] * x[j];
        }
        x[i] /= U[i*n + i];
    }
}

JNIEXPORT jdoubleArray JNICALL Java_util_NativeMatrix4_invert
  (JNIEnv *env, jobject obj, jdoubleArray matrixArray) {

    // 获取输入矩阵数据
    jdouble *input = (*env)->GetDoubleArrayElements(env, matrixArray, NULL);
    if (input == NULL) {
        return NULL;
    }

    // 创建输出数组
    jdoubleArray result = (*env)->NewDoubleArray(env, 16);
    if (result == NULL) {
        (*env)->ReleaseDoubleArrayElements(env, matrixArray, input, JNI_ABORT);
        return NULL;
    }

    const int n = 4;
    jdouble L[16], U[16];
    int pivot[4];

    // 进行LU分解
    if (!lu_decomposition(input, L, U, pivot)) {
        // 矩阵奇异，不可逆
        (*env)->ReleaseDoubleArrayElements(env, matrixArray, input, JNI_ABORT);
        return NULL;
    }

    jdouble inv[16];
    jdouble b[4], y[4], x[4];

    // 对每一列求解 A * inv_col = e_col
    for (int col = 0; col < n; col++) {
        // 构造单位向量（考虑行置换）
        for (int i = 0; i < n; i++) {
            b[i] = (pivot[i] == col) ? 1.0f : 0.0f;
        }

        // 前向替换：L * y = b
        forward_substitution(L, b, y);

        // 后向替换：U * x = y
        backward_substitution(U, y, x);

        // 将解存入逆矩阵的对应列
        for (int i = 0; i < n; i++) {
            inv[i*n + col] = x[i];
        }
    }

    // 设置结果数组
    (*env)->SetDoubleArrayRegion(env, result, 0, 16, inv);

    // 释放输入数组
    (*env)->ReleaseDoubleArrayElements(env, matrixArray, input, JNI_ABORT);

    return result;
}
