package com.zebra.rfidreader.demo.reader_connection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDResults;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfidreader.demo.R;
import com.zebra.rfidreader.demo.application.Application;
import com.zebra.rfidreader.demo.common.Constants;
import com.zebra.rfidreader.demo.common.CustomProgressDialog;
import com.zebra.rfidreader.demo.common.Inventorytimer;
import com.zebra.rfidreader.demo.home.MainActivity;
import com.zebra.rfidreader.demo.settings.AdvancedOptionsContent;
import com.zebra.rfidreader.demo.settings.SettingsDetailActivity;

import java.util.ArrayList;

import static com.zebra.rfidreader.demo.application.Application.LAST_CONNECTED_READER;
import static com.zebra.rfidreader.demo.settings.AdvancedOptionsContent.DPO_ITEM_INDEX;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * <p/>
 * Use the {@link ReadersListFragment#newInstance} factory method to
 * create an instance of this fragment.
 * <p/>
 * Fragment to maintain the list of readers
 */
public class ReadersListFragment extends Fragment {
    public static ArrayList<ReaderDevice> readersList = new ArrayList<>();
    private PasswordDialog passwordDialog;
    private DeviceConnectTask deviceConnectTask;
    private static final String RFD8500 = "RFD8500";
    private ReaderListAdapter readerListAdapter;
    private ListView pairedListView;
    private TextView tv_emptyView;
    private CustomProgressDialog progressDialog;
    private Activity activity = null;
    private static ReadersListFragment rlf = null;

    // The on-click listener for all devices in the ListViews
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {

        public void onItemClick(AdapterView<?> av, View v, int pos, long arg3) {
            // Get the device MAC address, which is the last 17 chars in the View
            ReaderDevice readerDevice = readerListAdapter.getItem(pos);
            if (Application.mConnectedReader == null) {

                if (deviceConnectTask == null || deviceConnectTask.isCancelled()) {
                    Application.is_connection_requested = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        deviceConnectTask = new DeviceConnectTask(readerDevice, "Connecting with " + readerDevice.getName(), getReaderPassword(readerDevice.getName()));
                        deviceConnectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        deviceConnectTask = new DeviceConnectTask(readerDevice, "Connecting with " + readerDevice.getName(), getReaderPassword(readerDevice.getName()));
                        deviceConnectTask.execute();
                    }
                }
            } else {
                {
                    if (Application.mConnectedReader.isConnected()) {
                        Application.is_disconnection_requested = true;
                        try {
                            Application.mConnectedReader.disconnect();
                        } catch (InvalidUsageException e) {
                            e.printStackTrace();
                        } catch (OperationFailureException e) {
                            e.printStackTrace();
                        }
                        //
                        ReaderDeviceDisConnected(Application.mConnectedDevice);
                        if (Application.NOTIFY_READER_CONNECTION)
                            sendNotification(Constants.ACTION_READER_DISCONNECTED, "Disconnected from " + Application.mConnectedReader.getHostName());
                        //
                        clearSettings();
                    }
                    if (!Application.mConnectedReader.getHostName().equalsIgnoreCase(readerDevice.getName())) {
                        Application.mConnectedReader = null;
                        if (deviceConnectTask == null || deviceConnectTask.isCancelled()) {
                            Application.is_connection_requested = true;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                deviceConnectTask = new DeviceConnectTask(readerDevice, "Connecting with " + readerDevice.getName(), getReaderPassword(readerDevice.getName()));
                                deviceConnectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            } else {
                                deviceConnectTask = new DeviceConnectTask(readerDevice, "Connecting with " + readerDevice.getName(), getReaderPassword(readerDevice.getName()));
                                deviceConnectTask.execute();
                            }
                        }
                    } else {
                        Application.mConnectedReader = null;
                    }
                }
            }
            // Create the result Intent and include the MAC address
        }
    };

    public ReadersListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ReadersListFragment.
     */
    public static ReadersListFragment newInstance() {
        return new ReadersListFragment();
    }

    public static ReadersListFragment getInstance() {
        if (rlf == null)
            rlf = new ReadersListFragment();

        return rlf;
    }

    private void clearSettings() {
        MainActivity.clearSettings();
        MainActivity.stopTimer();
        getActivity().invalidateOptionsMenu();
        Inventorytimer.getInstance().stopTimer();
        Application.mIsInventoryRunning = false;
        if (Application.mIsInventoryRunning) {
            Application.isBatchModeInventoryRunning = false;
        }
        if (Application.isLocatingTag) {
            Application.isLocatingTag = false;
        }
        //update dpo icon in settings list
        AdvancedOptionsContent.ITEMS.get(DPO_ITEM_INDEX).icon = R.drawable.title_dpo_disabled;
        Application.mConnectedDevice = null;
        Application.isAccessCriteriaRead = false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (activity == null)
            activity = (Activity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_readers_list, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_readers_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initializeViews();

        readersList.clear();
        loadPairedDevices();

        readerListAdapter = new ReaderListAdapter(getActivity(), R.layout.readers_list_item, readersList);

        if (readerListAdapter.getCount() == 0) {
            pairedListView.setEmptyView(tv_emptyView);
        } else
            pairedListView.setAdapter(readerListAdapter);

        pairedListView.setOnItemClickListener(mDeviceClickListener);
        pairedListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

    }

    private void initializeViews() {
        pairedListView = (ListView) getActivity().findViewById(R.id.bondedReadersList);
        tv_emptyView = (TextView) getActivity().findViewById(R.id.empty);
    }

    private void loadPairedDevices() {
        new AsyncTask<Void, Void, Boolean>() {
            InvalidUsageException exception;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    ArrayList<ReaderDevice> readersListArray = Application.readers.GetAvailableRFIDReaderList();
                    readersList.addAll(readersListArray);
                } catch (InvalidUsageException ex) {
                    exception = ex;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (exception != null)
                    Toast.makeText(getActivity(), exception.getInfo(), Toast.LENGTH_SHORT).show();
                else {
                    if (Application.mConnectedDevice != null) {
                        int index = readersList.indexOf(Application.mConnectedDevice);
                        Log.d("APP", "index: " + index);
                        if (index != -1) {
                            readersList.remove(index);
                            readersList.add(index, Application.mConnectedDevice);
                        } else {
                            Application.mConnectedDevice = null;
                            Application.mConnectedReader = null;
                        }
                    }
                    if (readerListAdapter.getCount() != 0) {
                        tv_emptyView.setVisibility(View.GONE);
                        pairedListView.setAdapter(readerListAdapter);
                    }
                    readerListAdapter.notifyDataSetChanged();
                }
            }
        }.execute();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PasswordDialog.isDialogShowing) {
            if (passwordDialog == null || !passwordDialog.isShowing()) {
                showPasswordDialog(Application.mConnectedDevice);
            }
        }
        capabilitiesRecievedforDevice();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (passwordDialog != null && passwordDialog.isShowing()) {
            PasswordDialog.isDialogShowing = true;
            passwordDialog.dismiss();
        }
    }

    /**
     * method to update connected reader device in the readers list on device connected event
     *
     * @param device device to be updated
     */
    public void ReaderDeviceConnected(ReaderDevice device) {
//        if (deviceConnectTask != null)
//            deviceConnectTask.cancel(true);
        if (device != null) {
            Application.mConnectedDevice = device;
            Application.is_connection_requested = false;
            changeTextStyle(device);
        } else
            Constants.logAsMessage(Constants.TYPE_ERROR, "ReadersListFragment", "deviceName is null or empty");
    }

    public void ReaderDeviceDisConnected(ReaderDevice device) {
        if (deviceConnectTask != null && !deviceConnectTask.isCancelled() && deviceConnectTask.getConnectingDevice().getName().equalsIgnoreCase(device.getName())) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            if (deviceConnectTask != null)
                deviceConnectTask.cancel(true);
        }
        if (device != null) {
            changeTextStyle(device);
        } else
            Constants.logAsMessage(Constants.TYPE_ERROR, "ReadersListFragment", "deviceName is null or empty");
        MainActivity.clearSettings();
    }

    public void readerDisconnected(ReaderDevice device, boolean forceDisconnect) {
        if (device != null) {
            if (Application.mConnectedReader != null && (!Application.AUTO_RECONNECT_READERS || forceDisconnect)) {
                try {
                    Application.mConnectedReader.disconnect();
                } catch (InvalidUsageException e) {
                    e.printStackTrace();
                } catch (OperationFailureException e) {
                    e.printStackTrace();
                }
                Application.mConnectedReader = null;
            }
            for (int idx = 0; idx < readersList.size(); idx++) {
                if (readersList.get(idx).getName().equalsIgnoreCase(device.getName()))
                    changeTextStyle(readersList.get(idx));
            }
        }
    }

    /**
     * method to update reader device in the readers list on device connection failed event
     *
     * @param device device to be updated
     */
    public void ReaderDeviceConnFailed(ReaderDevice device) {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        if (deviceConnectTask != null)
            deviceConnectTask.cancel(true);
        if (device != null)
            changeTextStyle(device);
        else
            Constants.logAsMessage(Constants.TYPE_ERROR, "ReadersListFragment", "deviceName is null or empty");

        sendNotification(Constants.ACTION_READER_CONN_FAILED, "Connection Failed!! was received");

        Application.mConnectedReader = null;
        Application.mConnectedDevice = null;
    }

    /**
     * check/un check the connected/disconnected reader list item
     *
     * @param device device to be updated
     */
    private void changeTextStyle(final ReaderDevice device) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int i = readerListAdapter.getPosition(device);
                    if (i >= 0) {
                        readerListAdapter.remove(device);
                        readerListAdapter.insert(device, i);
                        readerListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    public void RFIDReaderAppeared(final ReaderDevice readerDevice) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (readerListAdapter != null && readerDevice != null) {

                        if (readerListAdapter.getCount() == 0) {
                            tv_emptyView.setVisibility(View.GONE);
                            pairedListView.setAdapter(readerListAdapter);
                        }
                        if (readersList.contains(readerDevice))
                            Log.d("RFIDDEMOAPP", "Duplicate reader");
                        else
                            readersList.add(readerDevice);

                        readerListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    public void RFIDReaderDisappeared(final ReaderDevice readerDevice) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (readerListAdapter != null && readerDevice != null) {

                        readerListAdapter.remove(readerDevice);
                        readersList.remove(readerDevice);
                        if (readerListAdapter.getCount() == 0) {
                            pairedListView.setEmptyView(tv_emptyView);
                        }
                        readerListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    /**
     * method to update serial and model of connected reader device
     */
    public void capabilitiesRecievedforDevice() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (readerListAdapter.getPosition(Application.mConnectedDevice) >= 0) {
                    ReaderDevice readerDevice = readerListAdapter.getItem(readerListAdapter.getPosition(Application.mConnectedDevice));
                    //readerDevice.setModel(Application.mConnectedDevice.getModel());
                    //readerDevice.setSerial(Application.mConnectedDevice.getSerial());
                    readerListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * method to show connect password dialog
     *
     * @param connectingDevice
     */
    public void showPasswordDialog(ReaderDevice connectingDevice) {
        if (Application.isActivityVisible()) {
            passwordDialog = new PasswordDialog(activity, connectingDevice);
            passwordDialog.show();
        } else
            PasswordDialog.isDialogShowing = true;
    }

    /**
     * method to cancel progress dialog
     */
    public void cancelProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        if (deviceConnectTask != null)
            deviceConnectTask.cancel(true);
    }

    public void ConnectwithPassword(String password, ReaderDevice readerDevice) {
        try {
            Application.mConnectedReader.disconnect();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
        deviceConnectTask = new DeviceConnectTask(readerDevice, "Connecting with " + readerDevice.getName(), password);
        deviceConnectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * method to get connect password for the reader
     *
     * @param address - device BT address
     * @return connect password of the reader
     */
    private String getReaderPassword(String address) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(Constants.READER_PASSWORDS, 0);
        return sharedPreferences.getString(address, null);
    }

    private void sendNotification(String action, String data) {
        if (activity != null) {
            if (activity.getTitle().toString().equalsIgnoreCase(getString(R.string.title_activity_settings_detail)) || activity.getTitle().toString().equalsIgnoreCase(getString(R.string.title_activity_readers_list)))
                ((SettingsDetailActivity) activity).sendNotification(action, data);
            else
                ((MainActivity) activity).sendNotification(action, data);
        }
    }

    void StoreConnectedReader() {
        if (Application.AUTO_RECONNECT_READERS) {
            LAST_CONNECTED_READER = Application.mConnectedReader.getHostName();
            SharedPreferences settings = getActivity().getSharedPreferences(Constants.APP_SETTINGS_STATUS, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Constants.LAST_READER, LAST_CONNECTED_READER);
            editor.commit();
        }
    }

    /**
     * async task to go for BT connection with reader
     */
    private class DeviceConnectTask extends AsyncTask<Void, String, Boolean> {
        private final ReaderDevice connectingDevice;
        private String prgressMsg;
        private OperationFailureException ex;
        private String password;

        DeviceConnectTask(ReaderDevice connectingDevice, String prgressMsg, String Password) {
            this.connectingDevice = connectingDevice;
            this.prgressMsg = prgressMsg;
            password = Password;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new CustomProgressDialog(activity, prgressMsg);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... a) {
            try {
                if (password != null)
                    connectingDevice.getRFIDReader().setPassword(password);
                connectingDevice.getRFIDReader().connect();
                if (password != null) {
                    SharedPreferences.Editor editor = activity.getSharedPreferences(Constants.READER_PASSWORDS, 0).edit();
                    editor.putString(connectingDevice.getName(), password);
                    editor.commit();
                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
                ex = e;
            }
            if (connectingDevice.getRFIDReader().isConnected()) {
                Application.mConnectedReader = connectingDevice.getRFIDReader();
                StoreConnectedReader();
                try {
                    Application.mConnectedReader.Events.addEventsListener(Application.eventHandler);
                } catch (InvalidUsageException e) {
                    e.printStackTrace();
                } catch (OperationFailureException e) {
                    e.printStackTrace();
                }
                connectingDevice.getRFIDReader().Events.setBatchModeEvent(true);
                connectingDevice.getRFIDReader().Events.setReaderDisconnectEvent(true);
                connectingDevice.getRFIDReader().Events.setBatteryEvent(true);
                connectingDevice.getRFIDReader().Events.setInventoryStopEvent(true);
                connectingDevice.getRFIDReader().Events.setInventoryStartEvent(true);
                Application.mConnectedReader.Events.setTagReadEvent(true);
                // if no exception in connect
                if (ex == null) {
                    try {
                        MainActivity.UpdateReaderConnection(false);
                    } catch (InvalidUsageException e) {
                        e.printStackTrace();
                    } catch (OperationFailureException e) {
                        e.printStackTrace();
                    }
                } else {
                    MainActivity.clearSettings();
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (activity != null && !activity.isFinishing())
                progressDialog.cancel();
            if (ex != null) {
                if (ex.getResults() == RFIDResults.RFID_CONNECTION_PASSWORD_ERROR) {
                    showPasswordDialog(connectingDevice);
                    ReaderDeviceConnected(connectingDevice);
                } else if (ex.getResults() == RFIDResults.RFID_BATCHMODE_IN_PROGRESS) {
                    Application.isBatchModeInventoryRunning = true;
                    Application.mIsInventoryRunning = true;
                    ReaderDeviceConnected(connectingDevice);
                    if (Application.NOTIFY_READER_CONNECTION)
                        sendNotification(Constants.ACTION_READER_CONNECTED, "Connected to " + connectingDevice.getName());
                } else if (ex.getResults() == RFIDResults.RFID_READER_REGION_NOT_CONFIGURED) {
                    ReaderDeviceConnected(connectingDevice);
                    Application.regionNotSet = true;
                    sendNotification(Constants.ACTION_READER_STATUS_OBTAINED, getString(R.string.set_region_msg));
                    Intent detailsIntent = new Intent(activity, SettingsDetailActivity.class);
                    detailsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    detailsIntent.putExtra(Constants.SETTING_ITEM_ID, R.id.regulatory);
                    startActivity(detailsIntent);
                } else
                    ReaderDeviceConnFailed(connectingDevice);
            } else {
                if (result) {
                    if (Application.NOTIFY_READER_CONNECTION)
                        sendNotification(Constants.ACTION_READER_CONNECTED, "Connected to " + connectingDevice.getName());
                    ReaderDeviceConnected(connectingDevice);
                } else {
                    ReaderDeviceConnFailed(connectingDevice);
                }
            }
            deviceConnectTask = null;
        }

        @Override
        protected void onCancelled() {
            deviceConnectTask = null;
            super.onCancelled();
        }

        public ReaderDevice getConnectingDevice() {
            return connectingDevice;
        }
    }
}
