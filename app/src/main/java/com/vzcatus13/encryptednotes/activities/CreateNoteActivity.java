package com.vzcatus13.encryptednotes.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vzcatus13.encryptednotes.R;
import com.vzcatus13.encryptednotes.utils.ColorNote;
import com.vzcatus13.encryptednotes.database.NotesDatabase;
import com.vzcatus13.encryptednotes.entities.Note;
import com.vzcatus13.encryptednotes.utils.Encryption;
import com.vzcatus13.encryptednotes.utils.FileUtils;
import com.vzcatus13.encryptednotes.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CreateNoteActivity extends AppCompatActivity {

    public static final int NOTE_DISCARDED = 2;
    public static final int NOTE_UNCHANGED = 3;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static ActivityResultLauncher<Intent> requestSelectImage;

    public static final String TAG = "CreateNoteActivity";

    private EditText inputNoteTitle;
    private EditText inputNoteText;
    private TextView modifiedDateNoteText;
    private TextView createdDateNoteText;
    private ImageView noteImage;
    private CoordinatorLayout createNoteLayout;
    private BottomSheetBehavior<CoordinatorLayout> noteMenuBottomSheet;
    private BottomSheetBehavior<LinearLayout> addMenuBottomSheet;

    private AlertDialog dialogDeleteNote;
    private AlertDialog dialogEncryptNote;
    private AlertDialog dialogRemoveEncryption;

    private Note existingNote;
    private long notebookId;
    private String selectedNoteColor;
    private boolean isNoteChanged;
    private boolean wasChanged;
    private boolean isNoteEncrypted;
    private boolean wasNoteEncrypted;
    private Bitmap imageBitmap;
    private boolean isImageChanged;
    private boolean isImageRemoved;
    char[] password;

    ColorNote[] colorNotes = {
            ColorNote.DEFAULT,
            ColorNote.LIGHT_PINK,
            ColorNote.PEACH_ORANGE,
            ColorNote.LEMON_YELLOW,
            ColorNote.TEA_GREEN,
            ColorNote.SKY_BLUE,
            ColorNote.BABY_BLUE,
            ColorNote.LAVENDER_PURPLE,
            ColorNote.PURPLE_PINK,
    };
    List<View> colorViews;
    List<ImageView> colorImageViews;
    LinearLayout colorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Disable screenshot capture
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_create_note);

        inputNoteTitle = findViewById(R.id.note_title);
        inputNoteText = findViewById(R.id.note_text);
        modifiedDateNoteText = findViewById(R.id.note_modified_date);
        createdDateNoteText = findViewById(R.id.note_created_date);
        noteImage = findViewById(R.id.note_image);
        createNoteLayout = findViewById(R.id.create_note);


        // Is this note exist and want to be updated
        if (getIntent().getBooleanExtra("isUpdate", false)) {
            existingNote = (Note) getIntent().getSerializableExtra("note");
            isImageChanged = false;
            isNoteEncrypted = existingNote.isEncrypted();
            password = getIntent().getCharArrayExtra("password");
            notebookId = existingNote.getNotebookId();
            updateNoteView();
        } else {
            // New note will be created
            selectedNoteColor = ColorNote.DEFAULT.toString();
            String createdText = getString(R.string.created) + " " + Utils.dateToString(CreateNoteActivity.this, new Date());
            createdDateNoteText.setText(createdText);
            isNoteEncrypted = false;
            notebookId = getIntent().getLongExtra("notebookId", -1);
        }
        isNoteChanged = false;
        wasNoteEncrypted = false;

        // Init note view
        initNoteView();

        // Register request for image file. Called in selectImage()
        requestSelectImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (result.getData() != null) {
                            Uri selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                try {
                                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                                    // If note exist then it and its image are changed
                                    if (existingNote != null) {
                                        // If image already exist then delete it
                                        if (imageBitmap != null) {
                                            new File(existingNote.getImagePath()).delete();
                                            imageBitmap = null;
                                        }
                                        isNoteChanged = true;
                                        isImageChanged = true;
                                    }

                                    imageBitmap = BitmapFactory.decodeStream(inputStream);
                                    noteImage.setImageBitmap(imageBitmap);
                                    noteImage.setVisibility(View.VISIBLE);
                                    findViewById(R.id.note_remove_image).setVisibility(View.VISIBLE);
                                    setImageDeleteIcon();
                                } catch (FileNotFoundException e) {
                                    Log.e(TAG, e.getMessage());
                                    showSnackBar(R.string.something_went_wrong);
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Initialize views of note
     * It needs to be called onCreate
     */
    private void initNoteView() {
        // Set up onClickListener on back button
        findViewById(R.id.back_btn).setOnClickListener( (view) -> onBackPressed() );

        // Set up onClickListener on remove image button
        ImageView noteRemoveImageButton = findViewById(R.id.note_remove_image);
        noteRemoveImageButton.setOnClickListener( view -> {
            if (existingNote != null) {
                isImageRemoved = true;
                isNoteChanged = true;
                new File(existingNote.getImagePath()).delete();
                existingNote.setImagePath(null);
            }

            imageBitmap = null;
            noteImage.setVisibility(View.GONE);
            noteRemoveImageButton.setVisibility(View.GONE);
        });

        // Listen title text changes
        inputNoteTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // After text of title is changed then note is also changed
                isNoteChanged = true;
            }
        });

        // Listen note text changes
        inputNoteText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // After text of note text is changed then note is also changed
                if (inputNoteText.hasFocus()) {
                    isNoteChanged = true;
                }
            }
        });

        // Init note menu
        initNoteMenu();
        // Init note add menu
        initNoteAddMenu();

        // Set color of note same as selected color
        setColorSelected(selectedNoteColor);
    }

    /**
     * Initialize note menu
     * It needs to be called onCreate
     */
    private void initNoteMenu() {
        // This is the root layout of menu
        final CoordinatorLayout layoutNoteMenu = findViewById(R.id.note_menu_bottom_sheet);

        // Initialize bottom sheet
        noteMenuBottomSheet = BottomSheetBehavior.from(layoutNoteMenu);
        // Set onClick listener on icon of menu
        findViewById(R.id.note_menu).setOnClickListener( v -> {
            // If bottom sheet is not expanded
            if (noteMenuBottomSheet.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                // Expand note menu bottom sheet and then hide add menu bottom sheet
                noteMenuBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                addMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                noteMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        colorList = findViewById(R.id.note_color_list); // This is the root layout for colors
        colorViews = new ArrayList<>(); // List with views of colors
        colorImageViews = new ArrayList<>(); // List with image views of colors, they are used to show selected color

        // For each color in colorNotes
        for (ColorNote colorNote : colorNotes) {
            int primaryColor = colorNote.getColor(CreateNoteActivity.this);
            int accentColor = Utils.blendColorLightOrDarkMode(CreateNoteActivity.this, primaryColor, 0.4f);

            // This is the parent layout for view and imageView
            FrameLayout frameLayout = new FrameLayout(CreateNoteActivity.this);
            LinearLayout.LayoutParams frameLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            frameLayout.setLayoutParams(frameLayoutParams);
            FrameLayout.MarginLayoutParams marginFrameLayoutParams = (FrameLayout.MarginLayoutParams) frameLayout.getLayoutParams();
            marginFrameLayoutParams.setMarginEnd(getResources().getDimensionPixelSize(R.dimen._10sdp));
            frameLayout.setLayoutParams(marginFrameLayoutParams);

            // This is the view of color
            View view = new View(CreateNoteActivity.this);
            ViewGroup.LayoutParams viewLayoutParams = new ViewGroup.LayoutParams(getResources().getDimensionPixelSize(R.dimen._25sdp), getResources().getDimensionPixelSize(R.dimen._25sdp));
            view.setLayoutParams(viewLayoutParams);

            // It is circle with border of R.dimen._1sdp
            GradientDrawable circleOutline = new GradientDrawable();
            circleOutline.setShape(GradientDrawable.OVAL);
            circleOutline.setStroke(getResources().getDimensionPixelSize(R.dimen._1sdp), accentColor);

            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(primaryColor);

            InsetDrawable insetCircle = new InsetDrawable(circle,
                    getResources().getDimensionPixelOffset(R.dimen._1sdp),
                    getResources().getDimensionPixelOffset(R.dimen._1sdp),
                    getResources().getDimensionPixelOffset(R.dimen._1sdp),
                    getResources().getDimensionPixelOffset(R.dimen._1sdp)
            );

            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] {circleOutline, insetCircle});
            view.setBackground(layerDrawable);

            colorViews.add(view);

            // This is imageView, which is used for displaying selected color
            // Padding is greater than border of circle, so it showing inside of color view
            ImageView imageView = new ImageView(CreateNoteActivity.this);
            ViewGroup.LayoutParams imageViewLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, getResources().getDimensionPixelSize(R.dimen._25sdp));
            imageView.setLayoutParams(imageViewLayoutParams);
            imageView.setPadding(
                    getResources().getDimensionPixelSize(R.dimen._3sdp),
                    getResources().getDimensionPixelSize(R.dimen._3sdp),
                    getResources().getDimensionPixelSize(R.dimen._3sdp),
                    getResources().getDimensionPixelSize(R.dimen._3sdp)
            );

            imageView.setColorFilter(accentColor);

            colorImageViews.add(imageView);

            frameLayout.addView(view);
            frameLayout.addView(imageView);

            colorList.addView(frameLayout);

            // When color view is clicked set selected color to name of enum
            // Color changed = note changed
            view.setOnClickListener(v -> {
                selectedNoteColor = colorNote.toString();
                setColorSelected(selectedNoteColor);
                isNoteChanged = true;
            });
        }

        // If note exists
        if (existingNote != null) {
            // Show delete option and set onClick listener
            layoutNoteMenu.findViewById(R.id.note_delete).setVisibility(View.VISIBLE);
            layoutNoteMenu.findViewById(R.id.note_delete).setOnClickListener(v -> {
                // Hide all bottom sheets and then show dialog to delete note
                addMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                noteMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });
        }

        // If note is not encrypted show encryption option
        if (!isNoteEncrypted) {
            layoutNoteMenu.findViewById(R.id.note_decrypt).setVisibility(View.GONE);
            layoutNoteMenu.findViewById(R.id.note_encrypt).setVisibility(View.VISIBLE);
            layoutNoteMenu.findViewById(R.id.note_encrypt).setOnClickListener( v -> {
                addMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                noteMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                // If note is empty (text is empty) then note can not be encrypted
                if (inputNoteText.length() == 0) {
                    showSnackBar(R.string.you_cant_encrypt_empty);
                } else {
                    showEncryptionDialog();
                }
            });
        } else {
            // Note is encrypted show decryption option
            layoutNoteMenu.findViewById(R.id.note_encrypt).setVisibility(View.GONE);
            layoutNoteMenu.findViewById(R.id.note_decrypt).setVisibility(View.VISIBLE);
            layoutNoteMenu.findViewById(R.id.note_decrypt).setOnClickListener( v -> {
                addMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                noteMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                // Show dialog to remove encryption
                showRemoveEncryptionDialog();
            });
        }
    }

    /**
     * Initialize note add menu
     * It needs to called onCreate
     */
    private void initNoteAddMenu() {
        final LinearLayout layoutAddMenu = findViewById(R.id.add_menu_bottom_sheet);

        addMenuBottomSheet = BottomSheetBehavior.from(layoutAddMenu);
        findViewById(R.id.note_add_menu).setOnClickListener(view -> {
            if (addMenuBottomSheet.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                addMenuBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                noteMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                addMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        layoutAddMenu.findViewById(R.id.note_add_image).setOnClickListener( v -> {
            addMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);

            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        CreateNoteActivity.this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION
                );
            } else {
                selectImage();
            }
        });
    }

    /**
     * Update note view with existing note information e.g. color or text
     * Also, if note is encrypted, call decrypt
     * It needs to be called onCreate
     */
    private void updateNoteView() {
        // If note is encrypted perform decryption of encrypted content
        if (existingNote.isEncrypted()) {
            // While note is decrypting and updating all views are hidden and indicator is visible
            inputNoteTitle.setVisibility(View.GONE);
            inputNoteText.setVisibility(View.GONE);
            createdDateNoteText.setVisibility(View.GONE);
            modifiedDateNoteText.setVisibility(View.GONE);
            noteImage.setVisibility(View.GONE);
            findViewById(R.id.note_remove_image).setVisibility(View.GONE);
            findViewById(R.id.note_icons).setVisibility(View.GONE);
            findViewById(R.id.indicator_create_note).setVisibility(View.VISIBLE);
            // Start decryption on new thread
            new Thread( () -> {
                try {
                    char[] noteText = Utils.bytesToChars(Encryption.decrypt(
                            existingNote.getNoteText(),
                            password
                    ));

                    // After decryption is completed show all views and hide indicator
                    runOnUiThread( () -> {
                        inputNoteText.setText(noteText, 0, noteText.length);
                        inputNoteTitle.setVisibility(View.VISIBLE);
                        inputNoteText.setVisibility(View.VISIBLE);
                        createdDateNoteText.setVisibility(View.VISIBLE);
                        modifiedDateNoteText.setVisibility(View.VISIBLE);
                        noteImage.setVisibility(View.VISIBLE);
                        findViewById(R.id.note_remove_image).setVisibility(View.VISIBLE);
                        findViewById(R.id.note_icons).setVisibility(View.VISIBLE);
                        findViewById(R.id.indicator_create_note).setVisibility(View.GONE);
                        Arrays.fill(noteText, '\0');
                    });
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException e) {
                    Log.e(TAG, e.getMessage());
                    showSnackBar(R.string.something_went_wrong);
                }
            }).start();
        } else {
            // Note is not encrypted, so just get plain content
            char[] noteText = Utils.bytesToChars(existingNote.getNoteText());
            inputNoteText.setText(noteText, 0, noteText.length);
        }

        inputNoteTitle.setText(existingNote.getTitle());
        selectedNoteColor = existingNote.getColor();

        // If there is any path of image
        if (existingNote.getImagePath() != null) {
            // Check if file with specified path exist, because it somehow can be missing e.g. notes are restored from backup from other device
            if (new File(existingNote.getImagePath()).exists()) {
                imageBitmap = BitmapFactory.decodeFile(existingNote.getImagePath());
                noteImage.setImageBitmap(imageBitmap);
                noteImage.setVisibility(View.VISIBLE);
                findViewById(R.id.note_remove_image).setVisibility(View.VISIBLE);
                setImageDeleteIcon();
            } else {
                noteImage.setVisibility(View.GONE);
            }
        } else {
            noteImage.setVisibility(View.GONE);
        }

        String modifiedText =  getString(R.string.modified) + " " + Utils.dateToString(CreateNoteActivity.this, existingNote.getModifiedAt());
        modifiedDateNoteText.setText(modifiedText);
        modifiedDateNoteText.setVisibility(View.VISIBLE);

        String createdText = getString(R.string.created) + " " + Utils.dateToString(CreateNoteActivity.this, existingNote.getCreatedAt());
        createdDateNoteText.setText(createdText);
    }

    /**
     * Called when note need to be saved to database
     */
    private void saveNote(boolean isFinish) {
        // If note is changed and text of note is not empty
        if (isNoteChanged && inputNoteText.length() > 0) {
            // Do activity need to be finished after saving
            // If so, hide all views and show indicator
            if (isFinish) {
                addMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                noteMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                inputNoteTitle.setVisibility(View.GONE);
                inputNoteText.setVisibility(View.GONE);
                createdDateNoteText.setVisibility(View.GONE);
                modifiedDateNoteText.setVisibility(View.GONE);
                noteImage.setVisibility(View.GONE);
                findViewById(R.id.note_remove_image).setVisibility(View.GONE);
                findViewById(R.id.note_icons).setVisibility(View.GONE);
                findViewById(R.id.indicator_create_note).setVisibility(View.VISIBLE);
            }

            // Save note on new thread
            Observable.fromCallable(() -> {
                final Note note = new Note();

                note.setTitle(inputNoteTitle.getText().toString());
                note.setModifiedAt(new Date());
                note.setColor(selectedNoteColor);
                note.setEncrypted(isNoteEncrypted);
                note.setNotebookId(notebookId);

                char[] noteText = new char[inputNoteText.length()];

                // Note need to be encrypted
                if (isNoteEncrypted) {
                    if (password != null) {
                        inputNoteText.getText().getChars(0, inputNoteText.length(), noteText, 0);
                        note.setNoteText(Encryption.encrypt(
                                Utils.charsToBytes(noteText),
                                password
                        ));

                        // Set dummy data, which is encrypted
                        // It used to check auth tag
                        note.setDummyData(Encryption.encrypt(
                                Utils.getRandomBytes(6),
                                password)
                        );
                    }
                } else {
                    inputNoteText.getText().getChars(0, inputNoteText.length(), noteText, 0);
                    note.setNoteText(Utils.charsToBytes(noteText));
                }

                // Here permanent decryption is called
                // Note was encrypted, but now it need to be decrypted
                if (existingNote != null && wasNoteEncrypted && !isNoteEncrypted) {
                    if (password != null) {
                        try {
                            note.setNoteText(Encryption.decrypt(
                                    existingNote.getNoteText(),
                                    password
                            ));
                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException e) {
                            Log.e(TAG, e.getMessage());
                            showSnackBar(R.string.something_went_wrong);
                        }
                        note.setDummyData(null);
                    }
                }

                // Check if note is exist
                // If so, then id and date of creation stays the same
                if (existingNote != null) {
                    note.setId(existingNote.getId());
                    note.setCreatedAt(existingNote.getCreatedAt());
                } else {
                    note.setCreatedAt(new Date());
                }

                // If image existed, but was removed OR exist
                if (imageBitmap != null || isImageRemoved) {
                    // Existing note exist
                    if (existingNote != null) {
                        // If image is changed e.g. replaced
                        if (isImageChanged) {
                            note.setImagePath(FileUtils.saveImageToStorage(CreateNoteActivity.this, imageBitmap));
                        } else {
                            note.setImagePath(existingNote.getImagePath());
                        }
                    } else {
                        note.setImagePath(FileUtils.saveImageToStorage(CreateNoteActivity.this, imageBitmap));
                    }
                }

                // If activity is finish, so populate password char array with zero chars
                if (password != null && isFinish) {
                    Arrays.fill(password, '\0');
                }

                // Populate note text array with zero chars
                Arrays.fill(noteText, '\0');

                isNoteChanged = false;
                wasChanged = true;

                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return true;
            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Boolean aBoolean) {

                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.e(TAG, e.getMessage());
                        }

                        @Override
                        public void onComplete() {
                             if (dialogEncryptNote != null) {
                                 dialogEncryptNote.dismiss();
                             }

                            if (dialogRemoveEncryption != null) {
                                dialogRemoveEncryption.dismiss();
                            }

                            // Do activity need to be finished after saving
                            // If so, finish
                            if (isFinish) {
                                Intent intent = new Intent();
                                setResult(RESULT_OK, intent);
                                finish();
                                CreateNoteActivity.super.onBackPressed();
                            }
                        }
                    });
        } else {
            // Do activity need to be finished after saving
            if (isFinish) {
                Intent intent = new Intent();
                if (wasChanged) {
                    if (password != null) {
                        Arrays.fill(password, '\0');
                    }
                    setResult(RESULT_OK, intent);
                    finish();
                    super.onBackPressed();
                }

                if (existingNote == null) {
                    setResult(NOTE_DISCARDED, intent);
                } else {
                    if (inputNoteText.length() > 0) {
                        setResult(NOTE_UNCHANGED, intent);
                    } else {
                        deleteNote(existingNote);
                    }
                }
                finish();
                super.onBackPressed();
            }
        }

        // TODO: add transfer note from notebook to notebook
    }

    /**
     * Called when note need to be deleted
     * After the call the activity is finished
     * @param note Note object to be deleted
     */
    private void deleteNote(Note note) {
        Observable.fromCallable( () -> {
            NotesDatabase.getDatabase(getApplicationContext()).noteDao().deleteNote(note);
            return true;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull Boolean aBoolean) {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        // Delete image from storage if there so
                        if (note.getImagePath() != null) {
                            new File(note.getImagePath()).delete();
                        }

                        Intent intent = new Intent();
                        intent.putExtra("isNoteDeleted", true);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
    }

    // This method called when user want to delete note
    private void showDeleteNoteDialog() {
        // If dialog is not initialized
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.dialog_delete_note,
                    (ViewGroup) findViewById(R.id.dialog_delete_note_layout)
            );

            builder.setView(view);
            dialogDeleteNote = builder.create();

            // Set background outside of dialog to transparent
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            // If user clicks delete
            view.findViewById(R.id.dialog_delete_note_okay).setOnClickListener( v -> {
                // Delete note and hide dialog
                deleteNote(existingNote);
                dialogDeleteNote.dismiss();
            });

            // If user clicks cancel then hide dialog
            view.findViewById(R.id.dialog_delete_note_cancel).setOnClickListener( v -> dialogDeleteNote.dismiss());
        }

        dialogDeleteNote.show();
    }

    /**
     * Called when user want to encrypt note
     * After the call the activity is finished
     */
    private void showEncryptionDialog() {
        // If dialog is not initialized
        if (dialogEncryptNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.dialog_encrypt_note,
                    (ViewGroup) findViewById(R.id.dialog_encrypt_note_layout)
            );

            // Indicator is hidden
            view.findViewById(R.id.dialog_encrypt_note_indicator).setVisibility(View.GONE);
            // Clip to outline is set to true, so if background of root view is custom (e.g. rounded corners)
            // then inner views will not go outside of root background (e.g. indicator will be rounded too)
            view.setClipToOutline(true);

            builder.setView(view);
            dialogEncryptNote = builder.create();

            // Set background outside of dialog to transparent
            if (dialogEncryptNote.getWindow() != null) {
                dialogEncryptNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }


            TextInputLayout passwordInputLayout = view.findViewById(R.id.dialog_encrypt_note_password_layout);
            passwordInputLayout.setBoxStrokeColor(ContextCompat.getColor(view.getContext(), R.color.colorAccent));
            passwordInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            passwordInputLayout.setErrorIconTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            passwordInputLayout.setErrorTextColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));

            TextInputLayout passwordCheckInputLayout = view.findViewById(R.id.dialog_encrypt_note_password_check_layout);
            passwordCheckInputLayout.setBoxStrokeColor(ContextCompat.getColor(view.getContext(), R.color.colorAccent));
            passwordCheckInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            passwordCheckInputLayout.setErrorIconTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            passwordCheckInputLayout.setErrorTextColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));

            TextInputEditText passwordEditText = view.findViewById(R.id.dialog_encrypt_note_password_edit_text);
            TextInputEditText passwordCheckEditText = view.findViewById(R.id.dialog_encrypt_note_password_check_edit_text);


            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Password should be at least 4 chars long, if less show error
                    if (passwordEditText.length() < 4) {
                        passwordInputLayout.setError(getString(R.string.password_min_limit));
                    } else {
                        passwordInputLayout.setError(null);
                    }
                }
            });

            passwordCheckEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    // When user input something then they try to correct error, so hide error message
                    passwordCheckInputLayout.setError(null);
                }
            });

            TextView encryptButton = view.findViewById(R.id.dialog_encrypt_note_okay);
            TextView cancelButton = view.findViewById(R.id.dialog_encrypt_note_cancel);

            // If user clicks encrypt button
            encryptButton.setOnClickListener( v -> {

                int passwordLength = passwordEditText.length();
                int passwordCheckLength = passwordCheckEditText.length();

                // Password and confirmation should be not empty
                if (passwordLength > 0 && passwordCheckLength > 0) {
                    // Password should be 4+ chars long
                    if (passwordLength >= 4) {

                        char[] pass = new char[passwordLength];
                        passwordEditText.getText().getChars(0, passwordLength, pass, 0);
                        char[] passCheck = new char[passwordCheckLength];
                        passwordCheckEditText.getText().getChars(0, passwordCheckLength, passCheck, 0);

                        // Check if password and confirmation are same
                        if (pass.length == passCheck.length && Arrays.equals(pass, passCheck)) {
                            // If user somehow try to encrypt empty text, then do not encrypt
                            if (inputNoteText.length() != 0) {
                                // Disable all buttons of dialog
                                // Disable all edit texts of dialog
                                // Disable cancel of dialog on back button click
                                // Disable cancel of dialog by touching outside of it
                                // Show indicator
                                view.findViewById(R.id.dialog_encrypt_note_indicator).setVisibility(View.VISIBLE);
                                dialogEncryptNote.setCanceledOnTouchOutside(false);
                                dialogEncryptNote.setCancelable(false);
                                passwordEditText.setFocusable(false);
                                passwordEditText.setEnabled(false);
                                passwordCheckEditText.setFocusable(false);
                                passwordCheckEditText.setEnabled(false);
                                encryptButton.setEnabled(false);
                                encryptButton.setTextColor(Color.GRAY);
                                cancelButton.setEnabled(false);
                                cancelButton.setTextColor(Color.GRAY);

                                password = pass.clone();
                                isNoteEncrypted = true;
                                isNoteChanged = true;
                                Arrays.fill(passCheck, '\0');
                                saveNote(true);
                            } else {
                                dialogEncryptNote.dismiss();
                            }
                        } else {
                            // Password and confirmation are different
                            passwordCheckInputLayout.setError(getString(R.string.passwords_no_match));
                        }
                    } else {
                        // Password is smaller than 4 chars long
                        passwordInputLayout.setError(getString(R.string.password_min_limit));
                    }
                } else {
                    // Password and confirmation is empty
                    passwordInputLayout.setError(getString(R.string.field_is_empty));
                    passwordCheckInputLayout.setError(getString(R.string.field_is_empty));
                }
            });

            // If user clicks cancel button
            cancelButton.setOnClickListener( v -> dialogEncryptNote.dismiss());

            // After every dismiss (cancel) of dialog it will be destroyed
            dialogEncryptNote.setOnDismissListener(dialog -> dialogEncryptNote = null);
        }

        dialogEncryptNote.show();
    }

    /**
     * Called when user wants to remove encryption
     * After the call the activity is finished
     */
    private void showRemoveEncryptionDialog() {
        // If dialog is not initialized
        if (dialogRemoveEncryption == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.dialog_remove_encryption,
                    (ViewGroup) findViewById(R.id.dialog_remove_encryption_layout)
            );

            // Indicator is hidden
            view.findViewById(R.id.dialog_remove_encryption_indicator).setVisibility(View.GONE);
            // Clip to outline is set to true, so if background of root view is custom (e.g. rounded corners)
            // then inner views will not go outside of root background (e.g. indicator will be rounded too)
            view.setClipToOutline(true);

            builder.setView(view);
            dialogRemoveEncryption = builder.create();

            // Set background outside of dialog to transparent
            if (dialogRemoveEncryption.getWindow() != null) {
                dialogRemoveEncryption.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            TextView removeEncryptionButton = view.findViewById(R.id.dialog_remove_encryption_okay);
            TextView cancelButton = view.findViewById(R.id.dialog_remove_encryption_cancel);

            // If user clicks remove button
            removeEncryptionButton.setOnClickListener( v -> {
                // Note was encrypted and is changed
                wasNoteEncrypted = true;
                isNoteEncrypted = false;
                isNoteChanged = true;
                // Disable all buttons of dialog
                // Disable cancel of dialog on back button click
                // Disable cancel of dialog by touching outside of it
                // Show indicator
                view.findViewById(R.id.dialog_remove_encryption_indicator).setVisibility(View.VISIBLE);
                removeEncryptionButton.setEnabled(false);
                removeEncryptionButton.setTextColor(Color.GRAY);
                cancelButton.setEnabled(false);
                cancelButton.setTextColor(Color.GRAY);
                dialogRemoveEncryption.setCancelable(false);
                dialogRemoveEncryption.setCanceledOnTouchOutside(false);

                saveNote(true);
            });

            // If user clicks cancel button
            cancelButton.setOnClickListener( v -> dialogRemoveEncryption.dismiss() );
        }

        dialogRemoveEncryption.show();
    }


    private void setNoteColor(int color) {
        createNoteLayout.setBackgroundColor(color);
        createNoteLayout.findViewById(R.id.note_icons).setBackgroundColor(color);
        createNoteLayout.findViewById(R.id.note_menu_bottom_sheet).setBackgroundColor(color);
        createNoteLayout.findViewById(R.id.add_menu_bottom_sheet).setBackgroundColor(color);

        Utils.setTransparentStatusAndNavigation(getWindow(), false, color, true, true);
    }

    /**
     * Called when user want to change (select) color
     * @param colorSelected NoteColor's name as string or HEX string
     */
    private void setColorSelected(String colorSelected) {
        // Set all imageViews to 0, so nobody is selected
        for (ImageView imageView : colorImageViews) {
            imageView.setImageResource(0);
        }

        // Set content description of color views
        for (int i = 0; i < colorViews.size(); i++) {
            colorViews.get(i).setContentDescription(colorNotes[i].getFriendlyName(CreateNoteActivity.this));
        }

        // Translate selectedColor to ColorNote enum
        // If selectedColor is custom (e.g. just hex code) then its ColorNote is null
        ColorNote colorNote = ColorNote.toColorNote(colorSelected);
        int color;

        // If colorNote is not null
        if (colorNote != null) {
            String selectedWord = getString(R.string.selected).toLowerCase();

            // Loop through available colorNotes
            for (int i = 0; i < colorNotes.length; i++) {
                // If colorNote is found then set its imageView as selected, also change content description to selected
                if (colorNote == colorNotes[i]) {
                    colorImageViews.get(i).setImageResource(R.drawable.ic_done);
                    colorViews.get(i).setContentDescription(colorNote.getFriendlyName(CreateNoteActivity.this) + " " + selectedWord);
                    break;
                }
            }

            // Get color as int
            color = colorNote.getColor(CreateNoteActivity.this);
        } else {
            // If color is custom then just parse it
            color = Color.parseColor(colorSelected);
        }

        // Set color of note
        setNoteColor(color);
    }

    /**
     * Called when user want to select image for note
     */
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        requestSelectImage.launch(intent);
    }


    private void setImageDeleteIcon() {
        ImageView deleteIcon = findViewById(R.id.note_remove_image);
        if (imageBitmap != null) {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(GradientDrawable.RECTANGLE);
            gradientDrawable.setCornerRadius(getResources().getDimension(R.dimen._6sdp));
            int defaultIconColor = ContextCompat.getColor(getApplicationContext(), R.color.colorRed);
            int adaptedIconColor = Utils.blendColorLightOrDarkMode(CreateNoteActivity.this, defaultIconColor, 0.25f);
            gradientDrawable.setStroke(
                   getResources().getDimensionPixelOffset(R.dimen._3sdp),
                    adaptedIconColor
            );

            deleteIcon.setImageTintList(ColorStateList.valueOf(adaptedIconColor));
            deleteIcon.setBackground(gradientDrawable);
            deleteIcon.setImageResource(R.drawable.ic_trash);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                showSnackBar(R.string.permission_denied);
            }
        }
    }

    private void showSnackBar(String string) {
        Snackbar.make(findViewById(R.id.note_icons), string, Snackbar.LENGTH_SHORT)
                .setAnchorView(findViewById(R.id.note_icons))
                .setTextColor(ContextCompat.getColor(CreateNoteActivity.this, R.color.colorText))
                .show();
    }

    private void showSnackBar(@StringRes int stringResId) {
        showSnackBar(getString(stringResId));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // If any of bottom sheets is opened and user clicks outside of it then hide bottom any opened bottom sheet
            if (noteMenuBottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                Rect outRect = new Rect();
                findViewById(R.id.note_menu_bottom_sheet).getGlobalVisibleRect(outRect);

                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    noteMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

            if (addMenuBottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                Rect outRect = new Rect();
                findViewById(R.id.add_menu_bottom_sheet).getGlobalVisibleRect(outRect);

                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    addMenuBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNote(false);
    }

    @Override
    public void onBackPressed() {
        saveNote(true);
    }
}