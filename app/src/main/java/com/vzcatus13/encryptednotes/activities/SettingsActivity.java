package com.vzcatus13.encryptednotes.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.vzcatus13.encryptednotes.R;
import com.vzcatus13.encryptednotes.utils.BackupUtils;
import com.vzcatus13.encryptednotes.utils.Utils;
import com.google.android.material.snackbar.Snackbar;


import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    private final static int REQUEST_WRITE_PERMISSION = 1;
    private final static int REQUEST_READ_PERMISSION = 2;

    private final static String TAG = "SettingsActivity";

    public final static String KEY_THEME = "KEY_THEME";
    public final static String THEME_AUTO = "THEME_AUTO";
    public final static String THEME_LIGHT = "THEME_LIGHT";
    public final static String THEME_NIGHT = "THEME_NIGHT";

    public final static String KEY_VIEW_GRID = "KEY_VIEW_GRID";

    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsFragment = new SettingsFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_frame, settingsFragment)
                .commit();

        Utils.setTransparentStatusAndNavigation(getWindow(), false, ContextCompat.getColor(SettingsActivity.this, R.color.colorPrimary), true, true);

        findViewById(R.id.settings_back_btn).setOnClickListener( v -> onBackPressed());

        initPreferenceChangeListener();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        AlertDialog dialogRestore;
        private static ActivityResultLauncher<Intent> getDbFileRequest;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // This is a request for DB file. Called in restore process
            getDbFileRequest = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri dbRestoreFileUri = result.getData().getData();
                            if (dbRestoreFileUri != null) {
                                try {
                                    boolean isRestored = BackupUtils.restoreDatabase(requireContext(), dbRestoreFileUri);
                                    if (isRestored) {
                                        Intent restartIntent = requireActivity().getBaseContext().getPackageManager().getLaunchIntentForPackage(requireActivity().getBaseContext().getPackageName());
                                        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        requireActivity().finish();
                                        startActivity(restartIntent);
                                        System.exit(0);
                                    } else {
                                        Snackbar.make(requireActivity().findViewById(R.id.settings_frame), R.string.restore_error, Snackbar.LENGTH_SHORT)
                                                .setTextColor(ContextCompat.getColor(requireContext(), R.color.colorText))
                                                .show();
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage());
                                    Snackbar.make(requireActivity().findViewById(R.id.settings_frame), R.string.restore_error, Snackbar.LENGTH_SHORT)
                                            .setTextColor(ContextCompat.getColor(requireContext(), R.color.colorText))
                                            .show();
                                }
                            }
                        }
                    });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getPreferenceManager().getContext();
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

            PreferenceCategory displayCategory = new PreferenceCategory(context);
            displayCategory.setTitle(R.string.settings_display);
            displayCategory.setIconSpaceReserved(false);

            ListPreference nightModeList = new ListPreference(context);
            nightModeList.setKey(KEY_THEME);
            nightModeList.setTitle(R.string.title_theme);
            nightModeList.setDialogTitle(R.string.title_theme_dialog);
            nightModeList.setSummary(R.string.summary_theme);
            nightModeList.setIconSpaceReserved(false);
            // If version >= Q, then it supports auto dark theme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                nightModeList.setEntries(new String[] {getString(R.string.system_theme), getString(R.string.light_theme), getString(R.string.night_theme)});
                nightModeList.setEntryValues(new String[] {THEME_AUTO, THEME_LIGHT, THEME_NIGHT});
            } else {
                nightModeList.setEntries(new String[] {getString(R.string.light_theme), getString(R.string.night_theme)});
                nightModeList.setEntryValues(new String[] {THEME_LIGHT, THEME_NIGHT});
            }
            nightModeList.setValueIndex(0);

            CheckBoxPreference gridViewCheckBox = new CheckBoxPreference(context);
            gridViewCheckBox.setKey(KEY_VIEW_GRID);
            gridViewCheckBox.setTitle(R.string.title_grid_view);
            gridViewCheckBox.setSummary(R.string.summary_grid_view);
            gridViewCheckBox.setChecked(true);
            gridViewCheckBox.setIconSpaceReserved(false);

            PreferenceCategory backupCategory = new PreferenceCategory(context);
            backupCategory.setTitle(R.string.settings_backup);
            backupCategory.setIconSpaceReserved(false);

            Preference backup = new Preference(context);
            backup.setTitle(R.string.title_backup);
            backup.setSummary(R.string.summary_backup);
            backup.setIconSpaceReserved(false);
            backup.setOnPreferenceClickListener(preference -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            requireActivity(),
                            new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_PERMISSION
                    );
                } else {
                    String result = BackupUtils.backupDatabase(requireContext());
                    Snackbar.make(requireActivity().findViewById(R.id.settings_frame), getString(R.string.backup_saved_to) + "\n" + result, Snackbar.LENGTH_SHORT)
                            .setTextColor(ContextCompat.getColor(requireContext(), R.color.colorText))
                            .show();
                }
                return true;
            });

            Preference restore = new Preference(context);
            restore.setTitle(R.string.title_restore);
            restore.setSummary(R.string.summary_restore);
            restore.setIconSpaceReserved(false);
            restore.setOnPreferenceClickListener(preference -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            requireActivity(),
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_READ_PERMISSION
                    );
                } else {
                    showRestoreDialog();
                }
                return true;
            });


            screen.addPreference(displayCategory);
            screen.addPreference(backupCategory);
            displayCategory.addPreference(nightModeList);
            displayCategory.addPreference(gridViewCheckBox);
            backupCategory.addPreference(backup);
            backupCategory.addPreference(restore);

            setPreferenceScreen(screen);

            // TODO: add auto backup
        }

        private void showRestoreDialog() {
            if (dialogRestore == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                View view = LayoutInflater.from(getContext()).inflate(
                        R.layout.dialog_backup_resotre,
                        requireActivity().findViewById(R.id.dialog_backup_restore_layout)
                );

                View indicator = view.findViewById(R.id.dialog_backup_restore_indicator);
                indicator.setVisibility(View.GONE);
                ((ImageView) view.findViewById(R.id.dialog_backup_restore_icon)).setImageResource(R.drawable.ic_restore);
                ((TextView) view.findViewById(R.id.dialog_backup_restore_title)).setText(R.string.title_restore);
                ((TextView) view.findViewById(R.id.dialog_backup_restore_text)).setText(R.string.restore_dialog_text);
                ((TextView) view.findViewById(R.id.dialog_backup_restore_warning)).setText(R.string.restore_dialog_warning);
                TextView restoreButton = view.findViewById(R.id.dialog_backup_restore_okay);
                restoreButton.setText(R.string.title_restore);
                TextView cancelButton = view.findViewById(R.id.dialog_backup_restore_cancel);
                cancelButton.setText(R.string.cancel);

                builder.setView(view);
                dialogRestore = builder.create();

                if (dialogRestore.getWindow() != null) {
                    dialogRestore.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                }

                restoreButton.setOnClickListener(v -> {
                    indicator.setVisibility(View.VISIBLE);
                    cancelButton.setTextColor(Color.GRAY);
                    restoreButton.setTextColor(Color.GRAY);
                    cancelButton.setEnabled(false);
                    restoreButton.setEnabled(false);
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    dialogRestore.dismiss();
                    if (getDbFileRequest != null) {
                        getDbFileRequest.launch(intent);
                    }
                });

                cancelButton.setOnClickListener(v -> dialogRestore.cancel());


                dialogRestore.setOnCancelListener(DialogInterface::dismiss);
                dialogRestore.setOnDismissListener(dialog -> dialogRestore = null);
            }

            dialogRestore.show();
        }
    }

    private void initPreferenceChangeListener() {
        if (onSharedPreferenceChangeListener == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
            onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
                if (key.equalsIgnoreCase(KEY_THEME)) {
                    String mode = sharedPreferences.getString(key, THEME_AUTO);
                    switch (mode) {
                        case THEME_AUTO:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
                        case THEME_LIGHT:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            break;
                        case THEME_NIGHT:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            break;
                    }
                    recreate();
                }
            };

            preferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String result = BackupUtils.backupDatabase(SettingsActivity.this);
                Snackbar.make(findViewById(R.id.settings_frame), getString(R.string.backup_saved_to) + "\n" + result, Snackbar.LENGTH_SHORT)
                        .setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.colorText))
                        .show();
            } else {
                Snackbar.make(findViewById(R.id.settings_frame), R.string.permission_denied, Snackbar.LENGTH_SHORT)
                        .setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.colorText))
                        .show();
            }
        } else if (requestCode == REQUEST_READ_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                settingsFragment.showRestoreDialog();
            } else {
                Snackbar.make(findViewById(R.id.settings_frame), R.string.permission_denied, Snackbar.LENGTH_SHORT)
                        .setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.colorText))
                        .show();
            }
        }
    }
}
