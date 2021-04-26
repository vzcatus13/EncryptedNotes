package com.vzcatus13.encryptednotes.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.vzcatus13.encryptednotes.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    private final static String TAG = "FileUtils";

    private final static int bufferSize = 10240;

    /**
     * Get destination, where app can store data outside it
     * @param context Context of app
     * @return File with destination outside app-specific storage
     */
    public static File getAppExternalFileStorage(Context context) {
        return new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name).replace(" ", ""));
    }

    /**
     * Get File, where images are stored within app-specific storage
     * @param context Context of app
     * @return File of images
     */
    public static File getImagesFileStorage(Context context) {
        return new File(context.getFilesDir() + File.separator + "images");
    }

    /**
     * Get File array with all images stored on app-specific storage
     * @param context Context of app
     * @return File array with all images
     */
    public static File[] getImagesFiles(Context context) {
        return getImagesFileStorage(context).listFiles();
    }

    /**
     * Delete all images on app-specific storage
     * @param context Context of app
     * @return True if all images are deleted
     */
    public static boolean deleteImages(Context context) {
        boolean success = true;
        File[] files = getImagesFiles(context);
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (!success) {
                    break;
                }
                success = file.delete();
            }
        }
        success = getImagesFileStorage(context).delete();
        return success;
    }

    /**
     * Copy pastes file from one directory to another
     * @param from File, which need to be copied
     * @param to Destination, where file will be pasted
     * @return True if file was successfully copied
     */
    public static boolean copyPasteFileTo(File from, File to) {
        try {
            if (to.createNewFile()) {
                byte[] buffer = new byte[bufferSize];
                int bytesToRead;
                OutputStream outputStream = new FileOutputStream(to);
                InputStream inputStream = new FileInputStream(from);
                while ((bytesToRead = inputStream.read(buffer, 0, bufferSize)) > 0) {
                    outputStream.write(buffer, 0, bytesToRead);
                }
                outputStream.flush();
                inputStream.close();
                outputStream.close();
                return true;
            }
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return false;
    }

    /**
     * Creates file from InputStream
     * @param to File, in which InputStream will be written
     * @param inputStream InputStream, from which file will be created
     * @return True if file was successfully created
     */
    public static boolean createFileFromInputStream(File to, InputStream inputStream) {
        try {
            if (to.createNewFile()) {
                byte[] buffer = new byte[bufferSize];
                int bytesToRead;
                OutputStream outputStream = new FileOutputStream(to);
                while ((bytesToRead = inputStream.read(buffer, 0, bufferSize)) > 0) {
                    outputStream.write(buffer, 0, bytesToRead);
                }
                outputStream.flush();
                outputStream.close();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * Save image to app's storage
     * @param context Context of app
     * @param bitmap Bitmap of image
     * @return String of absolute path to saved image
     * @throws IOException
     */
    public static String saveImageToStorage(Context context, Bitmap bitmap) throws IOException {
        String stringImagesDir = context.getFilesDir().getAbsolutePath() + File.separator + "images";
        File fileImagesDir = new File(stringImagesDir);
        if (!fileImagesDir.exists()) {
            fileImagesDir.mkdir();
        }

        File imagePath = new File(fileImagesDir, Utils.getRandomString(13) + ".jpeg");
        while (imagePath.exists()) {
            imagePath = new File(fileImagesDir, Utils.getRandomString(13) + ".jpeg");
        }

        FileOutputStream fileOutputStream = new FileOutputStream(imagePath);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();

        return imagePath.getAbsolutePath();

        // TODO: add encryption for images
        //  operate bitmap as byte array BitmapFactory.decodeByteArray
    }
}
