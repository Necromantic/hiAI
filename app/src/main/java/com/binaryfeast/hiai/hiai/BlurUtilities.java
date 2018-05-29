package com.binaryfeast.hiai.hiai;

import android.graphics.Bitmap;

import android.graphics.Color;



/**

 * Blurring functions for Android bitmaps

 *

 * Sliding window algorithm inspired by http://elynxsdk.free.fr/ext-docs/Blur/Fast_box_blur.pdf

 *

 * Released into the public domain

 *

 * Created by chedabob on 30/03/2014.

 * https://gist.github.com/chedabob/9870984

 *

 */

public class BlurUtilities {



    /**

     * Approximates a gaussian blur to within 3% by performing 3 box blurs on the source

     * @param input The input bitmap

     * @param radius The radius of the blur function in pixels

     * @return Blurred bitmap

     */

    public static Bitmap approxGaussianBlur (Bitmap input, int radius) {



        Bitmap temp = null;



        for (int i = 0; i < 3; i++) {

            if (temp == null) {

                temp = boxBlur(input,radius);

            }

            else {

                temp = boxBlur(temp,radius);

            }

        }

        return temp;

    }



    /**

     * Box blur using a sliding window

     * @param input The input bitmap

     * @param radius The radius of the blur function in pixels

     * @return Blurred bitmap

     */

    public static Bitmap boxBlur(Bitmap input, int radius) {

        return horizontalPass(verticalPass(input, radius), radius);

    }



    /**

     * Horizontal motion blur component of the box blur

     * @param input The input bitmap

     * @param radius The radius of the blur function in pixels

     * @return Horizontally blurred bitmap

     */

    private static Bitmap horizontalPass(Bitmap input, int radius) {



        int width = input.getWidth();

        int height = input.getHeight();



        int [] pixels = new int [width * height];

        input.getPixels(pixels,0,width,0,0,width,height);



        int [] outPixels = new int [width * height];

        input.getPixels(outPixels,0,width,0,0,width,height);



        for (int y = 0; y < height; y++) {

            int averageA = 0;

            int currentRightMost, previousLeftMost;
            int currentR = 0, currentG = 0, currentB = 0, currentA = 0;

            for (int x = 0; x < width; x++) {

                if (x == 0) {

                    for (int i = -radius; i <= radius; i++) {

                        int pixel = pixels[(y * width) + wrapValue(x + i, width)];


                        int a = Color.alpha(pixel);

                        averageA += a;

                    }

                } else {

                    currentRightMost = pixels[(y * width) + wrapValue(x + radius, width)];

                    previousLeftMost = pixels[(y * width) + wrapValue((x - 1) - radius, width)];



                    currentR = Color.red(currentRightMost);

                    currentG = Color.green(currentRightMost);

                    currentB = Color.blue(currentRightMost);

                    currentA = Color.alpha(currentRightMost);


                    int prevA = Color.alpha(previousLeftMost);


                    averageA -= prevA;

                    averageA += currentA;

                }



                int newColor = Color.argb( clampColour(averageA / (2 * radius + 1)), currentR, currentG, currentB);

                outPixels[(y * width) + x] = newColor;

            }

        }



        return Bitmap.createBitmap(outPixels,width,height,input.getConfig());

    }



    /**

     * Vertically motion blur component of the box blur

     * @param input The input bitmap

     * @param radius The radius of the blur function in pixels

     * @return Vertically blurred bitmap

     */

    private static Bitmap verticalPass(Bitmap input, int radius) {

        int width = input.getWidth();

        int height = input.getHeight();

        int [] pixels = new int [width * height];

        input.getPixels(pixels,0,width,0,0,width,height);



        int [] outPixels = new int [width * height];

        input.getPixels(outPixels,0,width,0,0,width,height);



        for (int x = 0; x < width; x++) {



            int averageA = 0;

            int currentRightMost = 0, previousLeftMost = 0;
            int currentR = 0, currentG = 0, currentB = 0, currentA = 0;

            for (int y = 0; y < height; y++) {



                if (y == 0) {

                    for (int i = -radius; i <= radius; i++) {

                        int pixel = pixels[(wrapValue(y + i, height )* width) + x];

                        float a = Color.alpha(pixel);


                        averageA += a;

                    }

                } else {

                    currentRightMost = pixels[ (wrapValue(y + radius, height) * width) + x ];

                    previousLeftMost = pixels[ (wrapValue((y- 1) - radius, height) * width) + x ];



                    currentR = Color.red(currentRightMost);

                    currentG = Color.green(currentRightMost);

                    currentB = Color.blue(currentRightMost);

                    currentA = Color.alpha(currentRightMost);


                    int prevA = Color.alpha(previousLeftMost);


                    averageA -= prevA;

                    averageA += currentA;

                }



                int newColor = Color.argb( clampColour(averageA / (2 * radius + 1)), currentR, currentG, currentB);

                outPixels[(y * width) + x] = newColor;

            }

        }



        return Bitmap.createBitmap(outPixels,width,height,input.getConfig());

    }



    /**

     * Clamps a colour between 0 and 255

     * @param input Value to clamp

     * @return Clamped value

     */

    private static int clampColour(int input) {

        if (input < 0) { return 0; }

        if (input > 255) { return 255;}

        return input;

    }



    /**

     * Clamps a pixel value between 0 and max

     * @param value The value to clamp

     * @param max The max the value can be

     * @return The clamped value

     */

    private static int wrapValue(int value, int max) {

        if (value < 0) {return 0;}

        if (value >= max) { return max - 1; }



        return value;

    }

}