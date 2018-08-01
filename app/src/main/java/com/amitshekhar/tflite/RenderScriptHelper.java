package com.amitshekhar.tflite;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicColorMatrix;
import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Size;

import com.example.yuv2rgbrenderscript.ScriptC_Yuv2Rgb;


final class RenderScriptHelper {

    // Convert to RGB using Intrinsic render script
    public static Bitmap convertYuvToRgbIntrinsic(RenderScript rs, byte[] data, Size imageSize) {

        int imageWidth = imageSize.getWidth();
        int imageHeight = imageSize.getHeight() ;

        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(rs));

        // Create the input allocation  memory for Renderscript to work with
        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(imageWidth).setY(imageHeight);

        Allocation aIn = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
        // Set the YUV frame data into the input allocation
        aIn.copyFrom(data);


        // Create the output allocation
        Type.Builder rgbType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(imageWidth).setY(imageHeight);

        Allocation aOut = Allocation.createTyped(rs, rgbType.create(), Allocation.USAGE_SCRIPT);



        yuvToRgbIntrinsic.setInput(aIn);
        // Run the script for every pixel on the input allocation and put the result in aOut
        yuvToRgbIntrinsic.forEach(aOut);

        // Create an output bitmap and copy the result into that bitmap
        Bitmap outBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        aOut.copyTo(outBitmap);

        return outBitmap ;

    }



    // Convert to RGB using render script - with Yuv2Rgb.rs
    public static Bitmap convertYuvToRgb(RenderScript rs,byte[] data, Size imageSize) {

        int imageWidth = imageSize.getWidth();
        int imageHeight = imageSize.getHeight() ;

        // Input allocation
        Type.Builder yuvType = new Type.Builder(rs, Element.createPixel(rs, Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV))
                .setX(imageWidth)
                .setY(imageHeight)
                .setMipmaps(false)
                .setYuvFormat(ImageFormat.YUV_420_888);
        Allocation ain = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
        ain.copyFrom(data);


        // output allocation
        Type.Builder rgbType = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(imageWidth)
                .setY(imageHeight)
                .setMipmaps(false);

        Allocation aOut = Allocation.createTyped(rs, rgbType.create(), Allocation.USAGE_SCRIPT);


        // Create the script
        ScriptC_Yuv2Rgb yuvScript = new ScriptC_Yuv2Rgb(rs);
        // Bind to script level -  set the allocation input and parameters from the java into the script level (thru JNI)
        yuvScript.set_gIn(ain);
        yuvScript.set_width(imageWidth);
        yuvScript.set_height(imageHeight);

        // invoke the script conversion method
        yuvScript.forEach_yuvToRgb(ain, aOut);

        Bitmap outBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        aOut.copyTo(outBitmap) ;

        return outBitmap ;

    }


    public static Bitmap resizeBitmap2(RenderScript rs, Bitmap src, int dstWidth) {
        Bitmap.Config  bitmapConfig = src.getConfig();
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        float srcAspectRatio = (float) srcWidth / srcHeight;
        int dstHeight = (int) (dstWidth / srcAspectRatio);
        dstHeight = dstWidth;

        float resizeRatio = (float) srcWidth / dstWidth;

        /* Calculate gaussian's radius */
        float sigma = resizeRatio / (float) Math.PI;
        // https://android.googlesource.com/platform/frameworks/rs/+/master/cpu_ref/rsCpuIntrinsicBlur.cpp
        float radius = 2.5f * sigma - 1.5f;
        radius = Math.min(25, Math.max(0.0001f, radius));

        /* Gaussian filter */
        Allocation tmpIn = Allocation.createFromBitmap(rs, src);
        Allocation tmpFiltered = Allocation.createTyped(rs, tmpIn.getType());
        ScriptIntrinsicBlur blurInstrinsic = ScriptIntrinsicBlur.create(rs, tmpIn.getElement());

        blurInstrinsic.setRadius(radius);
        blurInstrinsic.setInput(tmpIn);
        blurInstrinsic.forEach(tmpFiltered);

        tmpIn.destroy();
        blurInstrinsic.destroy();

        /* Resize */
        Bitmap dst = Bitmap.createBitmap(dstWidth, dstHeight, bitmapConfig);
        Type t = Type.createXY(rs, tmpFiltered.getElement(), dstWidth, dstHeight);
        Allocation tmpOut = Allocation.createTyped(rs, t);
        ScriptIntrinsicResize resizeIntrinsic = ScriptIntrinsicResize.create(rs);

        resizeIntrinsic.setInput(tmpFiltered);
        resizeIntrinsic.forEach_bicubic(tmpOut);
        tmpOut.copyTo(dst);

        tmpFiltered.destroy();
        tmpOut.destroy();
        resizeIntrinsic.destroy();

        return dst;
    }




    public static Bitmap applyBlurEffectIntrinsic(RenderScript rs,Bitmap inBitmap, Size imageSize) {

        Bitmap outBitmap = inBitmap.copy(inBitmap.getConfig(), true);

        Allocation aIn = Allocation.createFromBitmap(rs, inBitmap,Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation aOut = Allocation.createTyped(rs, aIn.getType());

        //Blur the image
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        // Set the blur radius
        script.setRadius(15f);
        script.setInput(aIn);
        script.forEach(aOut);
        aOut.copyTo(outBitmap);

        return outBitmap ;
    }



    public static Bitmap applyGrayScaleEffectIntrinsic(RenderScript rs,Bitmap inBitmap, Size imageSize) {

        Bitmap grayBitmap = inBitmap.copy(inBitmap.getConfig(), true);

        Allocation aIn = Allocation.createFromBitmap(rs, inBitmap,Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation aOut = Allocation.createTyped(rs, aIn.getType());

        //Make the image grey scale
        final ScriptIntrinsicColorMatrix scriptColor = ScriptIntrinsicColorMatrix.create(rs, Element.U8_4(rs));
        scriptColor.setGreyscale();
        scriptColor.forEach(aIn, aOut);
        aOut.copyTo(grayBitmap);

        return grayBitmap ;

    }


}