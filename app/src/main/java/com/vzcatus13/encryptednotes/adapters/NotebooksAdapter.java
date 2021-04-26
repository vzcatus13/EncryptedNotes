package com.vzcatus13.encryptednotes.adapters;

import android.app.Activity;
import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.vzcatus13.encryptednotes.R;
import com.vzcatus13.encryptednotes.database.NotesDatabase;
import com.vzcatus13.encryptednotes.entities.Notebook;
import com.vzcatus13.encryptednotes.listeners.NotebookListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class NotebooksAdapter extends RecyclerView.Adapter<NotebooksAdapter.NotebookViewHolder> {

    private final static int NOTEBOOK_ITEM = 1;
    private final static int ADD_BUTTON_ITEM = 2;

    private final List<Notebook> notebooks;
    private int selectedPosition = 0;
    private final NotebookListener notebookListener;
    private View mainNotebookView;

    private final Context context;

    private AlertDialog dialogAddNotebook;
    private AlertDialog dialogDeleteNotebook;
    private AlertDialog dialogEditNotebook;

    public NotebooksAdapter(List<Notebook> notebooks, NotebookListener notebookListener, Context context) {
        this.notebooks = notebooks;
        this.notebookListener = notebookListener;
        this.context = context;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    @NonNull
    @Override
    public NotebookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView;

        if (viewType == NOTEBOOK_ITEM) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_containter_notebook,
                    parent,
                    false
            );
        } else {
            ImageView addButton = new ImageView(context);
            addButton.setImageResource(R.drawable.ic_round_add_circle_outline);
            addButton.setColorFilter(ContextCompat.getColor(context, R.color.colorNotebookSelected));
            ViewGroup.MarginLayoutParams marginLayoutParams = new ViewGroup.MarginLayoutParams(context.getResources().getDimensionPixelSize(R.dimen._25sdp), context.getResources().getDimensionPixelSize(R.dimen._25sdp));
            marginLayoutParams.setMargins(
                    0,
                    context.getResources().getDimensionPixelSize(R.dimen._9sdp),
                    context.getResources().getDimensionPixelSize(R.dimen._3sdp),
                    context.getResources().getDimensionPixelSize(R.dimen._9sdp)
            );
            addButton.setLayoutParams(marginLayoutParams);
            itemView = addButton;
        }

        return new NotebookViewHolder(itemView, context);
    }

    @Override
    public void onBindViewHolder(@NonNull NotebookViewHolder holder, int position) {
        if (position < notebooks.size()) {
            if (position == 0) {
                mainNotebookView = holder.itemView;
            }

            holder.setNotebook(notebooks.get(position), position == selectedPosition);
            holder.itemView.setOnClickListener(v -> notebookListener.onNotebookClicked(notebooks.get(position), holder.itemView, position));
            if (position != 0) {
                holder.itemView.setOnLongClickListener(v -> {
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.AppTheme_BottomSheetDialogTheme_Default);
                    View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_notebook_menu, (LinearLayout) ((Activity) context).findViewById(R.id.notebook_menu_bottom_sheet));

                    bottomSheetView.findViewById(R.id.edit_notebook).setOnClickListener(view -> {
                        showEditDialog(position);
                        bottomSheetDialog.cancel();
                    });

                    bottomSheetView.findViewById(R.id.delete_notebook).setOnClickListener(view -> {
                        showDeleteDialog(position);
                        bottomSheetDialog.cancel();
                    });

                    bottomSheetDialog.setContentView(bottomSheetView);
                    bottomSheetDialog.show();
                    return true;
                });
            }
        } else {
            if (!notebooks.isEmpty()) {
                holder.itemView.setOnClickListener(v -> showAddNotebookDialog());
                holder.itemView.setVisibility(View.VISIBLE);
            } else {
                holder.itemView.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return notebooks.size() + 1;
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == notebooks.size()) ? ADD_BUTTON_ITEM : NOTEBOOK_ITEM;
    }

    static class NotebookViewHolder extends RecyclerView.ViewHolder {

        TextView notebookTitle;
        LayerDrawable bgNotebook;
        LayerDrawable bgNotebookSelected;

        Context context;

        public NotebookViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            notebookTitle = itemView.findViewById(R.id.layout_notebook_title);
            this.context = context;
        }

        public void setNotebook(Notebook notebook, boolean isSelected) {
            long id = notebook.getId();
            String title = notebook.getTitle();

            if (isSelected) {
                notebookTitle.setTextColor(ContextCompat.getColor(context, R.color.colorNotebookSelectedText));
            } else {
                notebookTitle.setTextColor(ContextCompat.getColor(context, R.color.colorNotebookText));
            }
            if (id == -1) {
                notebookTitle.setText(context.getString(R.string.all_notes));
            } else {
                notebookTitle.setText(title);
            }
            itemView.setBackground(getBackground(isSelected));
        }

        private LayerDrawable getBackground(boolean isSelected) {
            if (isSelected) {
                if (bgNotebookSelected == null) {
                    GradientDrawable gradientDrawable = new GradientDrawable();
                    gradientDrawable.setShape(GradientDrawable.RECTANGLE);
                    gradientDrawable.setColor(ContextCompat.getColor(context, R.color.colorNotebookSelected));
                    gradientDrawable.setCornerRadius(context.getResources().getDimension(R.dimen._30sdp));

                    GradientDrawable outline = new GradientDrawable();
                    outline.setShape(GradientDrawable.RECTANGLE);
                    outline.setColor(ContextCompat.getColor(context, R.color.colorNotebookSelectedBorder));
                    outline.setCornerRadius(context.getResources().getDimension(R.dimen._30sdp));

                    InsetDrawable insetDrawable = new InsetDrawable(gradientDrawable,
                            context.getResources().getDimensionPixelOffset(R.dimen._1sdp),
                            context.getResources().getDimensionPixelOffset(R.dimen._1sdp),
                            context.getResources().getDimensionPixelOffset(R.dimen._1sdp),
                            context.getResources().getDimensionPixelOffset(R.dimen._1sdp));

                    bgNotebookSelected = new LayerDrawable(new Drawable[] {outline, insetDrawable});
                }
                return bgNotebookSelected;
            } else {
                if (bgNotebook == null) {
                    GradientDrawable gradientDrawable = new GradientDrawable();
                    gradientDrawable.setShape(GradientDrawable.RECTANGLE);
                    gradientDrawable.setColor(ContextCompat.getColor(context, R.color.colorNotebook));
                    gradientDrawable.setCornerRadius(context.getResources().getDimension(R.dimen._30sdp));

                    int rippleColor = ContextCompat.getColor(context, R.color.colorNoteRipple);
                    RippleDrawable rippleDrawable = new RippleDrawable(
                            new ColorStateList(new int[][] { new int[] {} }, new int[] {rippleColor}),
                            gradientDrawable,
                            null
                    );

                    GradientDrawable outline = new GradientDrawable();
                    outline.setShape(GradientDrawable.RECTANGLE);
                    outline.setColor(ContextCompat.getColor(context, R.color.colorNotebookBorder));
                    outline.setCornerRadius(context.getResources().getDimension(R.dimen._30sdp));

                    InsetDrawable insetDrawable = new InsetDrawable(rippleDrawable,
                            context.getResources().getDimensionPixelOffset(R.dimen._1sdp),
                            context.getResources().getDimensionPixelOffset(R.dimen._1sdp),
                            context.getResources().getDimensionPixelOffset(R.dimen._1sdp),
                            context.getResources().getDimensionPixelOffset(R.dimen._1sdp));

                    bgNotebook = new LayerDrawable(new Drawable[] {outline, insetDrawable});
                }
                return bgNotebook;
            }
        }
    }

    private void showAddNotebookDialog() {
        if (dialogAddNotebook == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context).inflate(
                    R.layout.dialog_add_notebook,
                    (ViewGroup) ((Activity) context).findViewById(R.id.dialog_add_notebook_layout)
            );

            view.findViewById(R.id.dialog_add_notebook_indicator).setVisibility(View.GONE);
            view.setClipToOutline(true);
            builder.setView(view);
            dialogAddNotebook = builder.create();

            if (dialogAddNotebook.getWindow() != null) {
                dialogAddNotebook.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            TextInputLayout nameInputLayout = view.findViewById(R.id.dialog_add_notebook_input_layout);
            nameInputLayout.setBoxStrokeColor(ContextCompat.getColor(view.getContext(), R.color.colorAccent));
            nameInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            nameInputLayout.setErrorIconTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            nameInputLayout.setErrorTextColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));

            TextInputEditText nameEditText = view.findViewById(R.id.dialog_add_notebook_input_edit_text);

            nameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    nameInputLayout.setError(null);
                }
            });

            TextView addButton = view.findViewById(R.id.dialog_add_notebook_okay);
            TextView cancelButton = view.findViewById(R.id.dialog_add_notebook_cancel);

            addButton.setOnClickListener(v -> {

                if (nameEditText.length() > 0) {
                    view.findViewById(R.id.dialog_add_notebook_indicator).setVisibility(View.VISIBLE);
                    dialogAddNotebook.setCanceledOnTouchOutside(false);
                    dialogAddNotebook.setCancelable(false);
                    nameEditText.setFocusable(false);
                    nameEditText.setEnabled(false);
                    addButton.setEnabled(false);
                    addButton.setTextColor(Color.GRAY);
                    cancelButton.setEnabled(false);
                    cancelButton.setTextColor(Color.GRAY);


                    new Thread(() -> {
                        Notebook notebook = new Notebook();
                        notebook.setTitle(Objects.requireNonNull(nameEditText.getText()).toString());
                        notebook.setCreatedAt(new Date());

                        long id = NotesDatabase.getDatabase(context).notebookDao().insertNotebook(notebook);

                        ((Activity) context).runOnUiThread(() -> {
                            dialogAddNotebook.dismiss();
                            notebook.setId(id);
                            notebooks.add(notebook);
                            notifyItemInserted(notebooks.size());
                        });
                    }).start();
                } else {
                    nameInputLayout.setError(context.getString(R.string.field_is_empty));
                }
            });

            cancelButton.setOnClickListener(v -> {
                dialogAddNotebook.dismiss();
            });

            dialogAddNotebook.setOnDismissListener(dialog -> dialogAddNotebook = null);
        }

        dialogAddNotebook.show();
    }

    private void deleteNotebook(int position) {
        if (position > notebooks.size() || position == 0) return;

        new Thread(() -> {
            NotesDatabase.getDatabase(context).notebookDao().deleteNotebook(notebooks.get(position));
            notebooks.remove(position);

            ((Activity) context).runOnUiThread(() -> {
                mainNotebookView.performClick();
                notifyItemRemoved(position);
            });
        }).start();
    }

    private void showDeleteDialog(int position) {
        if (dialogDeleteNotebook == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context).inflate(
                    R.layout.dialog_delete_notebook,
                    (ViewGroup) ((Activity) context).findViewById(R.id.dialog_delete_notebook_layout)
            );

            builder.setView(view);
            dialogDeleteNotebook = builder.create();

            if (dialogDeleteNotebook.getWindow() != null) {
                dialogDeleteNotebook.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            TextView deleteButton = view.findViewById(R.id.dialog_delete_notebook_okay);
            TextView cancelButton = view.findViewById(R.id.dialog_delete_notebook_cancel);

            deleteButton.setOnClickListener(v -> {
                deleteNotebook(position);
                dialogDeleteNotebook.dismiss();
            });

            cancelButton.setOnClickListener(v -> {
                dialogDeleteNotebook.dismiss();
            });

            dialogDeleteNotebook.setOnDismissListener(dialog -> dialogDeleteNotebook = null);
        }

        dialogDeleteNotebook.show();
    }

    private void showEditDialog(int position) {
        if (dialogEditNotebook == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context).inflate(
                    R.layout.dialog_add_notebook,
                    (ViewGroup) ((Activity) context).findViewById(R.id.dialog_add_notebook_layout)
            );

            view.findViewById(R.id.dialog_add_notebook_indicator).setVisibility(View.GONE);
            view.setClipToOutline(true);
            ((ImageView) view.findViewById(R.id.dialog_add_notebook_icon)).setImageResource(R.drawable.ic_edit);
            ((TextView) view.findViewById(R.id.dialog_add_notebook_title)).setText(R.string.edit_notebook);
            ((TextView) view.findViewById(R.id.dialog_add_notebook_okay)).setText(R.string.edit);
            builder.setView(view);
            dialogEditNotebook = builder.create();

            if (dialogEditNotebook.getWindow() != null) {
                dialogEditNotebook.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            TextInputLayout nameInputLayout = view.findViewById(R.id.dialog_add_notebook_input_layout);
            nameInputLayout.setBoxStrokeColor(ContextCompat.getColor(view.getContext(), R.color.colorAccent));
            nameInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            nameInputLayout.setErrorIconTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));
            nameInputLayout.setErrorTextColor(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.colorRed)));

            TextInputEditText nameEditText = view.findViewById(R.id.dialog_add_notebook_input_edit_text);

            nameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    nameInputLayout.setError(null);
                }
            });

            TextView changeButton = view.findViewById(R.id.dialog_add_notebook_okay);
            TextView cancelButton = view.findViewById(R.id.dialog_add_notebook_cancel);

            changeButton.setOnClickListener(v -> {

                if (nameEditText.length() > 0) {
                    view.findViewById(R.id.dialog_add_notebook_indicator).setVisibility(View.VISIBLE);
                    dialogEditNotebook.setCanceledOnTouchOutside(false);
                    dialogEditNotebook.setCancelable(false);
                    nameEditText.setFocusable(false);
                    nameEditText.setEnabled(false);
                    changeButton.setEnabled(false);
                    changeButton.setTextColor(Color.GRAY);
                    cancelButton.setEnabled(false);
                    cancelButton.setTextColor(Color.GRAY);


                    new Thread(() -> {
                        Notebook notebook = notebooks.get(position);
                        notebook.setTitle(Objects.requireNonNull(nameEditText.getText()).toString());

                        NotesDatabase.getDatabase(context).notebookDao().insertNotebook(notebook);

                        ((Activity) context).runOnUiThread(() -> {
                            dialogEditNotebook.dismiss();
                            notebooks.remove(position);
                            notebooks.add(position, notebook);
                            notifyItemChanged(position);
                        });
                    }).start();
                } else {
                    nameInputLayout.setError(context.getString(R.string.field_is_empty));
                }
            });

            cancelButton.setOnClickListener(v -> {
                dialogEditNotebook.dismiss();
            });

            dialogEditNotebook.setOnDismissListener(dialog -> dialogEditNotebook = null);
        }

        dialogEditNotebook.show();
    }
}
