package terraria.util;

public class MathHelper {
    // source code: https://stackoverflow.com/questions/523531/fast-transcendent-trigonometric-functions-for-java
    // Return an approx to sin(pi/2 * x) where -1 <= x <= 1.
    // In that range it has a max absolute error of 5e-9
    // according to Hastings, Approximations For Digital Computers.
    public static double xsin(double x) {
        double x2 = x * x;
        return ((((.00015148419 * x2
                - .00467376557) * x2
                + .07968967928) * x2
                - .64596371106) * x2
                + 1.57079631847) * x;
    }
    public static int betterFloorDivision(int dividend, int divisor) {
        int result = dividend / divisor;
        if (dividend < 0 && dividend % divisor != 0) result --;
        return result;
    }

}
