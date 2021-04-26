package com.vzcatus13.encryptednotes.listeners;

import android.view.View;

import com.vzcatus13.encryptednotes.entities.Notebook;

public interface NotebookListener {

    void onNotebookClicked(Notebook notebook, View view, int position);
}
