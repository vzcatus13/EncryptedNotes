package com.vzcatus13.encryptednotes.listeners;

import android.view.View;

import com.vzcatus13.encryptednotes.entities.Note;

public interface NoteListener {

    void onNoteClicked(Note note, View view, int position);
}
