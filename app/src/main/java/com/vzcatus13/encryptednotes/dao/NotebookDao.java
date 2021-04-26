package com.vzcatus13.encryptednotes.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.vzcatus13.encryptednotes.entities.Notebook;

import java.util.List;

@Dao
public interface NotebookDao {

    /**
     * Get all notebooks
     * @return List of notebooks
     */
    @Query("SELECT * FROM notebooks ORDER BY id")
    List<Notebook> getAllNotebooks();

    /**
     * Get notebook by id
     * @param notebookId long id of notebook
     * @return Notebook instance
     */
    @Query("SELECT * FROM notebooks WHERE id = :notebookId LIMIT 1")
    Notebook getNotebookById(long notebookId);

    /**
     * Insert or replace existing notebook with new one
     * @param notebook Notebook to insert or replace for
     * @return long id entry of inserted or replaced notebook
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNotebook(Notebook notebook);

    /**
     * Delete notebook
     * @param notebook Notebook to be deleted
     */
    @Delete
    void deleteNotebook(Notebook notebook);
}
