package com.zebra.rfidreader.demo.settings;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.zebra.rfidreader.demo.R;
import com.zebra.rfidreader.demo.application.Application;
import com.zebra.rfidreader.demo.common.LinkProfileUtil;
import com.zebra.rfidreader.demo.settings.ProfileContent.ProfilesItem;


import java.util.ArrayList;
import java.util.List;

import static com.zebra.rfidreader.demo.application.Application.ActiveProfile;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ProfilesItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ProfileRecyclerViewAdapter extends RecyclerView.Adapter<ProfileRecyclerViewAdapter.ViewHolder> {

    private final List<ProfilesItem> mValues;
    private final OnListFragmentInteractionListener mListener;
    public ArrayAdapter<String> mLinkProfileAdapter = null;
    public ArrayAdapter<CharSequence> mSessionAdapter;
    public ArrayList<ViewHolder> mViewHolderAdapter = new ArrayList<>();
    public boolean isUserProfileEnable = false;
    private int[] powerLevels;
    LinkProfileUtil linkProfileUtil;

    public ProfileRecyclerViewAdapter(List<ProfilesItem> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        //
        mViewHolderAdapter.add(position, holder);
        //
        holder.mItem = mValues.get(position);
        holder.mContentView.setText(mValues.get(position).content);
        holder.enableUI(holder.mItem.bUIEnabled);
        if (Application.mIsInventoryRunning || Application.isLocatingTag)
            holder.contentSwitch.setEnabled(false);
        else
            holder.contentSwitch.setEnabled(true);
        //
        if (ActiveProfile.id.equals(holder.mItem.id))
            holder.mContentView.setTextColor(0xFFFF7043);
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LoadProfile(holder, position);
            }
        });

        holder.contentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                holder.mItem.powerLevel = new Integer(String.valueOf(holder.mtextPower.getText()));
                holder.mItem.LinkProfileIndex = holder.mLinkProfileSpinner.getSelectedItemPosition();
                holder.mItem.SessionIndex = holder.mSession.getSelectedItemPosition();
                holder.mItem.DPO_On = holder.mDynamicPower.isChecked();
                holder.mItem.isOn = isChecked;

                if (null != mListener) {
                    mListener.onProfileFragmentSwitchInteraction(holder.mItem, isChecked);
                }
                if (isChecked) {
                    for (int index = 0; index < mValues.size(); index++) {
                        if (index != position)
                            mValues.get(index).isOn = false;
                    }
                    holder.mContentView.setTextColor(0xFFFF7043);
                    // hide all
                    for (ViewHolder holder1 : mViewHolderAdapter) {
                        if (!ActiveProfile.id.equals(holder1.mItem.id))
                            holder1.mContentView.setTextColor(Color.BLACK);
                    }
                } else
                    holder.mContentView.setTextColor(Color.BLACK);
            }

        });

    }

    private void LoadProfile(ViewHolder holder, int position) {

        // show details
        holder.mtextViewDetails.setText(holder.mItem.details);
        // get current visibility
        int visibility = holder.mView.findViewById(R.id.profileSettings).getVisibility();
        // hide all
        for (ViewHolder holder1 : mViewHolderAdapter) {
            holder1.mView.findViewById(R.id.profileSettings).setVisibility(View.GONE);
            holder1.mView.findViewById(R.id.contentSwitch).setVisibility(View.GONE);
            if (!ActiveProfile.id.equals(holder1.mItem.id))
                holder1.mContentView.setTextColor(Color.BLACK);
        }
        // show current one
        if (visibility == View.GONE) {
            holder.mView.findViewById(R.id.profileSettings).setVisibility(View.VISIBLE);
            holder.mView.findViewById(R.id.contentSwitch).setVisibility(View.VISIBLE);
            //
            holder.mLinkProfileSpinner.setAdapter(mLinkProfileAdapter);
            holder.mSession.setAdapter(mSessionAdapter);

            if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected() && Application.mConnectedReader.Config.Antennas!= null && holder.mContentView.getText().equals("User Defined")) {
                powerLevels = Application.mConnectedReader.ReaderCapabilities.getTransmitPowerLevelValues();
                linkProfileUtil = LinkProfileUtil.getInstance();
                if (Application.antennaRfConfig != null) {
                    holder.mtextPower.setText(String.valueOf(ActiveProfile.powerLevel));
                    holder.mLinkProfileSpinner.setSelection(ActiveProfile.LinkProfileIndex);
                }
                if (Application.singulationControl != null)
                    holder.mSession.setSelection(ActiveProfile.SessionIndex);
                holder.mDynamicPower.setChecked(ActiveProfile.DPO_On);
            } else {
                holder.mtextPower.setText(new Integer(mValues.get(position).powerLevel).toString());
                holder.mSession.setSelection(mValues.get(position).SessionIndex);
                holder.mLinkProfileSpinner.setSelection(mValues.get(position).LinkProfileIndex);
                holder.mDynamicPower.setChecked(mValues.get(position).DPO_On);
            }
            if (ActiveProfile.id.equals(holder.mItem.id))
                holder.contentSwitch.setChecked(true);
            else
                holder.contentSwitch.setChecked(mValues.get(position).isOn);
            if (mValues.get(position).isOn)
                holder.mContentView.setTextColor(0xFFFF7043);
        } else {
            holder.mView.findViewById(R.id.profileSettings).setVisibility(View.GONE);
            holder.mView.findViewById(R.id.contentSwitch).setVisibility(View.GONE);
        }
        if (holder.mContentView.getText().equals("User Defined")) {
            isUserProfileEnable = true;
        } else {
            isUserProfileEnable = false;
        }

    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mContentView;
        public ProfilesItem mItem;
        public TextView mtextViewDetails;
        public EditText mtextPower;
        public Spinner mLinkProfileSpinner;
        public Spinner mSession;
        public CheckBox mDynamicPower;
        public SwitchCompat contentSwitch;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = view.findViewById(R.id.content);
            mtextViewDetails = view.findViewById(R.id.contentDetails);
            mtextPower = view.findViewById(R.id.powerLevelProfile);
            mLinkProfileSpinner = view.findViewById(R.id.linkProfile);
            mSession = view.findViewById(R.id.session);
            mDynamicPower = view.findViewById(R.id.dynamicPower);
            contentSwitch = view.findViewById(R.id.contentSwitch);
        }

        public void enableUI(boolean enabled) {
            mtextPower.setEnabled(enabled);
            mLinkProfileSpinner.setEnabled(enabled);
            mSession.setEnabled(enabled);
            mDynamicPower.setEnabled(enabled);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }

    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onProfileFragmentSwitchInteraction(ProfilesItem item, boolean isChecked);
    }

}
