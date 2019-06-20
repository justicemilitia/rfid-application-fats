package com.zebra.rfidreader.demo.home;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.BATCH_MODE;
import com.zebra.rfid.api3.BEEPER_VOLUME;
import com.zebra.rfid.api3.DYNAMIC_POWER_OPTIMIZATION;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.Events;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.LOCK_DATA_FIELD;
import com.zebra.rfid.api3.LOCK_PRIVILEGE;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.PreFilters;
import com.zebra.rfid.api3.RFIDResults;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TAG_FIELD;
import com.zebra.rfid.api3.TagAccess;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.VersionInfo;
import com.zebra.rfidreader.demo.R;
import com.zebra.rfidreader.demo.access_operations.AccessOperationsFragment;
import com.zebra.rfidreader.demo.access_operations.AccessOperationsLockFragment;
import com.zebra.rfidreader.demo.application.Application;
import com.zebra.rfidreader.demo.common.Constants;
import com.zebra.rfidreader.demo.common.CustomProgressDialog;
import com.zebra.rfidreader.demo.common.CustomToast;
import com.zebra.rfidreader.demo.common.Inventorytimer;
import com.zebra.rfidreader.demo.common.LinkProfileUtil;
import com.zebra.rfidreader.demo.common.MatchModeFileLoader;
import com.zebra.rfidreader.demo.common.ResponseHandlerInterfaces;
import com.zebra.rfidreader.demo.common.ResponseHandlerInterfaces.BatteryNotificationHandler;
import com.zebra.rfidreader.demo.common.ResponseHandlerInterfaces.ReaderDeviceFoundHandler;
import com.zebra.rfidreader.demo.common.ResponseHandlerInterfaces.TriggerEventHandler;
import com.zebra.rfidreader.demo.common.asciitohex;
import com.zebra.rfidreader.demo.data_export.DataExportTask;
import com.zebra.rfidreader.demo.inventory.InventoryFragment;
import com.zebra.rfidreader.demo.inventory.InventoryListItem;
import com.zebra.rfidreader.demo.locate_tag.LocationingFragment;
import com.zebra.rfidreader.demo.locate_tag.RangeGraph;
import com.zebra.rfidreader.demo.notifications.NotificationsService;
import com.zebra.rfidreader.demo.rapidread.RapidReadFragment;
import com.zebra.rfidreader.demo.reader_connection.ReadersListFragment;
import com.zebra.rfidreader.demo.settings.AdvancedOptionItemFragment;
import com.zebra.rfidreader.demo.settings.AdvancedOptionsContent;
import com.zebra.rfidreader.demo.settings.BackPressedFragment;
import com.zebra.rfidreader.demo.settings.PreFilterFragment;
import com.zebra.rfidreader.demo.settings.ProfileContent;
import com.zebra.rfidreader.demo.settings.ProfileFragment;
import com.zebra.rfidreader.demo.settings.SettingListFragment;
import com.zebra.rfidreader.demo.settings.SettingsDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static com.zebra.rfidreader.demo.application.Application.ActiveProfile;
import static com.zebra.rfidreader.demo.application.Application.LAST_CONNECTED_READER;
import static com.zebra.rfidreader.demo.settings.AdvancedOptionsContent.DPO_ITEM_INDEX;
import static com.zebra.rfidreader.demo.settings.ProfileContent.UpdateActiveProfile;
import static com.zebra.rfidreader.demo.settings.ProfileContent.UpdateProfilesForRegulatory;
import static java.util.concurrent.TimeUnit.SECONDS;


public class MainActivity extends AppCompatActivity implements Readers.RFIDReaderEventHandler, NavigationView.OnNavigationItemSelectedListener {
    //Tag to identify the currently displayed fragment
    protected static final String TAG_CONTENT_FRAGMENT = "ContentFragment";
    //Messages for progress bar
    private static final String MSG_READ = "Reading Tags";
    private static final String MSG_WRITE = "Writing Data";
    private static final String MSG_LOCK = "Executing Lock Command";
    private static final String MSG_KILL = "Executing Kill Command";
    private static final int BEEP_DELAY_TIME_MIN = 0;
    private static final int BEEP_DELAY_TIME_MAX = 300;

    /**
     * method to start a timer task for LED glow for the duration of 10ms
     */
    public static Timer tLED;
    private static ArrayList<ReaderDeviceFoundHandler> readerDeviceFoundHandlers = new ArrayList<>();
    private static ArrayList<BatteryNotificationHandler> batteryNotificationHandlers = new ArrayList<>();

    /**
     * method to start a timer task to beep for the duration of 10ms
     */
    public Timer tbeep;
    /**
     * method to start a timer task to beep for locate functionality and configure the ON OFF duration.
     */
    public Timer locatebeep;
    protected Boolean isInventoryAborted;
    protected boolean isInventoryAbortedNotifier;
    protected boolean isLocationingAborted;
    protected int accessTagCount;
    //To indicate indeterminate progress
    protected CustomProgressDialog progressDialog;
    protected Menu menu;
    NotificationManager notificationManager;
    MediaPlayer mPlayer;
    //Special layout for Navigation Drawer
    private DrawerLayout mDrawerLayout;
    //List view for navigation drawer items
    private CharSequence mTitle;
    private String[] mOptionTitles;
    private Boolean isTriggerRepeat;
    private boolean pc = false;
    private boolean rssi = false;
    private boolean phase = false;
    private boolean channelIndex = false;
    private boolean tagSeenCount = false;
    private boolean isDeviceDisconnected = false;
    private AsyncTask<Void, Void, Boolean> DisconnectTask;

    //for beep and LED
    private boolean beepON = false;
    private boolean beepONLocate = false;
    private String TAG = "RFIDDEMO";
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 10;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_CSV = 11;
    private AsyncTask<Void, Void, Boolean> AutoConnectDeviceTask;
    private ScanEnableTask scanEnableTask;

    //common Result Intent broadcasted by DataWedge
    private static final String DW_APIRESULT_ACTION = "com.symbol.datawedge.api.RESULT_ACTION";
    private static final String scanner_status = "com.symbol.datawedge.scanner_status";
    private String RFID_DEMO_SCANNER_DISABLE = "RFID_DEMO_SCANNER_DISABLE";
    private NavigationView navigationView;

    /**
     * method to know whether bluetooth is enabled or not
     *
     * @return - true if bluetooth enabled
     * - false if bluetooth disabled
     */
    public static boolean isBluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    /**
     * Method for registering the classes for device events like paired,unpaired, connected and disconnected.
     * The registered classes will get notified when device event occurs.
     *
     * @param readerDeviceFoundHandler - handler class to register with base receiver activity
     */
    public static void addReaderDeviceFoundHandler(ReaderDeviceFoundHandler readerDeviceFoundHandler) {
        readerDeviceFoundHandlers.add(readerDeviceFoundHandler);
    }

    public static void addBatteryNotificationHandler(BatteryNotificationHandler batteryNotificationHandler) {
        batteryNotificationHandlers.add(batteryNotificationHandler);
    }

    public static void removeReaderDeviceFoundHandler(ReaderDeviceFoundHandler readerDeviceFoundHandler) {
        readerDeviceFoundHandlers.remove(readerDeviceFoundHandler);
    }

    public static void removeBatteryNotificationHandler(BatteryNotificationHandler batteryNotificationHandler) {
        batteryNotificationHandlers.remove(batteryNotificationHandler);
    }

    /**
     * method to start a timer task to get device battery status per every 60 seconds
     */
    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?> taskHandle;

    public static void startTimer() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
            final Runnable task = new Runnable() {
                public void run() {
                    try {
                        if (Application.mConnectedReader != null)
                            Application.mConnectedReader.Config.getDeviceStatus(true, true, false);
                        else
                            stopTimer();
                    } catch (InvalidUsageException e) {
                        e.printStackTrace();
                    } catch (OperationFailureException e) {
                        e.printStackTrace();
                    }
                }
            };
            taskHandle = scheduler.scheduleAtFixedRate(task, 0, 60, SECONDS);
        }
    }

    /**
     * method to clear reader's settings on disconnection
     */
    public static void clearSettings() {
        Application.antennaPowerLevel = null;
        Application.antennaRfConfig = null;
        Application.singulationControl = null;
        Application.rfModeTable = null;
        Application.regulatory = null;
        Application.batchMode = -1;
        Application.tagStorageSettings = null;
        Application.reportUniquetags = null;
        Application.dynamicPowerSettings = null;
        Application.settings_startTrigger = null;
        Application.settings_stopTrigger = null;
        // Application.beeperVolume = BEEPER_VOLUME.HIGH_BEEP;
        Application.preFilters = null;
        if (Application.versionInfo != null)
            Application.versionInfo.clear();
        Application.regionNotSet = false;
        Application.isBatchModeInventoryRunning = null;
        Application.BatteryData = null;
        Application.is_disconnection_requested = false;
        Application.mConnectedDevice = null;
//        Application.mConnectedReader = null;
    }

    /**
     * method to stop timer
     */
    public static void stopTimer() {
        if (taskHandle != null) {
            taskHandle.cancel(true);
            scheduler.shutdown();
        }
        taskHandle = null;
        scheduler = null;
    }

    public static void UpdateReaderConnection(Boolean fullUpdate) throws InvalidUsageException, OperationFailureException {

        if (fullUpdate)
            Application.mConnectedReader.PostConnectReaderUpdate();

        Application.mConnectedReader.Events.setBatchModeEvent(true);
        Application.mConnectedReader.Events.setReaderDisconnectEvent(true);
        Application.mConnectedReader.Events.setInventoryStartEvent(true);
        Application.mConnectedReader.Events.setInventoryStopEvent(true);
        Application.mConnectedReader.Events.setTagReadEvent(true);
        Application.mConnectedReader.Events.setHandheldEvent(true);
        Application.mConnectedReader.Events.setBatteryEvent(true);
        Application.mConnectedReader.Events.setPowerEvent(true);
        Application.mConnectedReader.Events.setOperationEndSummaryEvent(true);


        Application.regulatory = Application.mConnectedReader.Config.getRegulatoryConfig();
        Application.regionNotSet = false;
        Application.rfModeTable = Application.mConnectedReader.ReaderCapabilities.RFModes.getRFModeTableInfo(0);
        LinkProfileUtil.getInstance().populateLinkeProfiles();
        //
        LoadProfileToReader();
        //
        Application.antennaRfConfig = Application.mConnectedReader.Config.Antennas.getAntennaRfConfig(1);
        Application.singulationControl = Application.mConnectedReader.Config.Antennas.getSingulationControl(1);
        Application.settings_startTrigger = Application.mConnectedReader.Config.getStartTrigger();
        Application.settings_stopTrigger = Application.mConnectedReader.Config.getStopTrigger();
        Application.tagStorageSettings = Application.mConnectedReader.Config.getTagStorageSettings();
        Application.dynamicPowerSettings = Application.mConnectedReader.Config.getDPOState();
        if (Application.mConnectedReader.getHostName().startsWith("RFD8500")) {
            Application.beeperVolume = BEEPER_VOLUME.QUIET_BEEP;
            Application.sledBeeperVolume = Application.mConnectedReader.Config.getBeeperVolume();
            Application.batchMode = Application.mConnectedReader.Config.getBatchModeConfig().getValue();
        }
        Application.reportUniquetags = Application.mConnectedReader.Config.getUniqueTagReport();
        Application.mConnectedReader.Config.getDeviceVersionInfo(Application.versionInfo);
        Application.mConnectedReader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);
        Application.mConnectedReader.Config.setLedBlinkEnable(Application.ledState);
        Application.mConnectedReader.Config.getDeviceStatus(true, false, false);
        //
        if (ActiveProfile.content.equals("Reader Defined")) {
            UpdateActiveProfile();
        }
        VersionInfo sdkversion = Application.mConnectedReader.versionInfo();
        Log.d("DEMOAPP", "SDK version " + sdkversion.getVersion());
        startTimer();
    }

    private static void LoadProfileToReader() {
        // update profiles based on reader type
        UpdateProfilesForRegulatory();
        if (!ActiveProfile.content.equals("Reader Defined")) {
            try {
                // Antenna
                Antennas.AntennaRfConfig antennaRfConfig;
                antennaRfConfig = Application.mConnectedReader.Config.Antennas.getAntennaRfConfig(1);
                antennaRfConfig.setTransmitPowerIndex(ActiveProfile.powerLevel);
                antennaRfConfig.setrfModeTableIndex(LinkProfileUtil.getInstance().getSimpleProfileModeIndex(ActiveProfile.LinkProfileIndex));
                Application.mConnectedReader.Config.Antennas.setAntennaRfConfig(1, antennaRfConfig);
                Application.antennaRfConfig = antennaRfConfig;

                // Singulation
                Antennas.SingulationControl singulationControl;
                singulationControl = Application.mConnectedReader.Config.Antennas.getSingulationControl(1);
                singulationControl.setSession(SESSION.GetSession(ActiveProfile.SessionIndex));

                if (ActiveProfile.id.equals("0"))
                    singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_AB_FLIP);
                else if (!ActiveProfile.id.equals("5"))
                    singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                // honour the prefilter over profile
                if (Application.mConnectedReader.Actions.PreFilters.length() > 0) {
                    PreFilters.PreFilter prefilter1 = Application.mConnectedReader.Actions.PreFilters.getPreFilter(0);
                    if (prefilter1 != null) {
                        if (Application.NON_MATCHING)
                            singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                        else
                            singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_B);
                    }
                }
                Application.mConnectedReader.Config.Antennas.setSingulationControl(1, singulationControl);
                Application.singulationControl = singulationControl;

                // DPO
                if (ActiveProfile.DPO_On)
                    Application.mConnectedReader.Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.ENABLE);
                else
                    Application.mConnectedReader.Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.DISABLE);

                Application.dynamicPowerSettings = Application.mConnectedReader.Config.getDPOState();
            } catch (InvalidUsageException e) {
            } catch (OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    private static FragmentManager supportmanager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTitle = getTitle();
        mOptionTitles = getResources().getStringArray(R.array.options_array);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        android.support.v7.app.ActionBarDrawerToggle toggle = new android.support.v7.app.ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        supportmanager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            selectItem(0);
            Application.eventHandler = new EventHandler();
            initializeConnectionSettings();
        } else {
            try {
                Application.mConnectedReader.Events.removeEventsListener(Application.eventHandler);
                Application.eventHandler = new EventHandler();
                Application.mConnectedReader.Events.addEventsListener(Application.eventHandler);
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
            }
        }

        Inventorytimer.getInstance().setActivity(this);

        if (Application.readers == null) {
            Application.readers = new Readers(this, ENUM_TRANSPORT.SERVICE_SERIAL);
        }
        Application.readers.attach(this);

        // Create a filter for the broadcast intent
        IntentFilter filter = new IntentFilter();
