package com.zebra.rfidreader.demo.settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.zebra.rfidreader.demo.R;
import com.zebra.rfidreader.demo.application.Application;
import com.zebra.rfidreader.demo.common.Constants;
import com.zebra.rfidreader.demo.home.MainActivity;

import java.io.File;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * <p/>
 * Use the {@link ApplicationSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 * <p/>
 * Fragment to show the Connection Settings UI
 */
public class ApplicationSettingsFragment extends Fragment {

//    public static Hashtable<String, Boolean> applicationSettings;
//
//    static {
//        applicationSettings = new Hashtable<>();
//        applicationSettings.put(Constants.AUTO_RECONNECT_READERS, false);
//        applicationSettings.put(Constants.NOTIFY_READER_AVAILABLE, false);
//        applicationSettings.put(Constants.NOTIFY_READER_CONNECTION, false);
//        applicationSettings.put(Constants.NOTIFY_BATTERY_STATUS, false);
//        applicationSettings.put(Constants.EXPORT_DATA, false);
//    }

    private CheckBox autoReconnectReaders;
    private CheckBox readerAvailable;
    private CheckBox readerConnection;
    private CheckBox readerBattery;
    private CheckBox exportData;
    private CheckBox tagListMatchMode;
    private CheckBox tagListMatchTagNames;
    private CheckBox asciiMode;
    private SharedPreferences settings;

    public ApplicationSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ConnectionSettingsFragment.
     */
    public static ApplicationSettingsFragment newInstance() {
        return new ApplicationSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connection_settings, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onResume() {
        super.onResume();
        loadCheckBoxStates();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initializeViews();
    }

    private void initializeViews() {
        autoReconnectReaders = ((CheckBox) getActivity().findViewById(R.id.autoReconnectReaders));
        readerAvailable = (CheckBox) getActivity().findViewById(R.id.readerAvailable);
        readerConnection = ((CheckBox) getActivity().findViewById(R.id.readerConnection));
        readerBattery = ((CheckBox) getActivity().findViewById(R.id.readerBattery));
        exportData = ((CheckBox) getActivity().findViewById(R.id.exportData));
        tagListMatchMode = ((CheckBox) getActivity().findViewById(R.id.tagListMatchMode));
        tagListMatchTagNames = ((CheckBox) getActivity().findViewById(R.id.tagListMatchTagNames));
        asciiMode = ((CheckBox) getActivity().findViewById(R.id.asciiMode));

        if (Application.mIsInventoryRunning || Application.isLocatingTag) {
            tagListMatchMode.setEnabled(false);
            tagListMatchTagNames.setEnabled(false);
        }
        loadCheckBoxStates();
        //
        File root = Environment.getExternalStorageDirectory();
        final File dir = new File(root.getAbsolutePath() + Constants.RFID_FILE_DIR);
        tagListMatchMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    File file = new File(dir, Constants.TAG_MATCH_FILE_NAME);
                    if (!file.exists()) {
                        Toast.makeText(getActivity(), getString(R.string.REQUIRES_TAGLIST_CSV), Toast.LENGTH_SHORT).show();
                        tagListMatchMode.setChecked(false);
                    } else {
                        tagListMatchTagNames.setEnabled(true);
                    }
                } else {
                    // clear friendly names
//                    tagListMatchTagNames.setChecked(false);
                    tagListMatchTagNames.setEnabled(false);
                }
                MainActivity.clearInventoryData();
            }
        });
        tagListMatchTagNames.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                File file = new File(dir, Constants.TAG_MATCH_FILE_NAME);
                if (!file.exists()) {
                    Toast.makeText(getActivity(), getString(R.string.REQUIRES_TAGLIST_CSV), Toast.LENGTH_SHORT).show();
                    tagListMatchTagNames.setChecked(false);
                }
            }
        });

        asciiMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Application.asciiMode = isChecked;
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStop() {
        super.onStop();
        storeCheckBoxesStatus();
    }

    /**
     * Method to load the checkbox states
     */
    private void loadCheckBoxStates() {
        settings = getActivity().getSharedPreferences(Constants.APP_SETTINGS_STATUS, 0);
        autoReconnectReaders.setChecked(settings.getBoolean(Constants.AUTO_RECONNECT_READERS, true));
        readerAvailable.setChecked(settings.getBoolean(Constants.NOTIFY_READER_AVAILABLE, false));
        readerConnection.setChecked(settings.getBoolean(Constants.NOTIFY_READER_CONNECTION, false));
        if (Build.MODEL.contains("MC33"))
            readerBattery.setChecked(settings.getBoolean(Constants.NOTIFY_BATTERY_STATUS, false));
        else
            readerBattery.setChecked(settings.getBoolean(Constants.NOTIFY_BATTERY_STATUS, true));
        exportData.setChecked(settings.getBoolean(Constants.EXPORT_DATA, false));
        tagListMatchMode.setChecked(settings.getBoolean(Constants.TAG_LIST_MATCH_MODE, false));
        if (!tagListMatchMode.isChecked()) {
            tagListMatchTagNames.setEnabled(false);
        } else
            tagListMatchTagNames.setChecked(settings.getBoolean(Constants.SHOW_CSV_TAG_NAMES, false));

        Application.asciiMode = settings.getBoolean(Constants.ASCII_MODE, false);
        asciiMode.setChecked(Application.asciiMode);
    }

    /**
     * Method to store the checkbox states
     */
    private void storeCheckBoxesStatus() {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getActivity().getSharedPreferences(Constants.APP_SETTINGS_STATUS, 0);
        SharedPreferences.Editor editor = settings.edit();

        //Save the chechbox status
        editor.putBoolean(Constants.AUTO_RECONNECT_READERS, autoReconnectReaders.isChecked());
        editor.putBoolean(Constants.NOTIFY_READER_AVAILABLE, readerAvailable.isChecked());
        editor.putBoolean(Constants.NOTIFY_READER_CONNECTION, readerConnection.isChecked());
        editor.putBoolean(Constants.NOTIFY_BATTERY_STATUS, readerBattery.isChecked());
        editor.putBoolean(Constants.EXPORT_DATA, exportData.isChecked());
        editor.putBoolean(Constants.TAG_LIST_MATCH_MODE, tagListMatchMode.isChecked());
        editor.putBoolean(Constants.SHOW_CSV_TAG_NAMES, tagListMatchTagNames.isChecked());
        editor.putBoolean(Constants.ASCII_MODE, asciiMode.isChecked());

        // Commit the edits!
        editor.commit();

        //Update the preferences in the Application
        Application.AUTO_RECONNECT_READERS = ((CheckBox) getActivity().findViewById(R.id.autoReconnectReaders)).isChecked();
        Application.NOTIFY_READER_AVAILABLE = ((CheckBox) getActivity().findViewById(R.id.readerAvailable)).isChecked();
        Application.NOTIFY_READER_CONNECTION = ((CheckBox) getActivity().findViewById(R.id.readerConnection)).isChecked();
        Application.NOTIFY_BATTERY_STATUS = ((CheckBox) getActivity().findViewById(R.id.readerBattery)).isChecked();
        Application.EXPORT_DATA = ((CheckBox) getActivity().findViewById(R.id.exportData)).isChecked();
        Application.TAG_LIST_MATCH_MODE = ((CheckBox) getActivity().findViewById(R.id.tagListMatchMode)).isChecked();
        Application.SHOW_CSV_TAG_NAMES = ((CheckBox) getActivity().findViewById(R.id.tagListMatchTagNames)).isChecked();
    }
}
