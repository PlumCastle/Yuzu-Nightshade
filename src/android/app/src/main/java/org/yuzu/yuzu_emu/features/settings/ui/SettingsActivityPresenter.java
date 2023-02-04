package org.yuzu.yuzu_emu.features.settings.ui;

import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;

import org.yuzu.yuzu_emu.NativeLibrary;
import org.yuzu.yuzu_emu.features.settings.model.Settings;
import org.yuzu.yuzu_emu.features.settings.utils.SettingsFile;
import org.yuzu.yuzu_emu.utils.DirectoryInitialization;
import org.yuzu.yuzu_emu.utils.DirectoryInitialization.DirectoryInitializationState;
import org.yuzu.yuzu_emu.utils.DirectoryStateReceiver;
import org.yuzu.yuzu_emu.utils.Log;
import org.yuzu.yuzu_emu.utils.ThemeUtil;

import java.io.File;

public final class SettingsActivityPresenter {
    private static final String KEY_SHOULD_SAVE = "should_save";

    private SettingsActivityView mView;

    private Settings mSettings = new Settings();

    private boolean mShouldSave;

    private DirectoryStateReceiver directoryStateReceiver;

    private String menuTag;
    private String gameId;

    public SettingsActivityPresenter(SettingsActivityView view) {
        mView = view;
    }

    public void onCreate(Bundle savedInstanceState, String menuTag, String gameId) {
        if (savedInstanceState == null) {
            this.menuTag = menuTag;
            this.gameId = gameId;
        } else {
            mShouldSave = savedInstanceState.getBoolean(KEY_SHOULD_SAVE);
        }
    }

    public void onStart() {
        prepareDirectoriesIfNeeded();
    }

    void loadSettingsUI() {
        if (mSettings.isEmpty()) {
            if (!TextUtils.isEmpty(gameId)) {
                mSettings.loadSettings(gameId, mView);
            } else {
                mSettings.loadSettings(mView);
            }
        }

        mView.showSettingsFragment(menuTag, false, gameId);
        mView.onSettingsFileLoaded(mSettings);
    }

    private void prepareDirectoriesIfNeeded() {
        File configFile = new File(DirectoryInitialization.getUserDirectory() + "/config/" + SettingsFile.FILE_NAME_CONFIG + ".ini");
        if (!configFile.exists()) {
            Log.error(DirectoryInitialization.getUserDirectory() + "/config/" + SettingsFile.FILE_NAME_CONFIG + ".ini");
            Log.error("yuzu config file could not be found!");
        }
        if (DirectoryInitialization.areDirectoriesReady()) {
            loadSettingsUI();
        } else {
            mView.showLoading();
            IntentFilter statusIntentFilter = new IntentFilter(
                    DirectoryInitialization.BROADCAST_ACTION);

            directoryStateReceiver =
                    new DirectoryStateReceiver(directoryInitializationState ->
                    {
                        if (directoryInitializationState == DirectoryInitializationState.YUZU_DIRECTORIES_INITIALIZED) {
                            mView.hideLoading();
                            loadSettingsUI();
                        } else if (directoryInitializationState == DirectoryInitializationState.CANT_FIND_EXTERNAL_STORAGE) {
                            mView.showExternalStorageNotMountedHint();
                            mView.hideLoading();
                        }
                    });

            mView.startDirectoryInitializationService(directoryStateReceiver, statusIntentFilter);
        }
    }

    public void setSettings(Settings settings) {
        mSettings = settings;
    }

    public Settings getSettings() {
        return mSettings;
    }

    public void onStop(boolean finishing) {
        if (directoryStateReceiver != null) {
            mView.stopListeningToDirectoryInitializationService(directoryStateReceiver);
            directoryStateReceiver = null;
        }

        if (mSettings != null && finishing && mShouldSave) {
            Log.debug("[SettingsActivity] Settings activity stopping. Saving settings to INI...");
            mSettings.saveSettings(mView);
        }

        ThemeUtil.applyTheme();

        NativeLibrary.ReloadSettings();
    }

    public void onSettingChanged() {
        mShouldSave = true;
    }

    public void saveState(Bundle outState) {
        outState.putBoolean(KEY_SHOULD_SAVE, mShouldSave);
    }
}