//        filter.addAction(scanner_status);
        filter.addAction(ACTION_SCREEN_OFF);
        filter.addAction(ACTION_SCREEN_ON);
        filter.addAction(DW_APIRESULT_ACTION);
        filter.addCategory("android.intent.category.DEFAULT");
        registerReceiver(BroadcastReceiver, filter);

        if (savedInstanceState == null) {
            loadReaders(this);
            // creates DW profile for Demo application
            createDWProfile();
        } else if (Application.AUTO_RECONNECT_READERS)
            AutoConnectDevice(this);
    }


    private void loadReaders(final MainActivity context) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                InvalidUsageException invalidUsageException = null;
                try {
                    ArrayList<ReaderDevice> readersListArray = Application.readers.GetAvailableRFIDReaderList();
                } catch (InvalidUsageException e) {
                    e.printStackTrace();
                    invalidUsageException = e;
                }
                if (invalidUsageException != null) {
                    Application.readers.Dispose();
                    Application.readers = null;
                    if (!isBluetoothEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivity(enableIntent);
                    }
                    Application.readers = new Readers(context, ENUM_TRANSPORT.BLUETOOTH);
                    Application.readers.attach(context);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (Application.AUTO_RECONNECT_READERS && Application.mConnectedDevice == null) {
                    AutoConnectDevice(context);
                }
            }
        }.execute();
    }

    private void ledsettigs() {
        SharedPreferences sharedPref = this.getSharedPreferences("LEDPreferences", Context.MODE_PRIVATE);
        Application.ledState = sharedPref.getBoolean("LED_STATE1", true);

    }

    private void beeperSettings() {

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.pref_beeper), Context.MODE_PRIVATE);
        int volume = sharedPref.getInt(getString(R.string.beeper_volume), 0);
        int streamType = AudioManager.STREAM_DTMF;
        int percantageVolume = 100;

        if (volume == 0) {
            Application.beeperVolume = BEEPER_VOLUME.HIGH_BEEP;
            percantageVolume = 100;
        }
        if (volume == 1) {
            Application.beeperVolume = BEEPER_VOLUME.MEDIUM_BEEP;
            percantageVolume = 75;
        }
        if (volume == 2) {
            Application.beeperVolume = BEEPER_VOLUME.LOW_BEEP;
            percantageVolume = 50;
        }
        if (volume == 3) {
            Application.beeperVolume = BEEPER_VOLUME.QUIET_BEEP;
            percantageVolume = 0;
        }
        Application.toneGenerator = new ToneGenerator(streamType, percantageVolume);
    }

    private String getReaderPassword(String address) {
        SharedPreferences sharedPreferences = this.getSharedPreferences(Constants.READER_PASSWORDS, 0);
        return sharedPreferences.getString(address, null);
    }

    // Autoconnect reader on detatch and attach or reader reboot.
    public void AutoConnectDevice(final Context activity) {
        AutoConnectDeviceTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onCancelled() {
                super.onCancelled();
                AutoConnectDeviceTask = null;
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
            }

            OperationFailureException exception;
            InvalidUsageException exceptionIN;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    int Prevreader;
                    if (Application.readers != null && Application.mConnectedReader == null/* && LAST_CONNECTED_READER.startsWith("RFD8500")*/) {
                        if (Application.readers.GetAvailableRFIDReaderList() != null) {
                            ArrayList<ReaderDevice> readersListArray = Application.readers.GetAvailableRFIDReaderList();
                            if (readersListArray.size() != 0 && readersListArray.size() <= 1) {
                                Application.mConnectedDevice = readersListArray.get(0);
                            } else {

                                for (Prevreader = 0; Prevreader < readersListArray.size(); Prevreader++) {
                                    if (readersListArray.get(Prevreader).getName().equals(LAST_CONNECTED_READER)) {
                                        if (readersListArray.size() != 0) {
                                            Application.mConnectedDevice = readersListArray.get(Prevreader);
                                            break;
                                        }
                                    } else {
                                        continue;
                                    }
                                }
                            }
                            if (Application.mConnectedDevice != null) {
                                Application.mConnectedReader = Application.mConnectedDevice.getRFIDReader();
                                try {
                                    if (!Application.mConnectedReader.isConnected() && !this.isCancelled()) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                String prgressMsg = "Connecting with " + Application.mConnectedReader.getHostName();
                                                if (Application.contextSettingDetails != null)
                                                    progressDialog = new CustomProgressDialog(Application.contextSettingDetails, prgressMsg);
                                                else
                                                    progressDialog = new CustomProgressDialog((MainActivity) activity, prgressMsg);
                                                progressDialog.show();
                                            }
                                        });
                                        String password = getReaderPassword(Application.mConnectedDevice.getName());
                                        if (password != null)
                                            Application.mConnectedReader.setPassword(password);
                                        Application.mConnectedReader.connect();
                                        StoreConnectedReader();
                                    } else {
                                        this.cancel(true);
                                    }
                                } catch (NullPointerException e) {
                                    Log.d(TAG, "null pointer ");
                                    e.printStackTrace();
                                } catch (InvalidUsageException e) {
                                    e.printStackTrace();
                                } //catch (OperationFailureException e) {
