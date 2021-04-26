package com.vzcatus13.encryptednotes.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.vzcatus13.encryptednotes.R;
import com.vzcatus13.encryptednotes.database.NotesDatabase;
import com.vzcatus13.encryptednotes.entities.Notebook;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupUtils {

    private final static String TAG = "BackupUtils";

    /**
     * Start backup of database
     * @param context Context of app
     * @return String, path to created backup
     */
    public static String backupDatabase(Context context) {
        // Get current database file
        File dbFile = context.getDatabasePath(NotesDatabase.DB_NAME);
        // Generate backup name
        String backupName = getBackupName();
        // Create file for backup directory
        File backupDir = new File(FileUtils.getAppExternalFileStorage(context).toString(), backupName);
        if (!backupDir.exists()) {
            // Create backup directory
            backupDir.mkdirs();
            // Copy backup file to backup directory
            File dbBackupFile = new File(backupDir.getAbsolutePath() + File.separator + "backup.db");
            FileUtils.copyPasteFileTo(dbFile, dbBackupFile);
            // Copy images to backup directory
            backupImages(context, backupDir);
        }
        return context.getString(R.string.app_name).replace(" ", "") + File.separator + backupName;
    }

    /**
     * Start backup of images
     * @param context Context of app
     * @param backupDir Destination, where images will be saved
     */
    public static void backupImages(Context context, File backupDir) {
        // Get images file
        File[] imagesFiles = FileUtils.getImagesFiles(context);
        if (imagesFiles != null && imagesFiles.length > 0) {
            // Create backup directory, if there is no
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            // Create file with images directory
            File imagesDir = new File(backupDir.getAbsoluteFile() + File.separator + "images");
            if (!imagesDir.exists()) {
                imagesDir.mkdir();
                // Copy each image to images directory
                for (File file : imagesFiles) {
                    File imageFile = new File(imagesDir.getAbsoluteFile() + File.separator + file.getName());
                    FileUtils.copyPasteFileTo(file, imageFile);
                }
            }
        }
    }

    /**
     * Restore database from given URI to .db file
     * @param context Context of app
     * @param uri URI to .db file
     * @return True if restored successfully
     */
    public static boolean restoreDatabase(Context context, Uri uri) throws IOException {
        if (isDatabaseFileValid(context.getContentResolver().openInputStream(uri))) {
            NotesDatabase notesDatabase = NotesDatabase.getDatabase(context);
            notesDatabase.close();

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            File databaseExisting = context.getDatabasePath(NotesDatabase.DB_NAME);
            // backup existing database
            File databaseBackup = new File(context.getFilesDir(), getBackupName() + ".db");
            FileUtils.copyPasteFileTo(databaseExisting, databaseBackup);
            // replace new database file with existing
            databaseExisting.delete();
            FileUtils.createFileFromInputStream(databaseExisting, inputStream);
            // check if database is valid
            if (!isDatabaseValid(context)) {
                notesDatabase.close();
                databaseExisting.delete();
                FileUtils.copyPasteFileTo(databaseBackup, databaseExisting);
                databaseBackup.delete();
                return false;
            }
            databaseBackup.delete();
            FileUtils.deleteImages(context);
            return true;
        }
        return false;
    }

    /**
     * Generate name for backup in format:
     * current: year, month, day, hour, minute, second (yyyy-MM-dd_HH:mm:ss)
     * @return String of backup name
     */
    private static String getBackupName() {
        String dateTimeString = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(new Date());
        return "backup_" + dateTimeString;
    }

    /**
     * Check if .db file is valid
     * @param inputStream InputStream with file in it
     * @return True if database file is valid
     */
    private static boolean isDatabaseFileValid(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[16];
        // Read first 16 bytes of file
        inputStream.read(buffer, 0, 16);
        byte[] check = "SQLite format 3\u0000".getBytes();
        return Arrays.equals(buffer, check);
    }

    /**
     * Check if database of app is valid
     * Database is valid if it has main notebook with id Notebook.MAIN_ID
     * @param context Context of app
     * @return True if database of app is valid
     */
    private static boolean isDatabaseValid(Context context) {
        NotesDatabase notesDatabase = NotesDatabase.getDatabase(context);
        AtomicBoolean isValid = new AtomicBoolean(false);
        Thread thread = new Thread(() -> { isValid.set(notesDatabase.notebookDao().getNotebookById(Notebook.MAIN_ID) != null); });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
        return isValid.get();
    }
}
