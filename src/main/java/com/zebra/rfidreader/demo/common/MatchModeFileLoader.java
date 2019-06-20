package com.zebra.rfidreader.demo.common;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.util.Log;

import com.zebra.rfidreader.demo.application.Application;
import com.zebra.rfidreader.demo.inventory.InventoryListItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.FileObserver.CREATE;
import static android.os.FileObserver.DELETE;
import static android.os.FileObserver.MOVED_TO;
import static com.zebra.rfidreader.demo.application.Application.TAG;

public class MatchModeFileLoader {

    private static MatchModeFileLoader m_instance;
    private TagListObserver m_observer;
    // taglist support
    protected TagListDataImportTask tagListDataImportTask;

    private File root = Environment.getExternalStorageDirectory();
    private File dir = new File(root.getAbsolutePath() + Constants.RFID_FILE_DIR);
    private File file = new File(dir, Constants.TAG_MATCH_FILE_NAME);

    public MatchModeFileLoader() {
    }

    public static MatchModeFileLoader getInstance() {
        if (m_instance == null)
            m_instance = new MatchModeFileLoader();
        return m_instance;
    }

    private class TagListObserver extends FileObserver {
        public TagListObserver(String path, int i) {
            super(path, i);
        }

        @Override
        public void onEvent(int event, @Nullable String path) {
            Log.d(TAG, "observer " + event);
            if (!Application.mIsInventoryRunning && Application.TAG_LIST_MATCH_MODE) {
                if (event == DELETE) {
                    if (!file.exists()) {
                        Application.TAG_LIST_MATCH_MODE = false;
                    }
                } else if (event == CREATE && !Application.TAG_LIST_MATCH_MODE)
                    LoadMatchModeCSV();
                else if (event == MOVED_TO) {
                    LoadMatchModeCSV();
                }
            }
        }
    }

    public void LoadMatchModeCSV() {
        if (m_observer == null) {
            m_observer = new TagListObserver(dir.getPath(), CREATE | DELETE | MOVED_TO);
            m_observer.startWatching();
        }
        if (file.exists()) {
            Application.tagListMap.clear();
            Application.tagsListCSV.clear();
            //Application.TAG_LIST_MATCH_MODE = true;
            LoadCSV();
        } else {
            Application.TAG_LIST_MATCH_MODE = false;
        }
    }

    private void LoadCSV() {
        Log.d(TAG, "Loading CSV");
        tagListDataImportTask = new TagListDataImportTask();
        tagListDataImportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public boolean isImportTaskRunning() {
        if (tagListDataImportTask != null && !tagListDataImportTask.isCancelled())
            return true;
        return false;
    }


    public class TagListDataImportTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
//            Toast.makeText(getApplicationContext(), "Importing tag match data...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Importing tag match data");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                //Constants.logAsMessage(TYPE_DEBUG, TAG, "TagListDataImportTask");
                BufferedReader br = null;
                String line = "";
                String cvsSplitBy = ",";
                if (file.exists()) {
                    br = new BufferedReader(new FileReader(file));
                    int count = 0;
                    Log.d(TAG, "tag match data br");
                    // create regex pattern
                    Pattern pattern = Pattern.compile("^\\p{XDigit}+$");
                    Matcher matcher;
                    while ((line = br.readLine()) != null) {
                        // use comma as separator
                        String[] row = line.split(cvsSplitBy);
                        if (row.length != 0 && !row[0].isEmpty()) {
                            //matches("^.*[^a-zA-Z0-9 ].*$") returns true if there is any char other than alpha and nos
                            matcher = pattern.matcher(row[0]);
                            if (matcher.matches()) {
                                Application.tagListMap.put(row[0], count);
                                InventoryListItem inv = (new InventoryListItem(row[0], 0, null, null, null, null, null, null));
                                if (row.length >= 2)
                                    inv.setTagDetails(row[1]);
                                inv.setTagStatus("MISS");
                                Application.tagsListCSV.add(inv);
                                count++;
                            }
                        }
                        //Log.d(TAG,"#"+line);
                    }
                    if (Application.tagsListCSV.size() == 0) {
                        Application.TAG_LIST_MATCH_MODE = false;
                    }
                }
                return true;
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage());
                Application.TAG_LIST_MATCH_MODE = false;
                return false;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return false;
            } finally {
            }
        }


        @Override
        protected void onPostExecute(Boolean result) {
            cancel(true);
//            if (result)
//                Toast.makeText(getApplicationContext(), "tag match data has been imported", Toast.LENGTH_SHORT).show();
//            else
//                Toast.makeText(getApplicationContext(), "Failed to import tag match data", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Importing tag match data done");
        }

        /**
         * Checks if external storage is available to at least read
         */
        private boolean isExternalStorageReadable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) ||
                    Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                return true;
            }
            return false;
        }
    }

}