//                                    e.printStackTrace();
//                                    exception = e;
//                                }
                                try {
                                    if (Application.mConnectedReader.Events != null) {
                                        Application.mConnectedReader.Events.addEventsListener(Application.eventHandler);
                                    }
                                } catch (InvalidUsageException e) {
                                    e.printStackTrace();
                                } catch (OperationFailureException e) {
                                    e.printStackTrace();
                                } catch (NullPointerException e) {
                                    Log.d(TAG, "null pointer ");
                                    e.printStackTrace();
                                }
                                if (exception == null) {
                                    try {
                                        MainActivity.UpdateReaderConnection(true);
                                    } catch (InvalidUsageException e) {
                                        e.printStackTrace();
                                    } catch (OperationFailureException e) {
                                        e.printStackTrace();
                                    } catch (NullPointerException e) {
                                        Log.d(TAG, "null pointer ");
                                        e.printStackTrace();
                                    }
                                } else {
                                    MainActivity.clearSettings();
                                }
                            }
                        }
                    }
                } catch (InvalidUsageException ex) {
                    exceptionIN = ex;
                } catch (OperationFailureException e) {
                    e.printStackTrace();
                    exception = e;
                }
                return null;
            }


            @Override
            protected void onPostExecute(Boolean result) {
                if (exception != null) {
                    if (exception.getResults() == RFIDResults.RFID_READER_REGION_NOT_CONFIGURED) {
                        try {
                            Application.mConnectedReader.Events.addEventsListener(Application.eventHandler);
                        } catch (InvalidUsageException e) {
                            e.printStackTrace();
                        } catch (OperationFailureException e) {
                            e.printStackTrace();

                        }
                        ReaderDeviceConnected(Application.mConnectedDevice);
                        Application.regionNotSet = true;
                        sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, getString(R.string.set_region_msg));
                        Intent detailsIntent = new Intent(getApplicationContext(), SettingsDetailActivity.class);
                        detailsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        detailsIntent.putExtra(Constants.SETTING_ITEM_ID, R.id.regulatory);
                        startActivity(detailsIntent);

                    } else if (exception.getResults() == RFIDResults.RFID_BATCHMODE_IN_PROGRESS) {
                        Application.isBatchModeInventoryRunning = true;
                        Application.mIsInventoryRunning = true;
                        ReaderDeviceConnected(Application.mConnectedDevice);
                        if (Application.NOTIFY_READER_CONNECTION)
                            sendNotification(Constants.ACTION_READER_CONNECTED, "Connected to " + Application.mConnectedDevice.getName());
                        try {
                            if (Application.mConnectedReader.Events != null) {
                                Application.mConnectedReader.Events.addEventsListener(Application.eventHandler);
                            }
                            //MainActivity.UpdateReaderConnection(false);
                            Application.mConnectedReader.Events.setBatchModeEvent(true);
                            Application.mConnectedReader.Events.setReaderDisconnectEvent(true);
                            Application.mConnectedReader.Events.setBatteryEvent(true);
                            Application.mConnectedReader.Events.setInventoryStopEvent(true);
                            Application.mConnectedReader.Events.setInventoryStartEvent(true);
                            Application.mConnectedReader.Events.setTagReadEvent(true);
                        } catch (InvalidUsageException e) {
                            e.printStackTrace();
                        } catch (OperationFailureException e) {
                            e.printStackTrace();
                        } catch (NullPointerException e) {
                            Log.d(TAG, "null pointer ");
                            e.printStackTrace();
                        }

                    } else {
                        try {
                            Application.mConnectedReader.disconnect();
                        } catch (InvalidUsageException e) {
                            e.printStackTrace();
                        } catch (OperationFailureException e) {
                            e.printStackTrace();
                        }
                        Application.mConnectedReader = null;
                        Application.mConnectedDevice = null;
                    }
                } else {
                    if (Application.mConnectedDevice != null) {
                        ReaderDeviceConnected(Application.mConnectedDevice);
                    }
                }
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                if (exceptionIN != null)
                    Toast.makeText(getApplicationContext(), exceptionIN.getInfo(), Toast.LENGTH_SHORT).show();
                AutoConnectDeviceTask = null;
                //Application.contextSettingDetails = null;
            }
        }.execute();
    }

    private void EnableLogger() {
        Application.mConnectedReader.Config.setLogLevel(Level.INFO);
    }

    private void PrintLogs() {
        try {
            String str[] = Application.mConnectedReader.Config.GetLogBuffer().split("\n");
            for (String st : str) {
                Log.d(TAG, st);
            }
        } catch (InvalidUsageException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Method to initialize the connection settings like notifications, auto detection, auto reconnection etc..
     */
    private void initializeConnectionSettings() {
        SharedPreferences settings = getSharedPreferences(Constants.APP_SETTINGS_STATUS, 0);
        Application.AUTO_DETECT_READERS = settings.getBoolean(Constants.AUTO_DETECT_READERS, true);
        Application.AUTO_RECONNECT_READERS = settings.getBoolean(Constants.AUTO_RECONNECT_READERS, true);
        Application.NOTIFY_READER_AVAILABLE = settings.getBoolean(Constants.NOTIFY_READER_AVAILABLE, false);
        Application.NOTIFY_READER_CONNECTION = settings.getBoolean(Constants.NOTIFY_READER_CONNECTION, false);
        if (Build.MODEL.contains("MC33"))
            Application.NOTIFY_BATTERY_STATUS = settings.getBoolean(Constants.NOTIFY_BATTERY_STATUS, false);
        else
            Application.NOTIFY_BATTERY_STATUS = settings.getBoolean(Constants.NOTIFY_BATTERY_STATUS, true);
        Application.EXPORT_DATA = settings.getBoolean(Constants.EXPORT_DATA, false);
        Application.TAG_LIST_MATCH_MODE = settings.getBoolean(Constants.TAG_LIST_MATCH_MODE, false);
        Application.SHOW_CSV_TAG_NAMES = settings.getBoolean(Constants.SHOW_CSV_TAG_NAMES, false);
        Application.asciiMode = settings.getBoolean(Constants.ASCII_MODE, false);
        Application.NON_MATCHING = settings.getBoolean(Constants.NON_MATCHING, false);
        LAST_CONNECTED_READER = settings.getString(Constants.LAST_READER, "");
        LoadProfiles();
        beeperSettings();
        ledsettigs();
        LoadTagListCSV();
    }

    private void LoadProfiles() {
        ProfileContent content = new ProfileContent(this);
        content.LoadDefaultProfiles();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(BroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action buttons
        switch (item.getItemId()) {
            case R.id.action_dpo:
                Intent detailsIntent = new Intent(MainActivity.this, SettingsDetailActivity.class);
                detailsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                detailsIntent.putExtra(Constants.SETTING_ITEM_ID, R.id.battery);
                startActivity(detailsIntent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_rapidread:
                selectItem(1);
                break;
            case R.id.nav_inventory:
                selectItem(2);
                break;
            case R.id.nav_locatetag:
                selectItem(3);
                break;
            case R.id.nav_profiles:
                selectItem(9);
//            {
//                Intent detailsIntent = new Intent(getApplicationContext(), SettingsDetailActivity.class);
//                detailsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                detailsIntent.putExtra(Constants.SETTING_ITEM_ID, R.id.profiles);
//                startActivity(detailsIntent);
//            }
                break;
            case R.id.nav_settings:
                selectItem(4);
                break;
            case R.id.nav_access_control:
                selectItem(5);
                break;
            case R.id.nav_prefilters:
                selectItem(6);
                break;
//            case R.id.nav_readerslist:
//                selectItem(7);
//                break;
            case R.id.nav_about:
                selectItem(8);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Method called on the click of a NavigationDrawer item to update the UI with the new selection
     *
     * @param position - postion of the item selected
     */
    public void selectItem(int position) {
        // update the no_items content by replacing fragments
        Fragment fragment = null;

        switch (position) {
            case 0:
                fragment = HomeFragment.newInstance();
                break;

            case 1:
                fragment = RapidReadFragment.newInstance();
                break;

            case 2:
                fragment = InventoryFragment.newInstance();
                break;

            case 3:
                fragment = LocationingFragment.newInstance();
                break;

            case 4:
                fragment = new SettingListFragment();
                break;

            case 5:
                fragment = AccessOperationsFragment.newInstance();
                break;

            case 6:
                fragment = PreFilterFragment.newInstance();
                break;

            case 7:
                fragment = ReadersListFragment.newInstance();
                break;

            case 8:
                fragment = AboutFragment.newInstance();
                break;

            case 9:
                fragment = ProfileFragment.newInstance();
                break;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (position == 0) {
            //Pop the back stack since we want to maintain only one level of the back stack
            //Don't add the transaction to back stack since we are navigating to the first fragment
            //being displayed and adding the same to the backstack will result in redundancy
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, TAG_CONTENT_FRAGMENT).commit();
        } else {
            //Pop the back stack since we want to maintain only one level of the back stack
            //Add the transaction to the back stack since we want the state to be preserved in the back stack
            //if (position != 9)
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, TAG_CONTENT_FRAGMENT).addToBackStack(null).commit();
        }

        // update selected item and title, then close the drawer
        //mDrawerList.setItemChecked(position, true);
        setTitle(mOptionTitles[position]);
        //mDrawerLayout.closeDrawer(mDrawerList);
    }

    /**
     * method to get currently displayed action bar icon
     *
     * @return resource id of the action bar icon
     */
    private int getActionBarIcon() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (fragment instanceof RapidReadFragment)
            return R.drawable.dl_rr;
        else if (fragment instanceof InventoryFragment)
            return R.drawable.dl_inv;
        else if (fragment instanceof LocationingFragment)
            return R.drawable.dl_loc;
        else if (fragment instanceof SettingListFragment)
            return R.drawable.dl_sett;
        else if (fragment instanceof AccessOperationsFragment)
            return R.drawable.dl_access;
        else if (fragment instanceof PreFilterFragment)
            return R.drawable.dl_filters;
        else if (fragment instanceof ReadersListFragment)
            return R.drawable.dl_rdl;
        else if (fragment instanceof AboutFragment)
            return R.drawable.dl_about;
        else
            return -1;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.activityResumed();
        if (!isAppIsInBackground(getApplicationContext())) {
            scanEnableTask = new ScanEnableTask(false);
            scanEnableTask.execute();
        }
    }

    public static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        ComponentName componentInfo = taskInfo.get(0).topActivity;
        if (componentInfo.getPackageName().equals(context.getPackageName())) {
            isInBackground = false;
        }

        return isInBackground;
    }

    /**
     * call back of activity,which will call before activity went to paused
     */
    @Override
    public void onPause() {
        super.onPause();
        Application.activityPaused();
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        //mDrawerToggle.syncState();
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks whether a hardware keyboard is available
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (fragment != null && fragment instanceof InventoryFragment && newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            findViewById(R.id.inventoryDataLayout).setVisibility(View.INVISIBLE);
            findViewById(R.id.inventoryButton).setVisibility(View.INVISIBLE);
        } else if (fragment != null && fragment instanceof InventoryFragment && newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            findViewById(R.id.inventoryDataLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.inventoryButton).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //update the selected item in the drawer and the title
            //mDrawerList.setItemChecked(0, true);
            setTitle(mOptionTitles[0]);
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
            if (fragment != null && fragment instanceof BackPressedFragment) {
                ((BackPressedFragment) fragment).onBackPressed();
            } else if (fragment != null && fragment instanceof HomeFragment) {
                //stop Timer
                Inventorytimer.getInstance().stopTimer();
                stopTimer();
                //
                if (DisconnectTask != null)
                    DisconnectTask.cancel(true);
                //disconnect from reader
                try {
                    if (Application.mConnectedReader != null) {
                        if (Application.mConnectedReader.isConnected()) {
//                            if(Application.mIsInventoryRunning)
//                                Application.mConnectedReader.Actions.Inventory.stop();
                            Application.mConnectedReader.Events.removeEventsListener(Application.eventHandler);
                        }
                        Application.mConnectedReader.disconnect();
                    }
                } catch (InvalidUsageException e) {
                    e.printStackTrace();
                } catch (OperationFailureException e) {
                    e.printStackTrace();
                }
                Application.mConnectedReader = null;

                // update dpo icon in settings list
                AdvancedOptionsContent.ITEMS.get(DPO_ITEM_INDEX).icon = R.drawable.title_dpo_disabled;
                clearSettings();
                Application.mConnectedDevice = null;
                // Application.mConnectedReader = null;
                ReadersListFragment.readersList.clear();
                if (Application.readers != null) {
                    Application.readers.deattach(this);
                    Application.readers.Dispose();
                    Application.readers = null;
                }
                Application.reset();
                super.onBackPressed();
            } else {
                super.onBackPressed();
            }
        }
    }

    /**
     * Callback method to handle the click of start/stop button in the inventory fragment
     *
     * @param v - Button Clicked
     */
    public synchronized void inventoryStartOrStop(View v) {
        if (MatchModeFileLoader.getInstance().isImportTaskRunning()) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.loading_csv), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Loading CSV");
            return;
        }
        FloatingActionButton button = (FloatingActionButton) v;
        Application.tagListMatchNotice = false;
        if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected()) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
            if (!Application.mIsInventoryRunning) {
                clearInventoryData();
                //button.setText("STOP");
                button.setImageResource(R.drawable.ic_play_stop);

                //Here we send the inventory command to start reading the tags
                if (fragment != null && fragment instanceof InventoryFragment) {
                    Spinner memoryBankSpinner = ((Spinner) findViewById(R.id.inventoryOptions));
                    memoryBankSpinner.setSelection(Application.memoryBankId);
                    memoryBankSpinner.setEnabled(false);
                    ((InventoryFragment) fragment).resetTagsInfo();
                }
                //set flag value
                isInventoryAborted = false;
                getTagReportingfields();
                //if (!Application.mConnectedReader.getHostName().startsWith("RFD8500") && Application.batchMode != 2 || Application.mConnectedReader.getHostName().startsWith("RFD8500") && Application.batchMode != 2)
                {
                    PrepareMatchModeList();
                }
                // UI update for inventory fragment
                if (fragment != null && fragment instanceof InventoryFragment && Application.TAG_LIST_MATCH_MODE) {
                    // TODO: This logic requires updates adpater being assigned particular list
                    if (Application.memoryBankId == 0) {
                        ((InventoryFragment) fragment).getAdapter().originalInventoryList = Application.tagsReadInventory;
                        ((InventoryFragment) fragment).getAdapter().searchItemsList = Application.tagsReadInventory;
                        ((InventoryFragment) fragment).getAdapter().notifyDataSetChanged();
                    } else if (Application.memoryBankId == 1) {  //matching tags
                        ((InventoryFragment) fragment).getAdapter().originalInventoryList = Application.matchingTagsList;
                        ((InventoryFragment) fragment).getAdapter().searchItemsList = Application.matchingTagsList;
                        ((InventoryFragment) fragment).getAdapter().notifyDataSetChanged();
                    } else if (Application.memoryBankId == 2) {  //missing tags
                        Application.missingTagsList.addAll(Application.tagsListCSV);
                        ((InventoryFragment) fragment).getAdapter().originalInventoryList = Application.missingTagsList;
                        ((InventoryFragment) fragment).getAdapter().searchItemsList = Application.missingTagsList;
                        ((InventoryFragment) fragment).getAdapter().notifyDataSetChanged();
                    } else if (Application.memoryBankId == 3) {  //unknown tags
                        ((InventoryFragment) fragment).getAdapter().originalInventoryList = Application.unknownTagsList;
                        ((InventoryFragment) fragment).getAdapter().searchItemsList = Application.unknownTagsList;
                        ((InventoryFragment) fragment).getAdapter().notifyDataSetChanged();
                    }
                    Application.tagsReadForSearch.addAll(((InventoryFragment) fragment).getAdapter().searchItemsList);
                }
                // UI update for RR fragment
                if (fragment != null && fragment instanceof RapidReadFragment) {
                    Application.memoryBankId = -1;
                    ((RapidReadFragment) fragment).resetTagsInfo();
                    if (Application.TAG_LIST_MATCH_MODE) {
                        if (Application.missedTags > 9999) {
                            TextView uniqueTags = (TextView) findViewById(R.id.uniqueTagContent);
                            //orignal size is 60sp - reduced size 45sp
                            uniqueTags.setTextSize(45);
                        }
                    }
                    ((RapidReadFragment) fragment).updateTexts();
                }
                // perform read or inventory
                if (fragment != null && fragment instanceof InventoryFragment && !((InventoryFragment) fragment).getMemoryBankID().equalsIgnoreCase("none") && !Application.TAG_LIST_MATCH_MODE) {
                    //If memory bank is selected, call read command with appropriate memory bank
                    inventoryWithMemoryBank(((InventoryFragment) fragment).getMemoryBankID());
                } else {
                    // unique read is enabled
                    if (Application.reportUniquetags != null && Application.reportUniquetags.getValue() == 1) {
                        Application.mConnectedReader.Actions.purgeTags();
                    }
                    //Perform inventory
                    try {
                        Application.mConnectedReader.Actions.Inventory.perform();
                        Application.mIsInventoryRunning = true;
                        Log.d(TAG, "Inventory.perform");
                    } catch (InvalidUsageException e) {
                        e.printStackTrace();
                    } catch (final OperationFailureException e) {
                        e.printStackTrace();
                        fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
                        if (fragment instanceof ResponseHandlerInterfaces.ResponseStatusHandler)
                            ((ResponseHandlerInterfaces.ResponseStatusHandler) fragment).handleStatusResponse(e.getResults());
                        sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, e.getVendorMessage());
                        if (Application.batchMode != -1) {
                            if (Application.batchMode == BATCH_MODE.ENABLE.getValue()) {
                                Application.isBatchModeInventoryRunning = true;
                            }
                        }
                    }
                }
            } else if (Application.mIsInventoryRunning) {
                Application.mInventoryStartPending = false;
                if (fragment != null && fragment instanceof InventoryFragment) {
                    ((Spinner) findViewById(R.id.inventoryOptions)).setEnabled(true);
                }
                //button.setText("START");
                button.setImageResource(android.R.drawable.ic_media_play);

                isInventoryAborted = true;
                //Here we send the abort command to stop the inventory
                try {
                    Application.mConnectedReader.Actions.Inventory.stop();
                    synchronized (isInventoryAborted) {
                        isInventoryAborted.notify();
                    }
                    if (((Application.settings_startTrigger != null && (Application.settings_startTrigger.getTriggerType() == START_TRIGGER_TYPE.START_TRIGGER_TYPE_HANDHELD
                            || Application.settings_startTrigger.getTriggerType() == START_TRIGGER_TYPE.START_TRIGGER_TYPE_PERIODIC)) || getRepeatTriggers())) {
                        operationHasAborted();
                    }
                    Log.d(TAG, "Inventory.stop");
                } catch (InvalidUsageException e) {
                    e.printStackTrace();
                } catch (OperationFailureException e) {
                    e.printStackTrace();
                    operationHasAborted();
                }
            }
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_disconnected), Toast.LENGTH_SHORT).show();
    }

    private void PrepareMatchModeList() {
        Log.d(TAG, "PrepareMatchModeList");
        if (Application.TAG_LIST_MATCH_MODE) {
            //This for loop will reset all the items in the tagsListCSV(making Tag count to zero)
            for (int i = 0; i < Application.tagsListCSV.size(); i++) {
                InventoryListItem inv = null;
                if (Application.tagsListCSV.get(i).getCount() != 0) {
                    inv = Application.tagsListCSV.remove(i);
                    InventoryListItem inventoryListItem = new InventoryListItem(inv.getTagID(), 0, null, null, null, null, null, null);
                    inventoryListItem.setTagDetails(inv.getTagDetails());
                    Application.tagsListCSV.add(i, inventoryListItem);
                } else {
                    if (Application.tagsListCSV.get(i).isVisible()) {
                        Application.tagsListCSV.get(i).setVisible(false);
                    }
                }
            }
            Application.UNIQUE_TAGS_CSV = Application.tagsListCSV.size();
            Application.tagsReadInventory.addAll(Application.tagsListCSV);
            Application.inventoryList.putAll(Application.tagListMap);
            Application.missedTags = Application.tagsListCSV.size();
            Application.matchingTags = 0;
            Log.d(TAG, "PrepareMatchModeList done");
        }
    }

    private void getTagReportingfields() {
        pc = false;
        phase = false;
        channelIndex = false;
        rssi = false;
        if (Application.tagStorageSettings != null) {
            TAG_FIELD[] tag_field = Application.tagStorageSettings.getTagFields();
            for (int idx = 0; idx < tag_field.length; idx++) {
                if (tag_field[idx] == TAG_FIELD.PEAK_RSSI)
                    rssi = true;
                if (tag_field[idx] == TAG_FIELD.PHASE_INFO)
                    phase = true;
                if (tag_field[idx] == TAG_FIELD.PC)
                    pc = true;
                if (tag_field[idx] == TAG_FIELD.CHANNEL_INDEX)
                    channelIndex = true;
                if (tag_field[idx] == TAG_FIELD.TAG_SEEN_COUNT)
                    tagSeenCount = true;
            }
        }
    }

    /**
     * Method to call when we want inventory to happen with memory bank parameters
     *
     * @param memoryBankID id of the memory bank
     */
    public void inventoryWithMemoryBank(String memoryBankID) {
        if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected()) {
            TagAccess tagAccess = new TagAccess();
            TagAccess.ReadAccessParams readAccessParams = tagAccess.new ReadAccessParams();
            //Set the param values
            readAccessParams.setCount(0);
            readAccessParams.setOffset(0);

            if ("RESERVED".equalsIgnoreCase(memoryBankID))
                readAccessParams.setMemoryBank(MEMORY_BANK.MEMORY_BANK_RESERVED);
            if ("EPC".equalsIgnoreCase(memoryBankID))
                readAccessParams.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
            if ("TID".equalsIgnoreCase(memoryBankID))
                readAccessParams.setMemoryBank(MEMORY_BANK.MEMORY_BANK_TID);
            if ("USER".equalsIgnoreCase(memoryBankID))
                readAccessParams.setMemoryBank(MEMORY_BANK.MEMORY_BANK_USER);
            try {
                //Read command with readAccessParams and accessFilter as null to read all the tags
                Application.mConnectedReader.Actions.TagAccess.readEvent(readAccessParams, null, null);
                Application.mIsInventoryRunning = true;
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
                if (fragment instanceof ResponseHandlerInterfaces.ResponseStatusHandler)
                    ((ResponseHandlerInterfaces.ResponseStatusHandler) fragment).handleStatusResponse(e.getResults());
                Toast.makeText(getApplicationContext(), e.getVendorMessage(), Toast.LENGTH_SHORT).show();
            }

        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_disconnected), Toast.LENGTH_SHORT).show();
    }

    /**
     * Method called when read button in AccessOperationsFragment is clicked
     *
     * @param v - Read Button
     */
    public void accessOperationsReadClicked(View v) {
        if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected()) {
            if (Application.mConnectedReader.isCapabilitiesReceived()) {
                AutoCompleteTextView tagIDField = ((AutoCompleteTextView) findViewById(R.id.accessRWTagID));
                if (tagIDField != null && tagIDField.getText() != null) {
                    String tagValue = tagIDField.getText().toString();
                    final String tagId;
                    if (Application.asciiMode == true)
                        tagId = asciitohex.convert(tagValue);
                    else
                        tagId = tagValue;

                    if (!tagId.isEmpty()) {
                        Application.accessControlTag = tagId;
                        EditText offsetText = (EditText) findViewById(R.id.accessRWOffsetValue);
                        EditText lengthText = (EditText) findViewById(R.id.accessRWLengthValue);
                        if (!offsetText.getText().toString().isEmpty()) {
                            if (!lengthText.getText().toString().isEmpty()) {
                                progressDialog = new CustomProgressDialog(this, MSG_READ);
                                progressDialog.show();
                                final Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);

                                TextView accessRWData = (TextView) findViewById(R.id.accessRWData);
                                if (accessRWData != null) {
                                    accessRWData.setText("");
                                }
                                timerDelayRemoveDialog(Constants.RESPONSE_TIMEOUT, progressDialog, "Read");

                                Application.isAccessCriteriaRead = true;

                                TagAccess tagAccess = new TagAccess();
                                final TagAccess.ReadAccessParams readAccessParams = tagAccess.new ReadAccessParams();
                                try {
                                    readAccessParams.setAccessPassword(Long.decode("0X" + ((EditText) findViewById(R.id.accessRWPassword)).getText().toString()));
                                } catch (NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    Toast.makeText(this, "Password field is empty!", Toast.LENGTH_LONG).show();
                                }
                                readAccessParams.setCount(Integer.parseInt(lengthText.getText().toString()));

                                readAccessParams.setMemoryBank(GetAccessRWMemoryBank());

                                readAccessParams.setOffset(Integer.parseInt(offsetText.getText().toString()));

                                new AsyncTask<Void, Void, Boolean>() {
                                    private InvalidUsageException invalidUsageException;
                                    private OperationFailureException operationFailureException;

                                    @Override
                                    protected Boolean doInBackground(Void... voids) {
                                        try {
                                            setAccessProfile(true);
                                            final TagData tagData = Application.mConnectedReader.Actions.TagAccess.readWait(tagId, readAccessParams, null, true);
                                            //TODO: revisit this code for need of flags
                                            if (Application.isAccessCriteriaRead && !Application.mIsInventoryRunning) {
                                                if (fragment instanceof AccessOperationsFragment)
                                                    ((AccessOperationsFragment) fragment).handleTagResponse(tagData);
                                            }
                                        } catch (InvalidUsageException e) {
                                            invalidUsageException = e;
                                            e.printStackTrace();
                                        } catch (OperationFailureException e) {
                                            operationFailureException = e;
                                            e.printStackTrace();
                                        }
                                        return true;
                                    }

                                    @Override
                                    protected void onPostExecute(Boolean result) {
                                        if (invalidUsageException != null) {
                                            progressDialog.dismiss();
                                            sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, invalidUsageException.getInfo());
                                        } else if (operationFailureException != null) {
                                            progressDialog.dismiss();
                                            sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, operationFailureException.getVendorMessage());
                                        }
                                    }
                                }.execute();
                            } else
                                Toast.makeText(getApplicationContext(), getString(R.string.empty_length), Toast.LENGTH_SHORT).show();
                        } else
                            Toast.makeText(getApplicationContext(), getString(R.string.empty_offset), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, Constants.TAG_EMPTY);
                        Toast.makeText(getApplicationContext(), Constants.TAG_EMPTY, Toast.LENGTH_SHORT).show();
                    }
                }
            } else
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_reader_not_updated), Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_disconnected), Toast.LENGTH_SHORT).show();
    }

    public static void setAccessProfile(boolean bSet) {
        if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected() && Application.mConnectedReader.isCapabilitiesReceived()
                && !Application.mIsInventoryRunning && !Application.isLocatingTag) {
            Antennas.AntennaRfConfig antennaRfConfig;
            try {
                if (bSet && Application.antennaRfConfig.getrfModeTableIndex() != 0) {
                    antennaRfConfig = Application.antennaRfConfig;
                    // use of default profile for access operation
                    antennaRfConfig.setrfModeTableIndex(0);
                    Application.mConnectedReader.Config.Antennas.setAntennaRfConfig(1, antennaRfConfig);
                    Application.antennaRfConfig = antennaRfConfig;
                } else if (!bSet && Application.antennaRfConfig.getrfModeTableIndex() != LinkProfileUtil.getInstance().getSimpleProfileModeIndex(ActiveProfile.LinkProfileIndex)) {
                    antennaRfConfig = Application.antennaRfConfig;
                    antennaRfConfig.setrfModeTableIndex(LinkProfileUtil.getInstance().getSimpleProfileModeIndex(ActiveProfile.LinkProfileIndex));
                    Application.mConnectedReader.Config.Antennas.setAntennaRfConfig(1, antennaRfConfig);
                    Application.antennaRfConfig = antennaRfConfig;
                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    private MEMORY_BANK GetAccessRWMemoryBank() {
        String bankItem = ((Spinner) findViewById(R.id.accessRWMemoryBank)).getSelectedItem().toString();
        if ("RESV".equalsIgnoreCase(bankItem) || bankItem.contains("PASSWORD"))
            return MEMORY_BANK.MEMORY_BANK_RESERVED;
        else if ("EPC".equalsIgnoreCase(bankItem) || bankItem.contains("PC"))
            return MEMORY_BANK.MEMORY_BANK_EPC;
        else if ("TID".equalsIgnoreCase(bankItem))
            return MEMORY_BANK.MEMORY_BANK_TID;
        else if ("USER".equalsIgnoreCase(bankItem))
            return MEMORY_BANK.MEMORY_BANK_USER;

        return MEMORY_BANK.MEMORY_BANK_EPC;
    }

    /**
     * Method called when write button in AccessOperationsFragment is clicked
     *
     * @param v - Write Button
     */
    public void accessOperationsWriteClicked(View v) {
        if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected()) {
            if (Application.mConnectedReader.isCapabilitiesReceived()) {
                AutoCompleteTextView tagIDField = ((AutoCompleteTextView) findViewById(R.id.accessRWTagID));
                if (tagIDField != null && tagIDField.getText() != null) {
                    final String tagId = (Application.asciiMode == true ? asciitohex.convert(tagIDField.getText().toString()) : tagIDField.getText().toString());
                    if (!tagId.isEmpty()) {
                        Application.accessControlTag = tagId;
                        EditText offsetText = (EditText) findViewById(R.id.accessRWOffsetValue);
                        if (!offsetText.getText().toString().isEmpty()) {
                            progressDialog = new CustomProgressDialog(this, MSG_WRITE);
                            progressDialog.show();
                            Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
                            timerDelayRemoveDialog(Constants.RESPONSE_TIMEOUT, progressDialog, "Write");
                            Application.isAccessCriteriaRead = true;

                            TagAccess tagAccess = new TagAccess();
                            final TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();
                            try {
                                writeAccessParams.setAccessPassword(Long.decode("0X" + ((EditText) findViewById(R.id.accessRWPassword)).getText().toString()));
                            } catch (NumberFormatException nfe) {
                                nfe.printStackTrace();
                                Toast.makeText(this, "Password field is empty!", Toast.LENGTH_LONG).show();
                            }

                            writeAccessParams.setMemoryBank(GetAccessRWMemoryBank());

                            writeAccessParams.setOffset(Integer.parseInt(((EditText) findViewById(R.id.accessRWOffsetValue)).getText().toString()));
                            String wData = ((TextView) findViewById(R.id.accessRWData)).getText().toString();
                            if (Application.asciiMode == true) {
                                wData = asciitohex.convert(wData);
                                writeAccessParams.setWriteData(wData);
                                writeAccessParams.setWriteDataLength(wData.length() / 4);
                            } else {
                                writeAccessParams.setWriteData(wData);
                                writeAccessParams.setWriteDataLength(wData.length() / 4);
                            }
                            new AsyncTask<Void, Void, Boolean>() {
                                private InvalidUsageException invalidUsageException;
                                private OperationFailureException operationFailureException;
                                private Boolean bResult = false;

                                @Override
                                protected Boolean doInBackground(Void... voids) {
                                    try {
                                        setAccessProfile(true);
                                        Application.mConnectedReader.Actions.TagAccess.writeWait(tagId, writeAccessParams, null, null, true, false);
                                        bResult = true;
                                    } catch (InvalidUsageException e) {
                                        invalidUsageException = e;
                                        e.printStackTrace();
                                    } catch (OperationFailureException e) {
                                        operationFailureException = e;
                                        e.printStackTrace();
                                    }
                                    return bResult;
                                }

                                @Override
                                protected void onPostExecute(Boolean result) {
                                    if (!result) {
                                        if (invalidUsageException != null) {
                                            progressDialog.dismiss();
                                            sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, invalidUsageException.getInfo());
                                        } else if (operationFailureException != null) {
                                            progressDialog.dismiss();
                                            sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, operationFailureException.getVendorMessage());
                                        }
                                    } else {
                                        Toast.makeText(getApplicationContext(), getString(R.string.msg_write_succeed), Toast.LENGTH_SHORT).show();
                                        startbeepingTimer();
                                    }
                                }
                            }.execute();
                        } else
                            Toast.makeText(getApplicationContext(), getString(R.string.empty_offset), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, Constants.TAG_EMPTY);
                        Toast.makeText(getApplicationContext(), Constants.TAG_EMPTY, Toast.LENGTH_SHORT).show();
                    }
                }
            } else
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_reader_not_updated), Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_disconnected), Toast.LENGTH_SHORT).show();
    }

    /**
     * Method called when lock button in AccessOperationsFragment is clicked
     *
     * @param v - Lock button
     */
    public void accessOperationLockClicked(View v) {
        if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected()) {
            if (Application.mConnectedReader.isCapabilitiesReceived()) {
                AutoCompleteTextView tagIDField = ((AutoCompleteTextView) findViewById(R.id.accessLockTagID));
                if (tagIDField != null && tagIDField.getText() != null) {
                    String tagValue = (Application.asciiMode == true ? asciitohex.convert(tagIDField.getText().toString()) : tagIDField.getText().toString());

                    if (!tagValue.isEmpty()) {
                        final String tagId = tagValue.toUpperCase();
                        Application.accessControlTag = tagId;
                        progressDialog = new CustomProgressDialog(this, MSG_LOCK);
                        progressDialog.show();
                        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
                        timerDelayRemoveDialog(Constants.RESPONSE_TIMEOUT, progressDialog, "Lock");
                        Application.isAccessCriteriaRead = true;

                        //Set the param values
                        TagAccess tagAccess = new TagAccess();
                        final TagAccess.LockAccessParams lockAccessParams = tagAccess.new LockAccessParams();
                        if (fragment != null && fragment instanceof AccessOperationsFragment) {
                            Fragment innerFragment = ((AccessOperationsFragment) fragment).getCurrentlyViewingFragment();
                            if (innerFragment != null && innerFragment instanceof AccessOperationsLockFragment) {
                                AccessOperationsLockFragment lockFragment = ((AccessOperationsLockFragment) innerFragment);
                                String lockMemoryBank = lockFragment.getLockMemoryBank();
                                if (lockMemoryBank != null && !lockMemoryBank.isEmpty()) {
                                    if (lockMemoryBank.equalsIgnoreCase("epc")) {

                                        if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_EPC_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_EPC_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_EPC_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_EPC_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK);

                                    } else if (lockMemoryBank.equalsIgnoreCase("tid")) {

                                        if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_TID_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_TID_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_TID_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_TID_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK);

                                    } else if (lockMemoryBank.equalsIgnoreCase("user")) {

                                        if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_USER_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_USER_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_USER_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_USER_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK);

                                    } else if (lockMemoryBank.equalsIgnoreCase("access pwd")) {

                                        if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_ACCESS_PASSWORD, LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_ACCESS_PASSWORD, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_ACCESS_PASSWORD, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_ACCESS_PASSWORD, LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK);

                                    } else if (lockMemoryBank.equalsIgnoreCase("kill pwd")) {
                                        if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_KILL_PASSWORD, LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_KILL_PASSWORD, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_KILL_PASSWORD, LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK);
                                        else if (lockFragment.getLockAccessPermission().equals(LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK))
                                            lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_KILL_PASSWORD, LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK);
                                    }
                                }
                            }
                        }
                        try {
                            lockAccessParams.setAccessPassword(Long.decode("0X" + ((EditText) findViewById(R.id.accessLockPassword)).getText().toString()));
                        } catch (NumberFormatException nfe) {
                            nfe.printStackTrace();
                            Toast.makeText(this, "Password field is empty!", Toast.LENGTH_LONG).show();
                        }
                        new AsyncTask<Void, Void, Boolean>() {
                            private InvalidUsageException invalidUsageException;
                            private OperationFailureException operationFailureException;
                            private Boolean bResult = false;

                            @Override
                            protected Boolean doInBackground(Void... voids) {
                                try {
                                    setAccessProfile(true);
                                    Application.mConnectedReader.Actions.TagAccess.lockWait(tagId, lockAccessParams, null, true);
                                    bResult = true;
                                    startbeepingTimer();
                                } catch (InvalidUsageException e) {
                                    invalidUsageException = e;
                                    e.printStackTrace();
                                } catch (OperationFailureException e) {
                                    operationFailureException = e;
                                    e.printStackTrace();
                                }
                                return bResult;
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                if (!result) {
                                    if (invalidUsageException != null) {
                                        progressDialog.dismiss();
                                        sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, invalidUsageException.getInfo());
                                    } else if (operationFailureException != null) {
                                        progressDialog.dismiss();
                                        sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, operationFailureException.getVendorMessage());
                                    }
                                } else
                                    Toast.makeText(getApplicationContext(), getString(R.string.msg_lock_succeed), Toast.LENGTH_SHORT).show();
                            }
                        }.execute();
                    } else {
                        Log.d(TAG, Constants.TAG_EMPTY);
                        Toast.makeText(getApplicationContext(), Constants.TAG_EMPTY, Toast.LENGTH_SHORT).show();
                    }
                }
            } else
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_reader_not_updated), Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_disconnected), Toast.LENGTH_SHORT).show();
    }

    /**
     * Method called when kill button in AccessOperationsFragment is clicked
     *
     * @param v - Kill button
     */
    public void accessOperationsKillClicked(View v) {
        if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected()) {
            if (Application.mConnectedReader.isCapabilitiesReceived()) {
                AutoCompleteTextView tagIDField = ((AutoCompleteTextView) findViewById(R.id.accessKillTagID));
                if (tagIDField != null && tagIDField.getText() != null) {
                    String tagValue = (Application.asciiMode == true ? asciitohex.convert(tagIDField.getText().toString()) : tagIDField.getText().toString());
                    if (!tagValue.isEmpty()) {
                        final String tagId = tagValue.toUpperCase();
                        Application.accessControlTag = tagId;
                        progressDialog = new CustomProgressDialog(this, MSG_KILL);
                        progressDialog.show();
                        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
                        timerDelayRemoveDialog(Constants.RESPONSE_TIMEOUT, progressDialog, "Kill");
                        Application.isAccessCriteriaRead = true;

                        //Set the param values
                        TagAccess tagAccess = new TagAccess();
                        final TagAccess.KillAccessParams killAccessParams = tagAccess.new KillAccessParams();
                        try {
                            killAccessParams.setKillPassword(Long.decode("0X" + ((EditText) findViewById(R.id.accessKillPassword)).getText().toString()));
                        } catch (NumberFormatException nfe) {
                            nfe.printStackTrace();
                            Toast.makeText(this, "Password field is empty!", Toast.LENGTH_LONG).show();
                        }
                        new AsyncTask<Void, Void, Boolean>() {
                            private InvalidUsageException invalidUsageException;
                            private OperationFailureException operationFailureException;
                            private Boolean bResult = false;

                            @Override
                            protected Boolean doInBackground(Void... voids) {
                                try {
                                    setAccessProfile(true);
                                    Application.mConnectedReader.Actions.TagAccess.killWait(tagId, killAccessParams, null, true);
                                    bResult = true;
                                    startbeepingTimer();
                                } catch (InvalidUsageException e) {
                                    invalidUsageException = e;
                                    e.printStackTrace();
                                } catch (OperationFailureException e) {
                                    operationFailureException = e;
                                    e.printStackTrace();
                                }
                                return bResult;
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                if (!result) {
                                    if (invalidUsageException != null) {
                                        progressDialog.dismiss();
                                        sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, invalidUsageException.getInfo());
                                    } else if (operationFailureException != null) {
                                        progressDialog.dismiss();
                                        sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, operationFailureException.getVendorMessage());
                                    }
                                } else
                                    Toast.makeText(getApplicationContext(), getString(R.string.msg_kill_succeed), Toast.LENGTH_SHORT).show();
                            }
                        }.execute();
                    } else {
                        Log.d(TAG, Constants.TAG_EMPTY);
                        Toast.makeText(getApplicationContext(), Constants.TAG_EMPTY, Toast.LENGTH_SHORT).show();
                    }
                }
            } else
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_reader_not_updated), Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_disconnected), Toast.LENGTH_SHORT).show();
    }


    /**
     * Method called when stop in locationing is clicked
     *
     * @param v - Locationing stop clicked
     */
    public void locationingButtonClicked(View v) {
        if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected()) {
            if (!Application.isLocatingTag) {
                //clear previous proximity details
                Application.TagProximityPercent = 0;
                Application.locateTag = ((AutoCompleteTextView) findViewById(R.id.lt_et_epc)).getText().toString();
                if (!Application.locateTag.isEmpty()) {
                    ((AutoCompleteTextView) findViewById(R.id.lt_et_epc)).setFocusable(false);
                    ((FloatingActionButton) v).setImageResource(R.drawable.ic_play_stop);

                    RangeGraph locationBar = (RangeGraph) findViewById(R.id.locationBar);
                    locationBar.setValue(0);
                    locationBar.invalidate();
                    locationBar.requestLayout();
                    Application.isLocatingTag = true;
                    new AsyncTask<Void, Void, Boolean>() {
                        private InvalidUsageException invalidUsageException;
                        private OperationFailureException operationFailureException;

                        @Override
                        protected Boolean doInBackground(Void... voids) {
                            try {
                                if (Application.asciiMode == true)
                                    Application.mConnectedReader.Actions.TagLocationing.Perform(asciitohex.convert(Application.locateTag), null, null);
                                else
                                    Application.mConnectedReader.Actions.TagLocationing.Perform(Application.locateTag, null, null);
//                                    Application.isLocatingTag = true;
                            } catch (InvalidUsageException e) {
                                e.printStackTrace();
                                invalidUsageException = e;
                            } catch (OperationFailureException e) {
                                e.printStackTrace();
                                operationFailureException = e;
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            if (invalidUsageException != null) {
                                Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
                                if (fragment instanceof ResponseHandlerInterfaces.ResponseStatusHandler)
                                    ((ResponseHandlerInterfaces.ResponseStatusHandler) fragment).handleStatusResponse(RFIDResults.RFID_API_PARAM_ERROR);
                                sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, invalidUsageException.getInfo());
                            } else if (operationFailureException != null) {
                                Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
                                if (fragment instanceof ResponseHandlerInterfaces.ResponseStatusHandler)
                                    ((ResponseHandlerInterfaces.ResponseStatusHandler) fragment).handleStatusResponse(operationFailureException.getResults());
                                sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, operationFailureException.getVendorMessage());
                            }
                        }
                    }.execute();
                } else {
                    Log.d(TAG, Constants.TAG_EMPTY);
                    Toast.makeText(getApplicationContext(), Constants.TAG_EMPTY, Toast.LENGTH_SHORT).show();
                }

            } else {
                new AsyncTask<Void, Void, Boolean>() {
                    private InvalidUsageException invalidUsageException;
                    private OperationFailureException operationFailureException;

                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        try {
                            Application.mConnectedReader.Actions.TagLocationing.Stop();
                            if (((Application.settings_startTrigger != null && (Application.settings_startTrigger.getTriggerType() == START_TRIGGER_TYPE.START_TRIGGER_TYPE_HANDHELD || Application.settings_startTrigger.getTriggerType() == START_TRIGGER_TYPE.START_TRIGGER_TYPE_PERIODIC)))
                                    || (Application.isBatchModeInventoryRunning != null && Application.isBatchModeInventoryRunning))
                                operationHasAborted();
                        } catch (InvalidUsageException e) {
                            invalidUsageException = e;
                            e.printStackTrace();
                        } catch (OperationFailureException e) {
                            operationFailureException = e;
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        if (invalidUsageException != null) {
                            progressDialog.dismiss();
                            sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, invalidUsageException.getInfo());
                        } else if (operationFailureException != null) {
                            progressDialog.dismiss();
                            sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, operationFailureException.getVendorMessage());
                        }
                    }
                }.execute();
                ((FloatingActionButton) v).setImageResource(android.R.drawable.ic_media_play);
                isLocationingAborted = true;
                (findViewById(R.id.lt_et_epc)).setFocusableInTouchMode(true);
                (findViewById(R.id.lt_et_epc)).setFocusable(true);
            }
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_disconnected), Toast.LENGTH_SHORT).show();
    }

    /**
     * method to stop progress dialog on timeout
     *
     * @param time
     * @param d
     * @param command
     */
    public void timerDelayRemoveDialog(long time, final Dialog d, final String command) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (d != null && d.isShowing()) {
                    d.dismiss();
                    //TODO: cross check on selective flag clearing
                    if (Application.isAccessCriteriaRead) {
                        if (accessTagCount == 0)
                            sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, getString(R.string.err_access_op_failed));
                        Application.isAccessCriteriaRead = false;
                    } else {
                        sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, command + " timeout");
                        if (Application.isActivityVisible())
                            callBackPressed();
                    }
                    Application.isAccessCriteriaRead = false;
                    accessTagCount = 0;
                }
            }
        }, time);
    }

    /**
     * Method to send the notification
     *
     * @param action - intent action
     * @param data   - notification message
     */
    public void sendNotification(String action, String data) {
        if (Application.isActivityVisible()) {
            if (action.equalsIgnoreCase(Constants.ACTION_READER_BATTERY_CRITICAL) || action.equalsIgnoreCase(Constants.ACTION_READER_BATTERY_LOW)) {
                new CustomToast(MainActivity.this, R.layout.toast_layout, data).show();
            } else {
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
            }
        } else {
            Intent i = new Intent(MainActivity.this, NotificationsService.class);
            i.putExtra(Constants.INTENT_ACTION, action);
            i.putExtra(Constants.INTENT_DATA, data);
            startService(i);
        }
    }

    /**
     * Method to be called from Fragments of this activity after handling the response from the reader(success / failure)
     */
    public void callBackPressed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.super.onBackPressed();
            }
        });
    }

    /**
     * Method to change operation status and ui in app on recieving abort status
     */
    private void operationHasAborted() {
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (Application.isBatchModeInventoryRunning != null && Application.isBatchModeInventoryRunning) {
            if (isInventoryAborted) {
                Application.isBatchModeInventoryRunning = false;
                Application.isGettingTags = true;
                if (Application.settings_startTrigger == null) {
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... voids) {
                            try {
                                // execute following code after STOP is finished
                                synchronized (isInventoryAborted) {
                                    isInventoryAborted.wait(50);
                                }
                                if (Application.mConnectedReader.isCapabilitiesReceived()) {
                                    UpdateReaderConnection(false);
                                } else {
                                    UpdateReaderConnection(true);
                                }
                                getTagReportingfields();
                                Application.mConnectedReader.Actions.getBatchedTags();
                            } catch (InvalidUsageException e) {
                                e.printStackTrace();
                            } catch (OperationFailureException e) {
                                e.printStackTrace();
                                Log.d(TAG, "OpFailEx " + e.getVendorMessage() + " " + e.getResults() + " " + e.getStatusDescription());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                } else {
                    Application.mConnectedReader.Actions.getBatchedTags();
                }
            }
        }
        if (Application.mIsInventoryRunning) {
            if (isInventoryAborted) {
                Application.mIsInventoryRunning = false;
                isInventoryAborted = false;
                isTriggerRepeat = null;
                if (Inventorytimer.getInstance().isTimerRunning())
                    Inventorytimer.getInstance().stopTimer();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (fragment instanceof InventoryFragment)
                            ((InventoryFragment) fragment).resetInventoryDetail();
                        else if (fragment instanceof RapidReadFragment)
                            ((RapidReadFragment) fragment).resetInventoryDetail();
                        //export Data to the file
                        if (Application.EXPORT_DATA)
                            if (Application.tagsReadInventory != null && !Application.tagsReadInventory.isEmpty()) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                    exportData();
                                } else {
                                    checkForExportPermission(REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            }
                    }
                });
            }
        } else if (Application.isLocatingTag) {
            if (isLocationingAborted) {
                Application.isLocatingTag = false;
                isLocationingAborted = false;
                if (fragment instanceof LocationingFragment)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((LocationingFragment) fragment).resetLocationingDetails(false);
                        }
                    });
            }
        }
    }

    void exportData() {
        new DataExportTask(getApplicationContext(), Application.tagsReadInventory, Application.mConnectedReader.getHostName(), Application.TOTAL_TAGS, Application.UNIQUE_TAGS, Application.mRRStartedTime).execute();
    }

    void LoadTagListCSV() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            MatchModeFileLoader.getInstance().LoadMatchModeCSV();
        } else {
            checkForExportPermission(REQUEST_CODE_ASK_PERMISSIONS_CSV);
        }
    }

    void checkForExportPermission(final int code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                switch (code) {
                    case REQUEST_CODE_ASK_PERMISSIONS:
                        exportData();
                        break;
                    case REQUEST_CODE_ASK_PERMISSIONS_CSV:
                        MatchModeFileLoader.getInstance().LoadMatchModeCSV();
                        break;
                }
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    //Toast.makeText(this,"Write to external storage permission needed to export inventory.",Toast.LENGTH_LONG).show();
                    showMessageOKCancel("Write to external storage permission needed to export the inventory.",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            code);
                                }
                            });
                    return;
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, code);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportData();
            }
        } else if (requestCode == REQUEST_CODE_ASK_PERMISSIONS_CSV) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MatchModeFileLoader.getInstance().LoadMatchModeCSV();
            }
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * method to set DPO status on Action bar
     *
     * @param level
     */
    public void setActionBarBatteryStatus(final int level) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (menu != null && menu.findItem(R.id.action_dpo) != null) {
                    if (Application.dynamicPowerSettings != null && Application.dynamicPowerSettings.getValue() == 1) {
                        menu.findItem(R.id.action_dpo).setIcon(R.drawable.action_battery_dpo_level);
                    } else {
                        menu.findItem(R.id.action_dpo).setIcon(R.drawable.action_battery_level);
                    }
                    menu.findItem(R.id.action_dpo).getIcon().setLevel(level);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.common_menu, menu);
        this.menu = menu;
        if (Application.BatteryData != null)
            setActionBarBatteryStatus(Application.BatteryData.getLevel());
        return true;
    }

    /**
     * method lear inventory data like total tags, unique tags, read rate etc..
     */
    public static void clearInventoryData() {
        Application.TOTAL_TAGS = 0;
        Application.mRRStartedTime = 0;
        Application.UNIQUE_TAGS = 0;
        Application.UNIQUE_TAGS_CSV = 0;
        Application.TAG_READ_RATE = 0;
        if (!(Application.isBatchModeInventoryRunning != null && Application.isBatchModeInventoryRunning)) {
            Application.missedTags = 0;
            Application.matchingTags = 0;
        }
        Application.currentFragment = "";
        if (Application.tagIDs != null)
            Application.tagIDs.clear();
        if (Application.tagsReadInventory.size() > 0)
            Application.tagsReadInventory.clear();
        if (Application.inventoryList != null && Application.inventoryList.size() > 0)
            Application.inventoryList.clear();

        if (Application.TAG_LIST_MATCH_MODE) {
            Application.matchingTagsList.clear();
            Application.missingTagsList.clear();
            Application.unknownTagsList.clear();
            Application.tagsReadForSearch.clear();
        }

    }

    /**
     * RR button in {@link com.zebra.rfidreader.demo.rapidread.RapidReadFragment} is clicked
     *
     * @param view - Button clicked
     */
    public void rrClicked(View view) {
        selectNavigationMenuItem(0);
        selectItem(1);
    }

    /**
     * Inventory button in {@link com.zebra.rfidreader.demo.inventory.InventoryFragment} is clicked
     *
     * @param view - Button clicked
     */
    public void invClicked(View view) {
        selectNavigationMenuItem(1);
        selectItem(2);
    }

    /**
     * Locationing button in {@link com.zebra.rfidreader.demo.locate_tag.LocationingFragment} is clicked
     *
     * @param view - Button clicked
     */
    public void locateClicked(View view) {
        selectNavigationMenuItem(2);
        selectItem(3);
    }

    /**
     * Settings button in {@link com.zebra.rfidreader.demo.settings.SettingListFragment} is clicked
     *
     * @param view - Button clicked
     */
    public void settClicked(View view) {
        selectNavigationMenuItem(5);
        selectItem(4);
    }

    /**
     * Access button in {@link com.zebra.rfidreader.demo.access_operations.AccessOperationsFragment} is clicked
     *
     * @param view - Button clicked
     */
    public void accessClicked(View view) {
        selectNavigationMenuItem(3);
        selectItem(5);
    }

    /**
     * Filter button in {@link com.zebra.rfidreader.demo.settings.PreFilterFragment} is clicked
     *
     * @param view - Button clicked
     */
    public void filterClicked(View view) {
        selectNavigationMenuItem(6);
        selectItem(6);
    }

    /**
     * About option in {@link com.zebra.rfidreader.demo.home.AboutFragment} is selected
     */
    public void aboutClicked() {
        selectNavigationMenuItem(7);
        selectItem(8);
    }

    private void readerReconnected(ReaderDevice readerDevice) {
        // store app reader
        Application.mConnectedDevice = readerDevice;

        Application.mConnectedReader = readerDevice.getRFIDReader();
        if (Application.isBatchModeInventoryRunning != null && Application.isBatchModeInventoryRunning) {
            clearInventoryData();
            Application.mIsInventoryRunning = true;
            Application.memoryBankId = 0;
            startTimer();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
            if (fragment instanceof ResponseHandlerInterfaces.BatchModeEventHandler) {
                ((ResponseHandlerInterfaces.BatchModeEventHandler) fragment).batchModeEventReceived();
            }
        } else
            try {
                UpdateReaderConnection(false);
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
            }
        ReaderDeviceConnected(readerDevice);
    }

    /**
     * Method to notify device disconnection
     *
     * @param readerDevice
     */
    private void readerDisconnected(ReaderDevice readerDevice) {
        stopTimer();
        //updateConnectedDeviceDetails(readerDevice, false);
        if (Application.NOTIFY_READER_CONNECTION)
            sendNotification(Constants.ACTION_READER_DISCONNECTED, "Disconnected from " + readerDevice.getName());
        clearSettings();
        setActionBarBatteryStatus(0);
        ReaderDeviceDisConnected(readerDevice);
        Application.mConnectedDevice = null;
        Application.mConnectedReader = null;
        Application.is_disconnection_requested = false;
    }

    public void inventoryAborted() {
        Inventorytimer.getInstance().stopTimer();
        Application.mIsInventoryRunning = false;
    }

    public void ReaderDeviceConnected(ReaderDevice device) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (fragment instanceof ReadersListFragment) {
            ((ReadersListFragment) fragment).ReaderDeviceConnected(device);
        } else if (fragment instanceof AboutFragment) {
            ((AboutFragment) fragment).deviceConnected();
        } else if (fragment instanceof AdvancedOptionItemFragment) {
            ((AdvancedOptionItemFragment) fragment).settingsListUpdated();
        }

