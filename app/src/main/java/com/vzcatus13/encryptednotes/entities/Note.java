package com.vzcatus13.encryptednotes.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "notes",
        foreignKeys = {
            @ForeignKey(
                    entity = Notebook.class,
                    parentColumns = "id",
                    childColumns = "notebookId",
                    onDelete = ForeignKey.SET_DEFAULT
            )})
public class Note implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "notebookId", defaultValue = "" + Notebook.MAIN_ID)
    private long notebookId = -1;

    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @ColumnInfo(name = "modified_at")
    private Date modifiedAt;

    @ColumnInfo(name = "note_text", typeAffinity = ColumnInfo.BLOB)
    private byte[] noteText;

    @ColumnInfo(name = "image_path")
    private String imagePath;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "isEncrypted")
    private boolean isEncrypted;

    @ColumnInfo(name = "dummyData", typeAffinity = ColumnInfo.BLOB)
    private byte[] dummyData;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public byte[] getNoteText() {
        return noteText;
    }

    public void setNoteText(byte[] noteText) {
        this.noteText = noteText;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setEncrypted(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public byte[] getDummyData() {
        return dummyData;
    }

    public void setDummyData(byte[] dummyData) {
        this.dummyData = dummyData;
    }

    public long getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(long notebookId) {
        this.notebookId = notebookId;
    }

    @Override
    public String toString() {
        return title + ": " + createdAt;
    }
}
