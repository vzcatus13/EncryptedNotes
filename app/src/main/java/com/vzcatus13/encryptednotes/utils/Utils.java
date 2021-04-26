package com.vzcatus13.encryptednotes.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.ColorUtils;

import com.vzcatus13.encryptednotes.R;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    private final static int SYSTEM_UI_SCRIM_DARK = Color.parseColor("#40000000");
    private final static int SYSTEM_UI_SCRIM_LIGHT = Color.parseColor("#35000000");
    private final static String CHARACTERS_FOR_RANDOM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Makes transparent status and navigation bars of Window
     * @param window Window of Activity or View
     * @param isEdgeToEdge Is Window placed edge-to-edge
     */
    public static void setTransparentStatusAndNavigation(Window window, boolean isEdgeToEdge) {
        int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
        // Get night mode flags of window
        int nightModeFlags = window.getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Set status and navigation bars color depend on night mode flag
        int statusBarColor = nightModeFlags == Configuration.UI_MODE_NIGHT_NO ? SYSTEM_UI_SCRIM_LIGHT : SYSTEM_UI_SCRIM_DARK;
        int navigationBarColor = nightModeFlags == Configuration.UI_MODE_NIGHT_NO ? SYSTEM_UI_SCRIM_LIGHT : SYSTEM_UI_SCRIM_DARK;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
                // Set icons of status bar to dark
                systemUiVisibility = systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            statusBarColor = Color.TRANSPARENT;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
                // Set buttons of navigation bar to dark
                systemUiVisibility = systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            navigationBarColor = Color.TRANSPARENT;
        }

        if (isEdgeToEdge) {
            systemUiVisibility = systemUiVisibility | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        }

        window.setStatusBarColor(statusBarColor);
        window.setNavigationBarColor(navigationBarColor);
        window.getDecorView().setSystemUiVisibility(systemUiVisibility);
    }

    /**
     * Makes transparent status and navigation bars
     * if applyToStatusBar and applyToNavigationBar are false.
     * @param window Window of Activity or View
     * @param isEdgeToEdge Is Window placed edge-to-edge
     * @param customColor Custom color of status and navigation bars
     * @param applyToStatusBar Is custom color need to be applied to status bar
     * @param applyToNavigationBar Is custom color need to be applied to navigation bar
     */
    public static void setTransparentStatusAndNavigation(Window window, boolean isEdgeToEdge, int customColor, boolean applyToStatusBar, boolean applyToNavigationBar) {
        int nightModeFlags = window.getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setTransparentStatusAndNavigation(window, isEdgeToEdge);
        }

        // If API supports light or dark icons, buttons of status and navigation bars
        // then custom color can be applied
        // Otherwise, blend custom color with light or dark scrim depending on night mode
        if (applyToStatusBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(customColor);
        } else {
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
                window.setStatusBarColor(ColorUtils.blendARGB(customColor, SYSTEM_UI_SCRIM_LIGHT, 0.35f));
            } else {
                window.setStatusBarColor(ColorUtils.blendARGB(customColor, SYSTEM_UI_SCRIM_DARK, 0.35f));
            }
        }

        // Algorithm is the same as above in status bar
        if (applyToNavigationBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setNavigationBarColor(customColor);
        } else {
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
                window.setNavigationBarColor(ColorUtils.blendARGB(customColor, SYSTEM_UI_SCRIM_LIGHT, 0.35f));
            } else {
                window.setNavigationBarColor(ColorUtils.blendARGB(customColor, SYSTEM_UI_SCRIM_DARK, 0.35f));
            }
        }
    }

    /**
     * Get random sting of characters and numbers
     * @param length Length of generated string
     * @return Random generated string with characters and numbers
     * @throws IllegalArgumentException If length is lower than one
     */
    public static String getRandomString(int length) {
        SecureRandom secureRandom = new SecureRandom();
        if (length <= 0) {
            throw new IllegalArgumentException("String length must be greater than zero");
        }

        StringBuilder randomString = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            randomString.append(CHARACTERS_FOR_RANDOM.charAt(secureRandom.nextInt(CHARACTERS_FOR_RANDOM.length())));
        }

        return randomString.toString();
    }

    /**
     * Get random byte array of data
     * @param length Length of generated array
     * @return Byte array with random bytes
     */
    public static byte[] getRandomBytes(int length) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    /**
     * Converts byte array to char array
     * @param bytes Byte array to be converted
     * @return Converted char array in UTF-8 charset
     */
    public static char[] bytesToChars(byte[] bytes) {
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
        char[] array = new char[charBuffer.limit()];
        charBuffer.get(array);
        return array;
    }

    /**
     * Converts char array to byte array
     * @param chars Char array to be converted
     * @return Converted byte array, where chars are encoded in UTF-8 charset
     */
    public static byte[] charsToBytes(char[] chars) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] array = new byte[byteBuffer.limit()];
        byteBuffer.get(array);
        return array;
    }

    /**
     * Blends color with black or white color depending on night mode
     * @param context Context of app
     * @param color Color to be blended
     * @param ratio Ratio of black, white color to inputed color
     * @return Integer of blended color
     */
    public static int blendColorLightOrDarkMode(Context context, int color, float ratio) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int blendColor = nightModeFlags == Configuration.UI_MODE_NIGHT_NO ? Color.BLACK : Color.WHITE;
        return ColorUtils.blendARGB(color, blendColor, ratio);
    }

    /**
     * Converts Date object to human-readable String
     * @param context Context of app
     * @param date Date object to be converted
     * @return String converted from Date
     */
    public static String dateToString(Context context, Date date) {
        Calendar calendarDate = Calendar.getInstance();
        calendarDate.setTime(date);
        Date now = new Date();
        Calendar calendarNow = Calendar.getInstance();
        calendarNow.setTime(now);
        Locale locale = Locale.getDefault();
        boolean is24HourFormat = DateFormat.is24HourFormat(context);
        SimpleDateFormat df;
        String formattedDate;
        if (calendarNow.get(Calendar.YEAR) == calendarDate.get(Calendar.YEAR)) {
            // If date is today
            if (calendarNow.get(Calendar.DAY_OF_YEAR) - calendarDate.get(Calendar.DAY_OF_YEAR) == 0) {
                if (is24HourFormat) {
                    df = new SimpleDateFormat("kk:mm", locale);
                } else {
                    df = new SimpleDateFormat("hh:mm a", locale);
                }
                df.setTimeZone(TimeZone.getDefault());
                formattedDate = context.getString(R.string.today) + ", " +
                        df.format(date);
            }
            // If date was yesterday
            else if (calendarNow.get(Calendar.DAY_OF_YEAR) - calendarDate.get(Calendar.DAY_OF_YEAR) == 1) {
                if (is24HourFormat) {
                    df = new SimpleDateFormat("kk:mm", locale);
                } else {
                    df = new SimpleDateFormat("hh:mm a", locale);
                }
                df.setTimeZone(TimeZone.getDefault());
                formattedDate = context.getString(R.string.yesterday) + ", " +
                        df.format(date);
            } else {
                if (is24HourFormat) {
                    df = new SimpleDateFormat("dd MMM kk:mm", locale);
                } else {
                    df = new SimpleDateFormat("dd MMM hh:mm a", locale);
                }
                df.setTimeZone(TimeZone.getDefault());
                formattedDate = df.format(date);
            }
        } else {
            df = new SimpleDateFormat("dd MMM yyyy", locale);
            df.setTimeZone(TimeZone.getDefault());
            formattedDate = df.format(date);
        }
        return formattedDate;
    }
}
