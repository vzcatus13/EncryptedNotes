package com.vzcatus13.encryptednotes.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.vzcatus13.encryptednotes.converters.DateLongConverter;
import com.vzcatus13.encryptednotes.dao.NoteDao;
import com.vzcatus13.encryptednotes.dao.NotebookDao;
import com.vzcatus13.encryptednotes.entities.Note;
import com.vzcatus13.encryptednotes.entities.Notebook;
import com.vzcatus13.encryptednotes.utils.Utils;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@Database(entities = { Note.class, Notebook.class}, version = 3, exportSchema = false)
@TypeConverters( {DateLongConverter.class} )
public abstract class NotesDatabase extends RoomDatabase {

    public static final String DB_NAME = "notes_db";
    private static NotesDatabase notesDatabase;

    public abstract NoteDao noteDao();
    public abstract NotebookDao notebookDao();

    /**
     * Get NotesDatabase instance
     * @param context Context of app
     * @return NotesDatabase instance
     */
    public static synchronized NotesDatabase getDatabase(Context context) {
        if (notesDatabase == null) {
            RoomDatabase.Callback callback = new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    new Thread(() -> {
                        Notebook mainNotebook = new Notebook();
                        mainNotebook.setId(Notebook.MAIN_ID);
                        mainNotebook.setTitle(Utils.getRandomString(13));
                        mainNotebook.setCreatedAt(new Date());
                        notesDatabase.notebookDao().insertNotebook(mainNotebook);
                    }).start();
                }

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    new Thread(() -> {
                        Notebook notebook = notesDatabase.notebookDao().getNotebookById(-1);
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (notebook == null) {
                                    Notebook mainNotebook = new Notebook();
                                    mainNotebook.setId(Notebook.MAIN_ID);
                                    mainNotebook.setTitle(Utils.getRandomString(13));
                                    mainNotebook.setCreatedAt(new Date());
                                    notesDatabase.notebookDao().insertNotebook(mainNotebook);
                                }
                            }
                        }, 500);
                    }).start();
                }
            };

            notesDatabase = Room.databaseBuilder(
                    context,
                    NotesDatabase.class,
                    DB_NAME
            )
                    .addCallback(callback)
                    .setJournalMode(JournalMode.TRUNCATE)
                    .build();
        }
        return notesDatabase;
    }
}
