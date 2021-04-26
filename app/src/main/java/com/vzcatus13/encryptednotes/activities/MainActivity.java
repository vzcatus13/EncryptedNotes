package com.vzcatus13.encryptednotes.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DimenRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import com.vzcatus13.encryptednotes.R;
import com.vzcatus13.encryptednotes.adapters.NotebooksAdapter;
import com.vzcatus13.encryptednotes.adapters.NotesAdapter;
import com.vzcatus13.encryptednotes.database.NotesDatabase;
import com.vzcatus13.encryptednotes.entities.Note;
import com.vzcatus13.encryptednotes.entities.Notebook;
import com.vzcatus13.encryptednotes.listeners.NoteListener;
import com.vzcatus13.encryptednotes.listeners.NotebookListener;
import com.vzcatus13.encryptednotes.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements NoteListener, NotebookListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_SHOW_ALL_NOTES = 3;
    public static final int REQUEST_CODE_SHOW_SELECTED_NOTES = 5;

    public static final String TAG = "MainActivity";

    private BottomAppBar bottomAppBar;
    private AppBarLayout notebookAppBar;
    private LinearLayout notebookBarLayout;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private RecyclerView notebooksRecyclerView;
    private List<Notebook> notebooksList;
    private NotebooksAdapter notebooksAdapter;

    private int noteClickedPosition = -1;
    private long notebookSelectedId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        initPreferenceChangeListener();

        bottomAppBar = findViewById(R.id.app_bar);
        notesRecyclerView = findViewById(R.id.notes_recycler_view);
        FloatingActionButton floatingActionButtonAddNote = findViewById(R.id.add_note);
        notebookBarLayout = findViewById(R.id.notebook_bar);
        notebookAppBar = findViewById(R.id.notebook_app_bar);
        notebooksRecyclerView = findViewById(R.id.notebooks_recycler_view);

        Utils.setTransparentStatusAndNavigation(getWindow(), true);
        // Change night mode to user's preference
        nightModeSwitch();


        setNotebookBarTopPadding(R.dimen._2sdp);
        setNotesRecyclerViewPadding(R.dimen._10sdp, R.dimen._5sdp);

        noteList = new ArrayList<>();
        setNotesRecyclerView();

        notebooksList = new ArrayList<>();
        setNotebooksRecyclerView();

        // Create request for adding new note
        ActivityResultLauncher<Intent> requestAddNote = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    switch (result.getResultCode()) {
                        case RESULT_OK:
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    getNotes(REQUEST_CODE_ADD_NOTE, false);
                                }
                            }, 500);
                            break;
                        case CreateNoteActivity.NOTE_DISCARDED:
                            Snackbar.make(floatingActionButtonAddNote, R.string.empty_note_discarded, Snackbar.LENGTH_SHORT)
                                    .setAnchorView(floatingActionButtonAddNote)
                                    .setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorText))
                                    .show();
                            break;
                    }
                }
        );

        // Set add new note listener
        floatingActionButtonAddNote.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
            intent.putExtra("notebookId", notebookSelectedId);
            requestAddNote.launch(intent);
        });

        // Create request for searching notes
        ActivityResultLauncher<Intent> requestSearchNote = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (result.getData() != null) {
                            if (result.getData().getBooleanExtra("isAnyChange", false)) {
                                if (notebookSelectedId == -1) {
                                    getNotes(REQUEST_CODE_SHOW_SELECTED_NOTES, false);
                                } else {
                                    getNotes(REQUEST_CODE_SHOW_ALL_NOTES, false);
                                }
                            }
                        }
                    }
                }
        );

        // Set listener to show navigation menu
        bottomAppBar.setNavigationOnClickListener(view -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(
                    MainActivity.this, R.style.AppTheme_BottomSheetDialogTheme_Default
            );
            View bottomSheetView = LayoutInflater.from(MainActivity.this)
                    .inflate(
                            R.layout.bottom_sheet_main_navigation_menu,
                            findViewById(R.id.navigation_menu_bottom_sheet)
                    );

            bottomSheetView.findViewById(R.id.search).setOnClickListener( v -> {
                Intent intent = new Intent(getApplicationContext(), SearchNoteActivity.class);
                bottomSheetDialog.cancel();
                requestSearchNote.launch(intent);
            });

            bottomSheetView.findViewById(R.id.settings).setOnClickListener( v -> {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                bottomSheetDialog.cancel();
                startActivity(intent);
            });

            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();
        });

        getNotes(REQUEST_CODE_SHOW_ALL_NOTES, false);
        getNotebooks();
    }

    private void setNotebookBarTopPadding(@DimenRes int id) {
        int statusBarId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        notebookBarLayout.setPadding(
                notebookBarLayout.getPaddingLeft(),
                notebookBarLayout.getPaddingTop() + getResources().getDimensionPixelSize(statusBarId) + getResources().getDimensionPixelSize(id),
                notebookBarLayout.getPaddingRight(),
                notebookBarLayout.getPaddingBottom()
        );
    }

    private void setNotesRecyclerViewPadding(@DimenRes int idTop, @DimenRes int idBottom) {
        notesRecyclerView.setPadding(
                notesRecyclerView.getPaddingLeft(),
                getResources().getDimensionPixelOffset(idTop),
                notesRecyclerView.getPaddingRight(),
                notesRecyclerView.getPaddingBottom()
        );

        bottomAppBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bottomAppBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                notesRecyclerView.setPadding(
                        notesRecyclerView.getPaddingLeft(),
                        notesRecyclerView.getPaddingTop(),
                        notesRecyclerView.getPaddingRight(),
                        bottomAppBar.getHeight() + getResources().getDimensionPixelSize(idBottom)
                );
            }
        });
    }

    private void getNotes(int requestCode, final boolean isNoteDeleted) {
        Observable.fromCallable(() -> {
            List<Note> notes;
            if (requestCode == REQUEST_CODE_SHOW_SELECTED_NOTES && notebookSelectedId != -1) {
                notes = NotesDatabase.getDatabase(getApplicationContext())
                        .noteDao().getNotesByNotebookId(notebookSelectedId);
            } else {
                notes = NotesDatabase.getDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
            }
            return notes;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Note>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull List<Note> notes) {
                        switch (requestCode) {
                            case REQUEST_CODE_SHOW_ALL_NOTES:
                            case REQUEST_CODE_SHOW_SELECTED_NOTES:
                                noteList.clear();
                                noteList.addAll(notes);
                                notesRecyclerViewChanged();
                                notesAdapter.notifyDataSetChanged();
                                break;
                            case REQUEST_CODE_ADD_NOTE:
                                noteList.add(0, notes.get(0));
                                notesRecyclerViewChanged();
                                notesAdapter.notifyItemInserted(0);
                                notesRecyclerView.smoothScrollToPosition(0);
                                notebookAppBar.setExpanded(true);
                                break;
                            case NotesAdapter.REQUEST_CODE_UPDATE_NOTE:
                                noteList.remove(noteClickedPosition);
                                notesRecyclerViewChanged();

                                if (isNoteDeleted) {
                                    notesAdapter.notifyItemRemoved(noteClickedPosition);
                                } else {
                                    noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                                    notesAdapter.notifyItemChanged(noteClickedPosition);
                                }
                                break;
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void onNoteClicked(Note note, View view, int position) {
        noteClickedPosition = position;
        notesAdapter.openNote(note);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NotesAdapter.REQUEST_CODE_UPDATE_NOTE) {
            switch (resultCode) {
                case RESULT_OK:
                    if (data != null) {
                        getNotes(NotesAdapter.REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
                    }
                    break;
                case CreateNoteActivity.NOTE_UNCHANGED:
                    break;
            }
        }
    }

    /**
     * Set recycler view layout to user's preference
     */
    private void setNotesRecyclerView() {
        boolean isGrid = sharedPreferences.getBoolean(SettingsActivity.KEY_VIEW_GRID, true);
        if (isGrid) {
            notesRecyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            );
        } else {
            notesRecyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            );
        }

        notesAdapter = new NotesAdapter(noteList, this, MainActivity.this, notesRecyclerView, isGrid);
        notesRecyclerView.setAdapter(notesAdapter);
    }

    /**
     * Called when notes recycler view changes (e.g. something added or removed)
     * Need to be called to set notebookBarLayout scroll flags, which are dependent on visible notes
     */
    private void notesRecyclerViewChanged() {
        // Set observer to wait until recycler view completely laid out
        notesRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                notesRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) notesRecyclerView.getLayoutManager();
                int spanCount = staggeredGridLayoutManager.getSpanCount();
                // Indexes of first visible notes
                int[] firstVisibleItems = staggeredGridLayoutManager.findFirstCompletelyVisibleItemPositions(null);
                // Indexes of last visible notes
                int[] lastVisibleItems = staggeredGridLayoutManager.findLastCompletelyVisibleItemPositions(null);
                int firstVisibleItem = -1;
                int lastVisibleItem = -1;

                // If notes shown as one column list
                if (spanCount == 1) {
                    firstVisibleItem = firstVisibleItems[0];
                    lastVisibleItem = lastVisibleItems[0];
                } else if (spanCount == 2) {
                    if (firstVisibleItems[0] == -1 || firstVisibleItems[1] == -1) {
                        firstVisibleItem = Math.max(firstVisibleItems[0], firstVisibleItems[1]);
                    } else {
                        firstVisibleItem = Math.min(firstVisibleItems[0], firstVisibleItems[1]);
                    }

                    lastVisibleItem = Math.max(lastVisibleItems[0], lastVisibleItems[1]);
                }

                AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) notebookBarLayout.getLayoutParams();

                // If there's any visible note
                if (firstVisibleItem != -1 || lastVisibleItem != -1) {
                    // If first visible note is start of note list
                    // And if last visible note is end of note list
                    // So notesRecyclerView can't scroll => then notebookBar also can't scroll
                    if (firstVisibleItem == 0 && lastVisibleItem == noteList.size() - 1) {
                        layoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL);
                    } else {
                        layoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
                    }
                } else {
                    layoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL);
                }
                notebookBarLayout.setLayoutParams(layoutParams);
            }
        });
    }

    private void setNotebooksRecyclerView() {
        notebooksRecyclerView.setLayoutManager(
                new LinearLayoutManager(MainActivity.this, RecyclerView.HORIZONTAL, false)
        );
        notebooksAdapter = new NotebooksAdapter(notebooksList, this, MainActivity.this);
        notebooksRecyclerView.setAdapter(notebooksAdapter);
    }

    /**
     * Called to change night mode depending on user preference
     */
    private void nightModeSwitch() {
        String mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mode = sharedPreferences.getString(SettingsActivity.KEY_THEME, SettingsActivity.THEME_AUTO);
        } else {
            mode = sharedPreferences.getString(SettingsActivity.KEY_THEME, SettingsActivity.THEME_LIGHT);
        }

        switch (mode) {
            case SettingsActivity.THEME_AUTO:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case SettingsActivity.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SettingsActivity.THEME_NIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    private void initPreferenceChangeListener() {
        if (onSharedPreferenceChangeListener == null) {
            onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
                if (key.equalsIgnoreCase(SettingsActivity.KEY_VIEW_GRID)) {
                    setNotesRecyclerView();
                }
            };

            sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        }
    }

    @Override
    public void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState, @androidx.annotation.NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt("noteClickedPosition", noteClickedPosition);
    }

    @Override
    protected void onRestoreInstanceState(@androidx.annotation.NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        noteClickedPosition = savedInstanceState.getInt("noteClickedPosition");
    }

    @Override
    public void onNotebookClicked(Notebook notebook, View view, int position) {
        notebookSelectedId = notebook.getId();
        getNotes(REQUEST_CODE_SHOW_SELECTED_NOTES, false);
        notebooksAdapter.setSelectedPosition(position);
        notebooksAdapter.notifyDataSetChanged();
        notebooksRecyclerView.smoothScrollToPosition(position);
    }

    private void getNotebooks() {
        Observable.fromCallable( () -> {
            List<Notebook> notebooks;
            notebooks = NotesDatabase.getDatabase(getApplicationContext()).notebookDao().getAllNotebooks();
            return notebooks;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Notebook>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull List<Notebook> notebooks) {
                        notebooksList.clear();
                        notebooksList.addAll(notebooks);
                        notesRecyclerViewChanged();
                        notebooksAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}