//        else if(fragment instanceof AccessOperationsFragment)
//            ((AccessOperationsFragment) fragment).deviceConnected(device);
        if (readerDeviceFoundHandlers != null && readerDeviceFoundHandlers.size() > 0) {
            for (ReaderDeviceFoundHandler readerDeviceFoundHandler : readerDeviceFoundHandlers)
                readerDeviceFoundHandler.ReaderDeviceConnected(device);
        }
        if (Application.NOTIFY_READER_CONNECTION)
            sendNotification(Constants.ACTION_READER_CONNECTED, "Connected to " + device.getName());
    }

    public void ReaderDeviceDisConnected(ReaderDevice device) {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();

        if (Application.mIsInventoryRunning) {
            inventoryAborted();
            //export Data to the file if inventory is running in batch mode
            if (Application.isBatchModeInventoryRunning != null && !Application.isBatchModeInventoryRunning)
                if (Application.EXPORT_DATA) {
                    if (Application.tagsReadInventory != null && !Application.tagsReadInventory.isEmpty()) {
                        new DataExportTask(getApplicationContext(), Application.tagsReadInventory, device.getName(), Application.TOTAL_TAGS, Application.UNIQUE_TAGS, Application.mRRStartedTime).execute();
                    }
                }
            Application.isBatchModeInventoryRunning = false;
        }
        if (Application.isLocatingTag) {
            Application.isLocatingTag = false;
        }
        //update dpo icon in settings list
        AdvancedOptionsContent.ITEMS.get(DPO_ITEM_INDEX).icon = R.drawable.title_dpo_disabled;

        Application.isAccessCriteriaRead = false;
        accessTagCount = 0;

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (fragment instanceof ReadersListFragment) {
            ((ReadersListFragment) fragment).readerDisconnected(device, false);
            ((ReadersListFragment) fragment).ReaderDeviceDisConnected(device);
        } else if (fragment instanceof LocationingFragment) {
            ((LocationingFragment) fragment).resetLocationingDetails(true);
        } else if (fragment instanceof InventoryFragment) {
            ((InventoryFragment) fragment).resetInventoryDetail();
        } else if (fragment instanceof RapidReadFragment) {
            ((RapidReadFragment) fragment).resetInventoryDetail();
        } else if (fragment instanceof AboutFragment) {
            ((AboutFragment) fragment).resetVersionDetail();
        } else if (fragment instanceof AdvancedOptionItemFragment) {
            ((AdvancedOptionItemFragment) fragment).settingsListUpdated();
        }

        if (readerDeviceFoundHandlers != null && readerDeviceFoundHandlers.size() > 0) {
            for (ReaderDeviceFoundHandler readerDeviceFoundHandler : readerDeviceFoundHandlers)
                readerDeviceFoundHandler.ReaderDeviceDisConnected(device);
        }

        if (Application.mConnectedReader != null && !Application.AUTO_RECONNECT_READERS) {
            try {
                Application.mConnectedReader.disconnect();
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
            }
            Application.mConnectedReader = null;
        }
    }

    @Override
    public void RFIDReaderAppeared(ReaderDevice device) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (fragment instanceof ReadersListFragment)
            ((ReadersListFragment) fragment).RFIDReaderAppeared(device);
        if (Application.NOTIFY_READER_AVAILABLE) {
            if (!device.getName().equalsIgnoreCase("null"))
                sendNotification(Constants.ACTION_READER_AVAILABLE, device.getName() + " is available.");
        }
        if (Application.AUTO_RECONNECT_READERS)
            AutoConnectDevice(this);
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice device) {
        if (AutoConnectDeviceTask != null) {
            AutoConnectDeviceTask.cancel(true);
        }
        Application.mReaderDisappeared = device;
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (fragment instanceof ReadersListFragment)
            ((ReadersListFragment) fragment).RFIDReaderDisappeared(device);
        if (Application.NOTIFY_READER_AVAILABLE)
            sendNotification(Constants.ACTION_READER_AVAILABLE, device.getName() + " is unavailable.");
    }

    private boolean getRepeatTriggers() {
        if ((Application.settings_startTrigger != null && (Application.settings_startTrigger.getTriggerType() == START_TRIGGER_TYPE.START_TRIGGER_TYPE_HANDHELD || Application.settings_startTrigger.getTriggerType() == START_TRIGGER_TYPE.START_TRIGGER_TYPE_PERIODIC))
                || (isTriggerRepeat != null && isTriggerRepeat))
            return true;
        else
            return false;
    }

    /**
     * Method which will called once notification received from reader.
     * update the operation status in the application based on notification type
     *
     * @param rfidStatusEvents - notification received from reader
     */

    private void notificationFromGenericReader(RfidStatusEvents rfidStatusEvents) {

        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
            if (Application.mConnectedReader != null)
                DisconnectTask = new UpdateDisconnectedStatusTask(Application.mConnectedReader.getHostName()).execute();
//            Application.mConnectedReader = null;
        } else if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.INVENTORY_START_EVENT) {
            if (!Application.isAccessCriteriaRead && !Application.isLocatingTag) {
                //if (!getRepeatTriggers() && Inventorytimer.getInstance().isTimerRunning()) {
                Application.mIsInventoryRunning = true;
                Inventorytimer.getInstance().startTimer();
                //}
            }
        } else if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.INVENTORY_STOP_EVENT) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            Application.tagListMatchNotice = false;
            //TODO: revisit why to clear here
            //accessTagCount = 0;
            //Application.isAccessCriteriaRead = false;

            if (Application.mIsInventoryRunning) {
                Inventorytimer.getInstance().stopTimer();
            } else if (Application.isGettingTags) {
                Application.isGettingTags = false;
                Application.mConnectedReader.Actions.purgeTags();
                if (Application.EXPORT_DATA) {

                    if (Application.TAG_LIST_MATCH_MODE) {
                        if (Application.tagsReadInventory != null && !Application.tagsReadInventory.isEmpty() && fragment instanceof InventoryFragment) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new DataExportTask(getApplicationContext(), ((InventoryFragment) fragment).getAdapter().searchItemsList, Application.mConnectedDevice.getName(), Application.TOTAL_TAGS, Application.UNIQUE_TAGS, Application.mRRStartedTime).execute();
                                }
                            });
                        } else if (Application.tagsReadInventory != null && !Application.tagsReadInventory.isEmpty() && fragment instanceof RapidReadFragment && Application.UNIQUE_TAGS != 0) {
                            Application.currentFragment = "RapidReadFragment";
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new DataExportTask(getApplicationContext(), Application.tagsReadInventory, Application.mConnectedDevice.getName(), Application.TOTAL_TAGS, Application.UNIQUE_TAGS, Application.mRRStartedTime).execute();
                                }
                            });
                        }
                    } else if (Application.tagsReadInventory != null && !Application.tagsReadInventory.isEmpty()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new DataExportTask(getApplicationContext(), Application.tagsReadInventory, Application.mConnectedDevice.getName(), Application.TOTAL_TAGS, Application.UNIQUE_TAGS, Application.mRRStartedTime).execute();
                            }
                        });
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (fragment instanceof ReadersListFragment) {
                            //((ReadersListFragment) fragment).cancelProgressDialog();
                            if (Application.mConnectedReader != null && Application.mConnectedReader.ReaderCapabilities.getModelName() != null) {
                                ((ReadersListFragment) fragment).capabilitiesRecievedforDevice();
                            }
                        }
                    }
                });
            }

            if (!getRepeatTriggers()) {
                if (Application.mIsInventoryRunning)
                    isInventoryAborted = true;
                else if (Application.isLocatingTag)
                    isLocationingAborted = true;
                operationHasAborted();
            }
        } else if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.OPERATION_END_SUMMARY_EVENT) {
            if (fragment instanceof RapidReadFragment)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((RapidReadFragment) fragment).updateInventoryDetails();
                    }
                });
        } else if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT && Application.isActivityVisible()) {
            Boolean triggerPressed = false;
            if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED)
                triggerPressed = true;
            Log.d(TAG, "notificationFromGenericReader " + fragment + " screen " + m_ScreenOn);
            if (m_ScreenOn) {
                if (triggerPressed && isTriggerImmediateorRepeat(triggerPressed) && fragment instanceof TriggerEventHandler) {
                    ((TriggerEventHandler) fragment).triggerPressEventRecieved();
                } else if (!triggerPressed && isTriggerImmediateorRepeat(triggerPressed) && fragment instanceof TriggerEventHandler) {
                    ((TriggerEventHandler) fragment).triggerReleaseEventRecieved();
                    Application.tagListMatchNotice = false;
                }
            }
        } else if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.BATTERY_EVENT) {
            final Events.BatteryData batteryData = rfidStatusEvents.StatusEventData.BatteryData;
            Application.BatteryData = batteryData;
            setActionBarBatteryStatus(batteryData.getLevel());

            if (batteryNotificationHandlers != null && batteryNotificationHandlers.size() > 0) {
                for (BatteryNotificationHandler batteryNotificationHandler : batteryNotificationHandlers)
                    batteryNotificationHandler.deviceStatusReceived(batteryData.getLevel(), batteryData.getCharging(), batteryData.getCause());
            }
            if (Application.NOTIFY_BATTERY_STATUS && batteryData.getCause() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (batteryData.getCause().trim().equalsIgnoreCase(Constants.MESSAGE_BATTERY_CRITICAL))
                            sendNotification(com.zebra.rfidreader.demo.common.Constants.ACTION_READER_BATTERY_CRITICAL, getString(R.string.battery_status__critical_message));
                        else if (batteryData.getCause().trim().equalsIgnoreCase(Constants.MESSAGE_BATTERY_LOW))
                            sendNotification(com.zebra.rfidreader.demo.common.Constants.ACTION_READER_BATTERY_CRITICAL, getString(R.string.battery_status_low_message));
                    }
                });
            }
        } else if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.BATCH_MODE_EVENT) {
            Application.isBatchModeInventoryRunning = true;
            startTimer();
            clearInventoryData();
            Application.mIsInventoryRunning = true;
            Application.memoryBankId = 0;
            PrepareMatchModeList();
            isTriggerRepeat = rfidStatusEvents.StatusEventData.BatchModeEventData.get_RepeatTrigger();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (fragment instanceof ResponseHandlerInterfaces.BatchModeEventHandler) {
                        ((ResponseHandlerInterfaces.BatchModeEventHandler) fragment).batchModeEventReceived();
                    }
                    if (fragment instanceof ReadersListFragment) {
                        if (Application.mConnectedReader != null && Application.mConnectedReader.ReaderCapabilities.getModelName() == null) {
                            ((ReadersListFragment) fragment).capabilitiesRecievedforDevice();
                        }
                    }
                }
            });
        }
    }

    /*
     *method to check if both start and stop trigger is IMMEDIATE or repeat trigger
     */
    public Boolean isTriggerImmediateorRepeat(Boolean trigPress) {
        if (trigPress && Application.settings_startTrigger.getTriggerType().toString().equalsIgnoreCase(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE.toString())
                && (!Application.settings_stopTrigger.getTriggerType().toString().equalsIgnoreCase(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_HANDHELD_WITH_TIMEOUT.toString()))
        ) {
            return true;
        } else if (!trigPress && !Application.settings_startTrigger.getTriggerType().toString().equalsIgnoreCase(START_TRIGGER_TYPE.START_TRIGGER_TYPE_HANDHELD.toString())
                && (Application.settings_stopTrigger.getTriggerType().toString().equalsIgnoreCase(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE.toString()))
        ) {
            return true;
        } else
            return false;
    }

    /**
     * method to send connect command request to reader
     * after connect button clicked on connect password dialog
     *
     * @param password     - reader password
     * @param readerDevice
     */
    public void connectClicked(String password, ReaderDevice readerDevice) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (fragment instanceof ReadersListFragment) {
            ((ReadersListFragment) fragment).ConnectwithPassword(password, readerDevice);
        }
    }

    /**
     * method which will exe cute after cancel button clicked on connect pwd dialog
     *
     * @param readerDevice
     */
    public void cancelClicked(ReaderDevice readerDevice) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
        if (fragment instanceof ReadersListFragment) {
            ((ReadersListFragment) fragment).readerDisconnected(readerDevice, true);
        }
    }

    public void startbeepingTimer() {
        if (Application.beeperVolume != BEEPER_VOLUME.QUIET_BEEP) {
            if (!beepON) {
                beepON = true;
                beep();
                if (tbeep == null) {
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            stopbeepingTimer();
                            beepON = false;
                        }
                    };
                    tbeep = new Timer();
                    tbeep.schedule(task, 10);
                }
            }
        }
    }

    /**
     * method to stop timer
     */
    public void stopbeepingTimer() {
        if (tbeep != null) {
            Application.toneGenerator.stopTone();
            tbeep.cancel();
            tbeep.purge();
        }
        tbeep = null;
    }

    public void beep() {
        int toneType = ToneGenerator.TONE_PROP_BEEP;
        Application.toneGenerator.startTone(toneType);
    }

    public void startlocatebeepingTimer(int proximity) {
        if (Application.beeperVolume != BEEPER_VOLUME.QUIET_BEEP) {
            int POLLING_INTERVAL1 = BEEP_DELAY_TIME_MIN + (((BEEP_DELAY_TIME_MAX - BEEP_DELAY_TIME_MIN) * (100 - proximity)) / 100);
            if (!beepONLocate) {
                beepONLocate = true;
                beep();
                if (locatebeep == null) {
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            stoplocatebeepingTimer();
                            beepONLocate = false;
                        }
                    };
                    locatebeep = new Timer();
                    locatebeep.schedule(task, POLLING_INTERVAL1, 10);
                }
            }
        }
    }

    /**
     * method to stop timer locate beep
     */
    public void stoplocatebeepingTimer() {
        if (locatebeep != null) {
            Application.toneGenerator.stopTone();
            locatebeep.cancel();
            locatebeep.purge();
        }
        locatebeep = null;
    }

    public class EventHandler implements RfidEventsListener {

        @Override
        public void eventReadNotify(RfidReadEvents e) {
            final TagData[] myTags = Application.mConnectedReader.Actions.getReadTags(100);
            if (myTags != null) {
                //Log.d("RFID_EVENT","l: "+myTags.length);
                final Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
                for (int index = 0; index < myTags.length; index++) {
                    if (myTags[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                            myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                    }
                    if (myTags[index].isContainsLocationInfo()) {
                        final int tag = index;
                        Application.TagProximityPercent = myTags[tag].LocationInfo.getRelativeDistance();
                        if (Application.TagProximityPercent > 0) {
                            startlocatebeepingTimer(Application.TagProximityPercent);
                        }
                        if (fragment instanceof LocationingFragment)
                            ((LocationingFragment) fragment).handleLocateTagResponse();
                    } else {
                        if (Application.isAccessCriteriaRead && !Application.mIsInventoryRunning) {
                            accessTagCount++;
                        } else {
                            if (myTags[index] != null && (myTags[index].getOpStatus() == null || myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS)) {
                                final int tag = index;

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Application.TAG_LIST_MATCH_MODE)
                                            new MatchingTagsResponseHandlerTask(myTags[tag], fragment).execute();
                                        else
                                            new ResponseHandlerTask(myTags[tag], fragment).execute();
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
            notificationFromGenericReader(rfidStatusEvents);
        }
    }

    /**
     * Async Task, which will handle tag data response from reader. This task is used to check whether tag is in inventory list or not.
     * If tag is not in the list then it will add the tag data to inventory list. If tag is there in inventory list then it will update the tag details in inventory list.
     */
    public class ResponseHandlerTask extends AsyncTask<Void, Void, Boolean> {
        private TagData tagData;
        private InventoryListItem inventoryItem;
        private InventoryListItem oldObject;
        private Fragment fragment;
        private String memoryBank;
        private String memoryBankData;

        ResponseHandlerTask(TagData tagData, Fragment fragment) {
            this.tagData = tagData;
            this.fragment = fragment;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean added = false;
            try {
                if (Application.inventoryList.containsKey(tagData.getTagID())) {
                    inventoryItem = new InventoryListItem(tagData.getTagID(), 1, null, null, null, null, null, null);
                    int index = Application.inventoryList.get(tagData.getTagID());
                    if (index >= 0) {
                        //Tag is already present. Update the fields and increment the count
                        if (tagData.getOpCode() != null)
                            if (tagData.getOpCode().toString().equalsIgnoreCase("ACCESS_OPERATION_READ")) {
                                memoryBank = tagData.getMemoryBank().toString();
                                memoryBankData = tagData.getMemoryBankData().toString();
                            }
                        oldObject = Application.tagsReadInventory.get(index);
                        int tagSeenCount = 0;
                        if (Integer.toString(tagData.getTagSeenCount()) != null)
                            tagSeenCount = tagData.getTagSeenCount();
                        if (tagSeenCount != 0) {
                            Application.TOTAL_TAGS += tagSeenCount;
                            oldObject.incrementCountWithTagSeenCount(tagSeenCount);
                        } else {
                            Application.TOTAL_TAGS++;
                            oldObject.incrementCount();
                        }
                        if (oldObject.getMemoryBankData() != null && !oldObject.getMemoryBankData().equalsIgnoreCase(memoryBankData))
                            oldObject.setMemoryBankData(memoryBankData);
                        if (pc)
                            oldObject.setPC(Integer.toHexString(tagData.getPC()));
                        if (phase)
                            oldObject.setPhase(Integer.toString(tagData.getPhase()));
                        if (channelIndex)
                            oldObject.setChannelIndex(Integer.toString(tagData.getChannelIndex()));
                        if (rssi)
                            oldObject.setRSSI(Integer.toString(tagData.getPeakRSSI()));
                    }
                } else {
                    //Tag is encountered for the first time. Add it.
                    if (Application.inventoryMode == 0 || (Application.inventoryMode == 1 && Application.UNIQUE_TAGS <= Constants.UNIQUE_TAG_LIMIT)) {
                        int tagSeenCount = 0;
                        if (Integer.toString(tagData.getTagSeenCount()) != null)
                            tagSeenCount = tagData.getTagSeenCount();
                        if (tagSeenCount != 0) {
                            Application.TOTAL_TAGS += tagSeenCount;
                            inventoryItem = new InventoryListItem(tagData.getTagID(), tagSeenCount, null, null, null, null, null, null);
                        } else {
                            Application.TOTAL_TAGS++;
                            inventoryItem = new InventoryListItem(tagData.getTagID(), 1, null, null, null, null, null, null);
                        }
                        added = Application.tagsReadInventory.add(inventoryItem);
                        if (added) {
                            Application.inventoryList.put(tagData.getTagID(), Application.UNIQUE_TAGS);
                            if (tagData.getOpCode() != null)

                                if (tagData.getOpCode().toString().equalsIgnoreCase("ACCESS_OPERATION_READ")) {
                                    memoryBank = tagData.getMemoryBank().toString();
                                    memoryBankData = tagData.getMemoryBankData().toString();

                                }
                            oldObject = Application.tagsReadInventory.get(Application.UNIQUE_TAGS);
                            oldObject.setMemoryBankData(memoryBankData);
                            oldObject.setMemoryBank(memoryBank);
                            if (pc)
                                oldObject.setPC(Integer.toHexString(tagData.getPC()));
                            if (phase)
                                oldObject.setPhase(Integer.toString(tagData.getPhase()));
                            if (channelIndex)
                                oldObject.setChannelIndex(Integer.toString(tagData.getChannelIndex()));
                            if (rssi)
                                oldObject.setRSSI(Integer.toString(tagData.getPeakRSSI()));
                            Application.UNIQUE_TAGS++;
                        }
                    }
                }
                // beep on each tag read
                startbeepingTimer();
            } catch (IndexOutOfBoundsException e) {
                //logAsMessage(TYPE_ERROR, TAG, e.getMessage());
                oldObject = null;
                added = false;
            } catch (Exception e) {
                // logAsMessage(TYPE_ERROR, TAG, e.getMessage());
                oldObject = null;
                added = false;
            }
            inventoryItem = null;
            memoryBank = null;
            memoryBankData = null;
            return added;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            cancel(true);
            if (oldObject != null && fragment instanceof ResponseHandlerInterfaces.ResponseTagHandler)
                ((ResponseHandlerInterfaces.ResponseTagHandler) fragment).handleTagResponse(oldObject, result);
            oldObject = null;
        }
    }

    protected class UpdateDisconnectedStatusTask extends AsyncTask<Void, Void, Boolean> {
        private final String device;
        // store current reader state
        private final ReaderDevice readerDevice;
        long disconnectedTime;
        boolean bConnected = false;

        public UpdateDisconnectedStatusTask(String device) {
            this.device = device;
            disconnectedTime = System.currentTimeMillis();
            // store current reader state
            readerDevice = Application.mConnectedDevice;
            //
            Application.mReaderDisappeared = null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (readerDevice != null && readerDevice.getName().equalsIgnoreCase(device)) {
                        readerDisconnected(readerDevice);
                    } else {
                        readerDisconnected(new ReaderDevice(device, null));
                    }
                }
            });
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (!Application.is_disconnection_requested && Application.AUTO_RECONNECT_READERS && readerDevice != null && device != null && device.equalsIgnoreCase(readerDevice.getName()) && readerDevice.getName().startsWith("RFD8500")) {
                if (isBluetoothEnabled()) {
                    int retryCount = 0;
                    while (!bConnected && retryCount < 10) {
                        if (isCancelled() || isDeviceDisconnected)
                            break;
                        try {
                            Thread.sleep(1000);
                            retryCount++;
                            if (Application.is_connection_requested || isCancelled())
                                break;
                            readerDevice.getRFIDReader().reconnect();
                            bConnected = true;
                            if (Application.mReaderDisappeared != null && Application.mReaderDisappeared.getName().equalsIgnoreCase(readerDevice.getName())) {
                                readerDevice.getRFIDReader().disconnect();
                                bConnected = false;
                                break;
                            }
                        } catch (InvalidUsageException e) {
                        } catch (OperationFailureException e) {
                            if (e.getResults() == RFIDResults.RFID_BATCHMODE_IN_PROGRESS) {
                                Application.isBatchModeInventoryRunning = true;
                                bConnected = true;
                            }
                            if (e.getResults() == RFIDResults.RFID_READER_REGION_NOT_CONFIGURED) {
                                try {
                                    readerDevice.getRFIDReader().disconnect();
                                    bConnected = false;
                                    break;
                                } catch (InvalidUsageException e1) {
                                    e1.printStackTrace();
                                } catch (OperationFailureException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return bConnected;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!isCancelled()) {
                if (result) {
                    if (readerDevice.getName().startsWith("RFD8500")) {
                        readerReconnected(readerDevice);
                        StoreConnectedReader();
                    }
                } else if (!Application.is_connection_requested) {
                    //sendNotification(Constants.ACTION_READER_CONN_FAILED, "Connection Failed!! was received");
                    try {
                        readerDevice.getRFIDReader().disconnect();
                    } catch (InvalidUsageException e) {
                        e.printStackTrace();
                    } catch (OperationFailureException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    void StoreConnectedReader() {
        if (Application.AUTO_RECONNECT_READERS) {
            LAST_CONNECTED_READER = Application.mConnectedReader.getHostName();
            SharedPreferences settings = getSharedPreferences(Constants.APP_SETTINGS_STATUS, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Constants.LAST_READER, LAST_CONNECTED_READER);
            editor.commit();
        }
    }

    /**
     * Scanner and RFID trigger mode enable and disable task
     */
    public class ScanEnableTask extends AsyncTask<Void, Void, Boolean> {

        private Boolean enable;

        ScanEnableTask(Boolean scanenable) {
            enable = scanenable;
        }

        @Override
        protected Boolean doInBackground(Void... a) {
            try {
                if (enable) {
                    //onPause
                    if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected())
                        Application.mConnectedReader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, false);
                } else {
                    //onResume
                    if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected())
                        Application.mConnectedReader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);
                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onCancelled() {
            scanEnableTask = null;
            super.onCancelled();
        }
    }

    private boolean m_ScreenOn = true;
    // Broadcast receiver to receive the scanner_status, and disable the scanner
    public BroadcastReceiver BroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_SCREEN_OFF:
                    //Log.d(TAG, "BroadcastReceiver: " + action + " " + Application.mConnectedReader.getHostName());
                    if (!Application.mIsInventoryRunning)
                        m_ScreenOn = false;
                    break;
                case ACTION_SCREEN_ON:
                    //Log.d(TAG, "BroadcastReceiver: " + action + " " + Application.mConnectedReader.getHostName());
                    m_ScreenOn = true;
                    break;
                case scanner_status:
                    //Log.d(TAG, intent.getExtras().getString("STATUS"));
                    break;
                case DW_APIRESULT_ACTION: {
                    String command = intent.getStringExtra("COMMAND");
                    String commandidentifier = intent.getStringExtra("COMMAND_IDENTIFIER");
                    String result = intent.getStringExtra("RESULT");

                    if (command != null && command.equals("com.symbol.datawedge.api.SET_CONFIG")) {
                        if (commandidentifier.equals(RFID_DEMO_SCANNER_DISABLE)) {
                            Bundle bundle = new Bundle();
                            String resultInfo = "";
                            if (intent.hasExtra("RESULT_INFO")) {
                                bundle = intent.getBundleExtra("RESULT_INFO");
                                resultInfo = bundle.getString("RESULT_CODE");
                            }
                            if (result.equals("SUCCESS")) {
                                Log.d(TAG, "Scanner Disabled");
                            } else {
                                Log.d(TAG, "Failed to Disable scanner " + resultInfo);
                            }
                            Set<String> keys = bundle.keySet();
                            resultInfo = "";
                            for (String key : keys) {
                                resultInfo += key + ": " + bundle.getString(key) + "\n";
                            }
                            Log.d(TAG, "Disable scanner " + resultInfo);
                        }
                    }
                }
                break;
            }
        }
    };

    private void createDWProfile() {
        // MAIN BUNDLE PROPERTIES
        Bundle bMain = new Bundle();
        bMain.putString("PROFILE_NAME", "RFIDMobileApp");
        bMain.putString("PROFILE_ENABLED", "true");              // <- that will be enabled
        bMain.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");   // <- or created if necessary.
        // PLUGIN_CONFIG BUNDLE PROPERTIES
        Bundle bConfig = new Bundle();
        bConfig.putString("PLUGIN_NAME", "BARCODE"); // barcode plugin
        bConfig.putString("RESET_CONFIG", "true");
        // PARAM_LIST BUNDLE PROPERTIES
        Bundle bParams = new Bundle();
        bParams.putString("scanner_selection", "auto");
        bParams.putString("scanner_input_enabled", "false"); // Mainly disable scanner plugin
        // NEST THE BUNDLE "bParams" WITHIN THE BUNDLE "bConfig"
        bConfig.putBundle("PARAM_LIST", bParams);
        // THEN NEST THE "bConfig" BUNDLE WITHIN THE MAIN BUNDLE "bMain"
        ArrayList<Bundle> bundleArrayList = new ArrayList<>();
        bundleArrayList.add(bConfig);
        // following requires arrayList
        bMain.putParcelableArrayList("PLUGIN_CONFIG", bundleArrayList);
        // CREATE APP_LIST BUNDLES (apps and/or activities to be associated with the Profile)
        Bundle ActivityList = new Bundle();
        ActivityList.putString("PACKAGE_NAME", getPackageName());      // Associate the profile with this app
        ActivityList.putStringArray("ACTIVITY_LIST", new String[]{"*"});
        // NEXT APP_LIST BUNDLE(S) INTO THE MAIN BUNDLE
        bMain.putParcelableArray("APP_LIST", new Bundle[]{
                ActivityList
        });
        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        i.putExtra("SEND_RESULT", "true");
        i.putExtra("COMMAND_IDENTIFIER", RFID_DEMO_SCANNER_DISABLE);
        this.sendBroadcast(i);
    }

    public static InputFilter filter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (!isAllowed(source.charAt(i)))
                    return "";
            }
            return null;
        }

        String allowed = "0123456789ABCDEFabcdef";

        private boolean isAllowed(char c) {
            if (Application.asciiMode == false) {
                for (char ch : allowed.toCharArray()) {
                    if (ch == c)
                        return true;
                }
                return false;
            }
            return true;
        }
    };

    private class MatchingTagsResponseHandlerTask extends AsyncTask<Void, Void, Boolean> {

        private TagData tagData;
        private InventoryListItem inventoryItem;
        private InventoryListItem oldObject;
        private Fragment fragment;
        private String memoryBank;
        private String memoryBankData;
        //private Toast myToast;

        MatchingTagsResponseHandlerTask(TagData tagData, Fragment fragment) {
            this.tagData = tagData;
            this.fragment = fragment;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            //TODO: background task with UI thread runnable requires fix, all lists are assigned to list adapter so always run from ui thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean added = false;
                    final Toast myToast;

                    //Application.isCSVtagsLoaded=true;
                    //Toast.makeText(getApplicationContext(), getResources().getString(R.string.tag_match_complete), Toast.LENGTH_SHORT).show();
                    try {
                        if (Application.inventoryList.containsKey(tagData.getTagID())) {
                            inventoryItem = new InventoryListItem(tagData.getTagID(), 1, null, null, null, null, null, null);
                            int index = Application.inventoryList.get(tagData.getTagID());
                            if (index >= 0) {
                                if (Application.tagListMap.containsKey(tagData.getTagID()))
                                    Application.tagsReadInventory.get(index).setTagStatus("MATCH");
                                else
                                    Application.tagsReadInventory.get(index).setTagStatus("UNKNOWN");
                                Application.TOTAL_TAGS++;
                                //Tag is already present. Update the fields and increment the count
                                if (tagData.getOpCode() != null)
                                    if (tagData.getOpCode().toString().equalsIgnoreCase("ACCESS_OPERATION_READ")) {
                                        memoryBank = tagData.getMemoryBank().toString();
                                        memoryBankData = tagData.getMemoryBankData().toString();
                                    }

                                if (Application.memoryBankId == 1) {  //matching tags
                                    if (Application.tagListMap.containsKey(tagData.getTagID()) && !Application.matchingTagsList.contains(Application.tagsReadInventory.get(index))) {
                                        Application.matchingTagsList.add(Application.tagsReadInventory.get(index));
                                        Application.tagsReadForSearch.add(Application.tagsReadInventory.get(index));
                                        added = true;
                                    }
                                } else if (Application.memoryBankId == 2 && Application.tagListMap.containsKey(tagData.getTagID())) {
                                    if (Application.missingTagsList.contains(Application.tagsReadInventory.get(index))) {
                                        Application.missingTagsList.remove(Application.tagsReadInventory.get(index));
                                        Application.tagsReadForSearch.remove(Application.tagsReadInventory.get(index));
                                        added = true;
                                    }
                                }
                                oldObject = Application.tagsReadInventory.get(index);
                                if (oldObject.getCount() == 0) {
                                    Application.missedTags--;
                                    Application.matchingTags++;
                                    Application.UNIQUE_TAGS++;
                                }
                                oldObject.incrementCount();
                                if (oldObject.getMemoryBankData() != null && !oldObject.getMemoryBankData().equalsIgnoreCase(memoryBankData))
                                    oldObject.setMemoryBankData(memoryBankData);
                                //oldObject.setEPCId(inventoryItem.getEPCId());

                                oldObject.setPC(Integer.toString(tagData.getPC()));
                                oldObject.setPhase(Integer.toString(tagData.getPhase()));
                                oldObject.setChannelIndex(Integer.toString(tagData.getChannelIndex()));
                                oldObject.setRSSI(Integer.toString(tagData.getPeakRSSI()));
                            }
                        } else {

                            //Tag is encountered for the first time. Add it.
                            if (Application.inventoryMode == 0 || (Application.inventoryMode == 1 && Application.UNIQUE_TAGS_CSV <= Constants.UNIQUE_TAG_LIMIT)) {
                                int tagSeenCount = tagData.getTagSeenCount();
                                if (tagSeenCount != 0) {
                                    Application.TOTAL_TAGS += tagSeenCount;
                                    inventoryItem = new InventoryListItem(tagData.getTagID(), tagSeenCount, null, null, null, null, null, null);
                                } else {
                                    Application.TOTAL_TAGS++;
                                    inventoryItem = new InventoryListItem(tagData.getTagID(), 1, null, null, null, null, null, null);
                                }
                                if (Application.tagListMap.containsKey(tagData.getTagID()))
                                    inventoryItem.setTagStatus("MATCH");
                                else
                                    inventoryItem.setTagStatus("UNKNOWN");
                                if (Application.memoryBankId == 1)
                                    Application.tagsReadInventory.add(inventoryItem);
                                else if (Application.memoryBankId == 3) {
                                    inventoryItem.setTagDetails("unknown");
                                    added = Application.tagsReadInventory.add(inventoryItem);
                                    Application.unknownTagsList.add(inventoryItem);
                                    Application.tagsReadForSearch.add(inventoryItem);
                                } else {
                                    if (inventoryItem.getTagDetails() == null) {
                                        inventoryItem.setTagDetails("unknown");
                                    }
                                    added = Application.tagsReadInventory.add(inventoryItem);
                                    if (Application.memoryBankId != 2)
                                        Application.tagsReadForSearch.add(inventoryItem);
                                }

                                if (added || Application.memoryBankId == 1) {
                                    Application.inventoryList.put(tagData.getTagID(), Application.UNIQUE_TAGS_CSV);
                                    if (tagData.getOpCode() != null)
                                        if (tagData.getOpCode().toString().equalsIgnoreCase("ACCESS_OPERATION_READ")) {
                                            memoryBank = tagData.getMemoryBank().toString();
                                            memoryBankData = tagData.getMemoryBankData().toString();
                                        }
                                    oldObject = Application.tagsReadInventory.get(Application.UNIQUE_TAGS_CSV);
                                    oldObject.setMemoryBankData(memoryBankData);
                                    oldObject.setMemoryBank(memoryBank);
                                    oldObject.setPC(Integer.toString(tagData.getPC()));
                                    oldObject.setPhase(Integer.toString(tagData.getPhase()));
                                    oldObject.setChannelIndex(Integer.toString(tagData.getChannelIndex()));
                                    oldObject.setRSSI(Integer.toString(tagData.getPeakRSSI()));
                                    Application.UNIQUE_TAGS++;
                                    Application.UNIQUE_TAGS_CSV++;
                                }
                            }
                        }
                        // beep on each tag read
                        startbeepingTimer();
                    } catch (IndexOutOfBoundsException e) {
                        //logAsMessage(TYPE_ERROR, TAG, e.getMessage());
                        oldObject = null;
                        added = false;
                    } catch (Exception e) {
                        //logAsMessage(TYPE_ERROR, TAG, e.getMessage());
                        oldObject = null;
                        added = false;
                    }
                    tagData = null;
                    inventoryItem = null;
                    memoryBank = null;
                    memoryBankData = null;
                    // call notifyDataSetChanged from same runnalbe instead of onPostExecute
                    if (oldObject != null && fragment instanceof ResponseHandlerInterfaces.ResponseTagHandler)
                        ((ResponseHandlerInterfaces.ResponseTagHandler) fragment).handleTagResponse(oldObject, false);
                    if (Application.matchingTags != 0 && Application.missedTags == 0 && !Application.tagListMatchNotice) {
                        myToast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.tag_match_complete), Toast.LENGTH_LONG);
                        Application.tagListMatchNotice = true;
                        myToast.show();
                        if (Application.tagListMatchAutoStop) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Button inventoryButton = (Button) findViewById(R.id.inventoryButton);
                                    if (inventoryButton != null) {
                                        if (Application.mIsInventoryRunning) {
                                            inventoryButton.performClick();
                                            //inventoryStartOrStop(inventoryButton);
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            });
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
//            if (oldObject != null && fragment instanceof ResponseHandlerInterfaces.ResponseTagHandler)
//                ((ResponseHandlerInterfaces.ResponseTagHandler) fragment).handleTagResponse(oldObject, result);
            oldObject = null;
        }

    }

    public void selectNavigationMenuItem(int pos) {
        navigationView.getMenu().getItem(pos).setChecked(true);
    }

}
