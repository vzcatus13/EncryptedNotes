package com.vzcatus13.encryptednotes.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vzcatus13.encryptednotes.R;
import com.vzcatus13.encryptednotes.adapters.NotesAdapter;
import com.vzcatus13.encryptednotes.database.NotesDatabase;
import com.vzcatus13.encryptednotes.entities.Note;
import com.vzcatus13.encryptednotes.listeners.NoteListener;
import com.vzcatus13.encryptednotes.utils.Utils;

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

public class SearchNoteActivity extends AppCompatActivity implements NoteListener {

    private static final int REQUEST_CODE_SEARCH_NOTE = 3;

    public static final String TAG = "SearchNoteActivity";

    private SharedPreferences sharedPreferences;

    private RecyclerView notesSearchRecyclerView;
    private EditText searchInput;

    private ImageView iconInfo;
    private TextView textViewInfo;

    private List<Note> noteList;
    private NotesAdapter notesAdapter;


    private int noteClickedPosition = -1;
    private boolean isAnyChange = false;

    private String lastQuery;
    private Timer searchTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_note);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SearchNoteActivity.this);

        notesSearchRecyclerView = findViewById(R.id.notes_search_recycler_view);
        searchInput = findViewById(R.id.search_bar_input);


        initSearch();
        Utils.setTransparentStatusAndNavigation(getWindow(), true);
        initSearchView();
    }

    private void initSearch() {
        noteList = new ArrayList<>();
        boolean isGrid = sharedPreferences.getBoolean(SettingsActivity.KEY_VIEW_GRID, true);
        if (isGrid) {
            notesSearchRecyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            );
        } else {
            notesSearchRecyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            );
        }
        notesAdapter = new NotesAdapter(noteList, this, SearchNoteActivity.this, notesSearchRecyclerView, isGrid);
        notesSearchRecyclerView.setAdapter(notesAdapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cancelSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
                lastQuery = s.toString().toLowerCase().trim();
                if (!lastQuery.isEmpty()) {
                    searchNote(lastQuery, REQUEST_CODE_SEARCH_NOTE, false);
                } else {
                    noteList.clear();
                    notesAdapter.notifyDataSetChanged();
                    iconInfo.setVisibility(View.VISIBLE);
                    textViewInfo.setVisibility(View.VISIBLE);
                    textViewInfo.setText(R.string.search_your_notes);
                }
            }
        });

    }

    private void initSearchView() {
        LinearLayout searchBarLayout = findViewById(R.id.search_bar_layout);

        findViewById(R.id.search_back).setOnClickListener(v -> onBackPressed());

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setColor(ContextCompat.getColor(SearchNoteActivity.this, R.color.colorPrimary));
        gradientDrawable.setStroke(getResources().getDimensionPixelOffset(R.dimen._1sdp), ContextCompat.getColor(SearchNoteActivity.this, R.color.colorPrimaryDark));
        InsetDrawable insetDrawable = new InsetDrawable(gradientDrawable,
                getResources().getDimensionPixelOffset(R.dimen._minus1sdp),
                getResources().getDimensionPixelOffset(R.dimen._minus1sdp),
                getResources().getDimensionPixelOffset(R.dimen._minus1sdp),
                getResources().getDimensionPixelOffset(R.dimen._1sdp)
        );

        searchBarLayout.setBackground(insetDrawable);

        iconInfo = findViewById(R.id.search_activity_icon_info);
        textViewInfo = findViewById(R.id.search_activity_textview_info);
        textViewInfo.setText(R.string.search_your_notes);

        ViewGroup.MarginLayoutParams searchBarLayoutParams = (ViewGroup.MarginLayoutParams) searchBarLayout.getLayoutParams();
        int statusBarId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        searchBarLayoutParams.setMargins(
                searchBarLayoutParams.leftMargin,
                searchBarLayoutParams.topMargin + getResources().getDimensionPixelSize(statusBarId),
                searchBarLayoutParams.rightMargin,
                searchBarLayoutParams.bottomMargin
        );

        int navigationBarId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        searchBarLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                searchBarLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                notesSearchRecyclerView.setPaddingRelative(
                        notesSearchRecyclerView.getPaddingLeft(),
                        notesSearchRecyclerView.getPaddingTop(),
                        notesSearchRecyclerView.getPaddingRight(),
                        notesSearchRecyclerView.getPaddingBottom() + getResources().getDimensionPixelSize(navigationBarId) +  + searchBarLayout.getHeight() + getResources().getDimensionPixelSize(statusBarId)
                );
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
            switch(resultCode) {
                case RESULT_OK:
                    if (data != null) {
                        searchNote(lastQuery, NotesAdapter.REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
                        isAnyChange = true;
                    }
                    break;
                case CreateNoteActivity.NOTE_UNCHANGED:
                    break;
            }
        }
    }

    private void searchNote(String query ,int requestCode, final boolean isNoteDeleted) {
        searchTimer = new Timer();

        searchTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Observable.fromCallable(() -> NotesDatabase.getDatabase(getApplicationContext())
                        .noteDao().searchNotes(query)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<List<Note>>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onNext(@NonNull List<Note> notes) {
                                switch (requestCode) {
                                    case REQUEST_CODE_SEARCH_NOTE:
                                        noteList.clear();
                                        noteList.addAll(notes);
                                        notesAdapter.notifyDataSetChanged();
                                        break;
                                    case NotesAdapter.REQUEST_CODE_UPDATE_NOTE:
                                        noteList.remove(noteClickedPosition);

                                        if (!isNoteDeleted) {
                                            noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                                        }
                                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                                        break;
                                }

                                if (noteList.isEmpty()) {
                                    iconInfo.setVisibility(View.VISIBLE);
                                    textViewInfo.setVisibility(View.VISIBLE);
                                    textViewInfo.setText(R.string.nothing_was_found);
                                } else {
                                    iconInfo.setVisibility(View.GONE);
                                    textViewInfo.setVisibility(View.GONE);
                                }


                            }

                            @Override
                            public void onError(@NonNull Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }
        }, 500);
    }

    private void cancelSearch() {
        if (searchTimer != null) {
            searchTimer.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        intent.putExtra("isAnyChange", isAnyChange);
        finish();
        super.onBackPressed();
    }
}