package com.zebra.rfidreader.demo.access_operations;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfidreader.demo.R;
import com.zebra.rfidreader.demo.application.Application;
import com.zebra.rfidreader.demo.common.asciitohex;
import com.zebra.rfidreader.demo.common.hextoascii;

import static com.zebra.rfidreader.demo.home.MainActivity.filter;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * <p/>
 * Use the {@link AccessOperationsKillFragment#newInstance} factory method to
 * create an instance of this fragment.
 * <p/>
 * Fragment to handle the Access Kill operation.
 */
public class AccessOperationsKillFragment extends Fragment implements AccessOperationsFragment.OnRefreshListener {
    private AutoCompleteTextView tagIDField;
    private ArrayAdapter<String> adapter;

    public AccessOperationsKillFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AccessOperationsKillFragment.
     */
    public static AccessOperationsKillFragment newInstance() {
        return new AccessOperationsKillFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_access_operations_kill, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tagIDField = ((AutoCompleteTextView) getActivity().findViewById(R.id.accessKillTagID));
        Application.updateTagIDs();
        adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, Application.tagIDs);
        tagIDField.setAdapter(adapter);
        if (Application.asciiMode == true) {
            tagIDField.setFilters(new InputFilter[]{filter});

        } else {
            tagIDField.setFilters(new InputFilter[]{filter, new InputFilter.AllCaps()});


        }

        if (Application.accessControlTag != null) {
            if (Application.asciiMode == true)
                tagIDField.setText(hextoascii.convert(Application.accessControlTag));
            else
                tagIDField.setText(Application.accessControlTag);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void handleTagResponse(final TagData response_tagData) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ACCESS_OPERATION_STATUS readAccessOperation = null;

                if (readAccessOperation != null) {
                    if (response_tagData.getOpStatus() != null && !response_tagData.getOpStatus().equals(ACCESS_OPERATION_STATUS.ACCESS_SUCCESS)) {
                        Toast.makeText(getActivity(), readAccessOperation.getValue(), Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(getActivity(), R.string.msg_kill_succeed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onUpdate() {
        if (isVisible() && tagIDField != null) {
            if(Application.asciiMode == true)
                Application.accessControlTag = asciitohex.convert(tagIDField.getText().toString());
            else
            Application.accessControlTag = tagIDField.getText().toString();
        }
    }

    @Override
    public void onRefresh() {
        if (Application.accessControlTag != null && tagIDField != null) {
            if (Application.asciiMode == true)
                tagIDField.setText(hextoascii.convert(Application.accessControlTag));
            else
                tagIDField.setText(Application.accessControlTag);
        }
    }
}
