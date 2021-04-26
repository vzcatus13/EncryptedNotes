package com.vzcatus13.encryptednotes.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.vzcatus13.encryptednotes.entities.Note;

import java.util.List;

@Dao
public interface NoteDao {

    /**
     * Get all notes as list
     * @return List of notes
     */
    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<Note> getAllNotes();

    /**
     * Insert or replace existing note with new note
     * @param note Note to insert or replace
     * @return long inserted id in DB
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNote(Note note);

    /**
     * Delete note from DB
     * @param note Note to be deleted
     */
    @Delete
    void deleteNote(Note note);

    /**
     * Find notes by search query
     * @param query Search query
     * @return List of notes
     */
    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :query || '%' AND isEncrypted = 1)" +
            " OR (note_text LIKE '%' || :query || '%' AND isEncrypted = 0) " +
            " ORDER BY id DESC")

    List<Note> searchNotes(String query);

    /**
     * Get notes by theirs notebook id
     * @param id Long id of notebook
     * @return List of notes
     */
    @Query("SELECT * FROM notes WHERE notebookId = :id ORDER BY id DESC")
    List<Note> getNotesByNotebookId(long id);
}
