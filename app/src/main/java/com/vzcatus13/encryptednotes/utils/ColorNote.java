package com.vzcatus13.encryptednotes.utils;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

import com.vzcatus13.encryptednotes.R;

public enum ColorNote {

    DEFAULT(R.string.default_color,"#ffffff", "#202125"),
    LIGHT_PINK(R.string.light_pink_color,"#ff9999", "#5b2b29"),
    PEACH_ORANGE(R.string.peach_orange_color,"#ffd6a5", "#60491d"),
    LEMON_YELLOW(R.string.lemon_yellow_color,"#fdffb6", "#625c1e"),
    TEA_GREEN(R.string.tea_green_color,"#caffbf", "#355822"),
    SKY_BLUE(R.string.sky_blue_color_color,"#9bf6ff", "#2f565d"),
    BABY_BLUE(R.string.baby_blue_color,"#a0c4ff", "#1f3c5e"),
    LAVENDER_PURPLE(R.string.lavender_purple_color,"#bdb2ff", "#41295d"),
    PURPLE_PINK(R.string.purple_pink_color,"#ffc6ff", "#5a2245");

    private final int friendlyNameStringResId;
    private final String hexValueLight;
    private final String hexValueDark;

    /**
     * Initialize new color for note
     * @param friendlyNameStringResId Int of human-readable string resource for color
     * @param hexValueLight String of light type of color in HEX
     * @param hexValueDark String of dark type of color in HEX
     */
    ColorNote(final int friendlyNameStringResId, final String hexValueLight, final String hexValueDark) {
        this.friendlyNameStringResId = friendlyNameStringResId;
        this.hexValueLight = hexValueLight;
        this.hexValueDark = hexValueDark;
    }

    /**
     * Get String in HEX depending on night mode
     * @param context Context of app
     * @return Color String in HEX
     */
    public String getHex(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_NO ? hexValueLight : hexValueDark;
    }

    /**
     * Get color as int depending on night mode
     * @param context Context of app
     * @return Color as int
     */
    public int getColor(Context context) {
        return Color.parseColor(this.getHex(context));
    }

    /**
     * Get ColorNote by its enum name
     * @param value Enum name of ColorNote
     * @return ColorNote enum
     */
    public static ColorNote toColorNote(String value) {
        try {
            return ColorNote.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get human-readable name of color
     * @param context Context of app
     * @return String of human-readable name of color
     */
    public String getFriendlyName(Context context) {
        return context.getString(friendlyNameStringResId);
    }

    /**
     * Get enum name as String
     * @return String of color name
     */
    @Override
    public String toString() {
        return this.name();
    }
}
