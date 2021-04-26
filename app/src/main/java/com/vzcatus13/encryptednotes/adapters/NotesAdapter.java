package com.vzcatus13.encryptednotes.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vzcatus13.encryptednotes.R;
import com.vzcatus13.encryptednotes.activities.CreateNoteActivity;
import com.vzcatus13.encryptednotes.entities.Note;
import com.vzcatus13.encryptednotes.listeners.NoteListener;
import com.vzcatus13.encryptednotes.utils.ColorNote;
import com.vzcatus13.encryptednotes.utils.Encryption;
import com.vzcatus13.encryptednotes.utils.Utils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    public static final int REQUEST_CODE_UPDATE_NOTE = 2;

    public static final String TAG = "NotesAdapter";

    private final List<Note> notes;
    private final NoteListener noteListener;
    private final boolean isGrid;

    private final Context context;

    private AlertDialog dialogDecryptNote;

    public NotesAdapter(List<Note> notes, NoteListener noteListener, Context context, RecyclerView recyclerView, boolean isGrid) {
        this.notes = notes;
        this.noteListener = noteListener;
        this.context = context;
        this.isGrid = isGrid;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_note,
                        parent,
                        false
                ),
                context
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (position == 0) {
            layoutParams.topMargin = 0;
        }

        if (position == 1 && isGrid) {
            layoutParams.topMargin = 0;
        }

        holder.setNote(notes.get(position));
        holder.noteLayout.setOnClickListener(v -> noteListener.onNoteClicked(notes.get(position), holder.itemView, position));
    }

    @Override
    public long getItemId(int position) {
        return (long) (position);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

            TextView noteTitle;
            TextView noteText;
            ImageView noteImage;

            LinearLayout noteLayout;

            Context context;

        public NoteViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            noteLayout = itemView.findViewById(R.id.layout_note);
            noteTitle = itemView.findViewById(R.id.layout_note_title);
            noteText = itemView.findViewById(R.id.layout_note_text);
            noteImage = itemView.findViewById(R.id.layout_note_image);
            this.context = context;
        }

        public void setNote(Note note) {
            String title = note.getTitle();
            char[] text;
            String colorString = note.getColor();
            String imagePath = note.getImagePath();

            if (note.isEncrypted()) {
                text = context.getString(R.string.encrypted_note_text).toCharArray();
                noteText.setGravity(Gravity.CENTER);
                noteLayout.findViewById(R.id.layout_note_lock_image).setVisibility(View.VISIBLE);
            } else {
                text = Utils.bytesToChars(note.getNoteText());
                noteText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                noteLayout.findViewById(R.id.layout_note_lock_image).setVisibility(View.GONE);
            }

            if (title.isEmpty()) {
                noteTitle.setVisibility(View.GONE);
            } else {
                noteTitle.setVisibility(View.VISIBLE);
                noteTitle.setText(title);
            }

            noteText.setText(text, 0, text.length);

            int color;
            if (colorString == null) {
                color = ColorNote.DEFAULT.getColor(context);
            } else {
                ColorNote colorNote = ColorNote.toColorNote(colorString);
                if (colorNote != null) {
                    color = colorNote.getColor(context);
                } else {
                    color = Color.parseColor(colorString);
                }
            }

            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(GradientDrawable.RECTANGLE);
            gradientDrawable.setColor(color);
            gradientDrawable.setCornerRadius(context.getResources().getDimension(R.dimen._5sdp));


            int rippleColor = ContextCompat.getColor(context, R.color.colorNoteRipple);
            RippleDrawable rippleDrawable = new RippleDrawable(
                    new ColorStateList(new int[][] { new int[] {} }, new int[] {rippleColor}),
                    gradientDrawable,
                    null
            );

            GradientDrawable noteOutline = new GradientDrawable();
            noteOutline.setShape(GradientDrawable.RECTANGLE);
            noteOutline.setCornerRadius(context.getResources().getDimension(R.dimen._5sdp));
            noteOutline.setStroke(context.getResources().getDimensionPixelOffset(R.dimen._1sdp), Utils.blendColorLightOrDarkMode(context, color, 0.4f));

            InsetDrawable insetDrawable = new InsetDrawable(rippleDrawable,
                    context.getResources().getDimensionPixelOffset(R.dimen._1sdp),
                    context.getResources().getDimensionPixelOffset(R.dimen._1sdp),
                    context.getResources().getDimensionPixelOffset(R.dimen._1sdp),
                    context.getResources().getDimensionPixelOffset(R.dimen._1sdp));

            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] {noteOutline, insetDrawable});
            itemView.setBackground(layerDrawable);

            ImageViewCompat.setImageTintList(noteLayout.findViewById(R.id.layout_note_lock_image), ColorStateList.valueOf(Utils.blendColorLightOrDarkMode(context, color, 0.4f)));

            if (imagePath != null && !note.isEncrypted()) {
                if (new File(imagePath).exists()) {
                    Glide.with(context).load(imagePath).into(noteImage);
                    noteLayout.setClipToOutline(true);
                    noteImage.setVisibility(View.VISIBLE);
                } else {
                    noteImage.setVisibility(View.GONE);
                }
            } else {
                noteImage.setVisibility(View.GONE);
            }

        }
    }

    public void openNote(Note note) {
        if (note.isEncrypted()) {
            showDecryptionDialog(note);
        } else {
            Intent intent = new Intent(context, CreateNoteActivity.class);
            intent.putExtra("isUpdate", true);
            intent.putExtra("note", note);
            ((Activity) context).startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
        }
    }

    private void showDecryptionDialog(Note note) {
        if (dialogDecryptNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context).inflate(
                    R.layout.dialog_decrypt_note,
                    (ViewGroup) ((Activity)context).findViewById(R.id.dialog_decrypt_note_layout)
            );

            view.findViewById(R.id.dialog_decrypt_note_indicator).setVisibility(View.GONE);
            view.setClipToOutline(true);
            builder.setView(view);
            dialogDecryptNote = builder.create();

            if (dialogDecryptNote.getWindow() != null) {
                dialogDecryptNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            TextInputLayout passwordInputLayout = view.findViewById(R.id.dialog_decrypt_note_password_layout);
            passwordInputLayout.setBoxStrokeColor(ContextCompat.getColor(view.getContext(), R.color.colorAccent));
            passwordInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            passwordInputLayout.setErrorIconTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            passwordInputLayout.setErrorTextColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));

            TextInputEditText passwordEditText = view.findViewById(R.id.dialog_decrypt_note_password_edit_text);

            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    passwordInputLayout.setError(null);
                }
            });

            TextView decryptButton = view.findViewById(R.id.dialog_decrypt_note_okay);
            TextView cancelButton = view.findViewById(R.id.dialog_decrypt_note_cancel);

            decryptButton.setOnClickListener( v -> {

                int passwordLength = passwordEditText.length();

                if (passwordLength > 0) {
                    view.findViewById(R.id.dialog_decrypt_note_indicator).setVisibility(View.VISIBLE);
                    dialogDecryptNote.setCanceledOnTouchOutside(false);
                    dialogDecryptNote.setCancelable(false);
                    passwordEditText.setFocusable(false);
                    passwordEditText.setEnabled(false);
                    decryptButton.setEnabled(false);
                    decryptButton.setTextColor(Color.GRAY);
                    cancelButton.setEnabled(false);
                    cancelButton.setTextColor(Color.GRAY);

                    char[] password = new char[passwordLength];
                    passwordEditText.getText().getChars(0, passwordLength, password, 0);
                    new Thread( () -> {
                        try {
                            Encryption.decrypt(note.getDummyData(), password);

                            ((Activity) context).runOnUiThread( () -> {
                                Intent intent = new Intent(context, CreateNoteActivity.class);
                                intent.putExtra("isUpdate", true);
                                intent.putExtra("note", note);
                                intent.putExtra("password", password);
                                ((Activity) context).startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
                                dialogDecryptNote.dismiss();
                            });
                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException e) {
                            ((Activity) context).runOnUiThread( () -> {
                                view.findViewById(R.id.dialog_decrypt_note_indicator).setVisibility(View.GONE);
                                dialogDecryptNote.dismiss();
                                View contextView = ((Activity) context).getWindow().getDecorView();
                                Snackbar.make(contextView, R.string.password_is_incorrect, Snackbar.LENGTH_SHORT)
                                        .setTextColor(ContextCompat.getColor(context, R.color.colorText))
                                        .show();
                            });
                        }
                    }).start();
                } else {
                    passwordInputLayout.setError(context.getString(R.string.field_is_empty));
                }
            });

            cancelButton.setOnClickListener( v -> dialogDecryptNote.dismiss());

            dialogDecryptNote.setOnDismissListener(dialog -> dialogDecryptNote = null);
        }

        dialogDecryptNote.show();
    }
}