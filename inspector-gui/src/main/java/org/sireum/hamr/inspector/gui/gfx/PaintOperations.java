/*
 * Copyright (c) 2020, Matthew Weis, Kansas State University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sireum.hamr.inspector.gui.gfx;


import javafx.scene.paint.Color;

/**
 * Holds utility methods for applying algorithms to certain colors.
 *
 * Goal: Given a FSM with N states and T[n] transitions per state, colors should be given these priorities:
 *
 * (1) Each of the states should be given a colors which are:
 *   (a) uniformly distinct from one-another
 *   (b) light enough to allow text to be read on top of them
 *   (c) (potentially) are decided through state-name hashes so that common states across FSMs
 * (2) Each state's arrows should be either black or colors closely-analogous to the state color
 *
 * See: https://www.canva.com/colors/color-wheel/ for explanations of how the below methods work.
 */
public final class PaintOperations {

    /**
     * See https://www.canva.com/colors/color-wheel/ "Complementary"
     * @param input input color whose complement is to be returned
     * @return the input {@link java.awt.Color}'s complement
     */
    public static Color complementary(Color input) {
        final double[] hsb = rgbToHsb(input);
        return Color.hsb(circularRem(hsb[0] + 0.5, 1.0) * 360.0, hsb[1], hsb[2]);
    }

    /**
     * Returns monochromatic shade from initial color, stepping lighter (positive step) or darker (negative step)
     * based on input.
     * @param input
     * @param step
     * @return
     */
    public static Color monochromatic(Color input, int step) {
        return monochromatic(input, step, 0.05);
    }

    public static Color monochromatic(Color input, int step, double stride) {
        final double[] hsb = rgbToHsb(input);
        final double b = confineToRange(hsb[2] + step * stride, 0.0, 1.0);
        return Color.hsb(hsb[0] * 360.0, hsb[1], b);
    }

    public static Color analogous(Color input, int step) {
        return analogous(input, step, 6.0 / 360.0);
    }

    public static Color analogous(Color input, int step, double stride) {
        final double[] hsb = rgbToHsb(input);
        final double h = circularRem(hsb[0] + step * stride, 1.0);
        return Color.hsb(h * 360.0, hsb[1], hsb[2]);
    }

    /**
     * Return n evenly spaced colors across the color wheel from a reference color which is both the
     * zero-index value of the returned color array and the determining factor of the hue and brightness used
     * across the returned array of colors.
     *
     * "n"-adic coloring
     *
     * @param reference
     * @param n
     * @return
     */
    public static Color[] uniformlyDistantColoring(Color reference, int n) {
        final double fn = (double) n;
        final double[] hsb = rgbToHsb(reference);
        final double stride = 1.0 / fn;

        final Color[] arr = new Color[n];
        for (int i=0; i < arr.length; i++) {
            final double h = circularRem(hsb[0] + i * stride, 1.0);
            arr[i] = Color.hsb(h * 360.0, hsb[1], hsb[2]);
        }

        return arr;
    }


    /**
     * return hue, saturation and brightness at the 0th, 1st, and 2nd positions in the array
     *
     * hue: 0.0 to 1.0 (later denormalized in a 0.0 through 360.0 scale)
     * saturation: 0.0 to 1.0 (later denormalized in a 0.0 through 100.0 scale)
     * brightness: 0.0 to 1.0 (later denormalized in a 0.0 through 100.0 scale)
     *
     * @param input
     * @return
     */
    public static double[] rgbToHsb(Color input) {
        final double[] hsb = new double[3];
        hsb[0] = input.getHue() / 360.0;
        hsb[1] = input.getSaturation();
        hsb[2] = input.getBrightness();

        return hsb;
    }

    /**
     * Modulo function that "wraps" around when given negative values instead of resulting in a negative result.
     * Assumes value is in range of [-modulus to modulus]
     *
     * todo fix for multiple wrap-arounds
     *
     * @param value
     * @param modulus
     * @return
     */
    public static double circularRem(double value, double modulus) {
        return ((value % modulus) + modulus) % modulus;
    }

    public static double confineToRange(double value, double min, double max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

}
