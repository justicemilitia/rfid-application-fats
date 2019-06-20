package com.zebra.rfidreader.demo.common;

import com.zebra.rfid.api3.RFModeTableEntry;
import com.zebra.rfidreader.demo.application.Application;

import java.util.ArrayList;

public class LinkProfileUtil {

    ArrayList<String> simpleProfiles = new ArrayList<>();
    private ArrayList<LinkProfileItem> LinkProfileList = new ArrayList<>();
    private boolean minTari_1250 = false;
    private boolean stepTari_6300 = false;
    private static LinkProfileUtil instance;
    private int[] powerLevels;

    public LinkProfileUtil() {
        populateLinkeProfiles();
    }

    public static LinkProfileUtil getInstance() {
        if (instance == null)
            instance = new LinkProfileUtil();
        return instance;
    }

    public int getSelectedLinkProfilePosition(long rfModeTableIndex) {
        for (LinkProfileItem item : LinkProfileList)
            if (item.getModeTableEntry().getModeIdentifer() == rfModeTableIndex) {
                return item.getIndex();
            }
        return 0;
    }

    public int getSimpleProfileModeIndex(int simpleProfileIndex) {
        if (simpleProfileIndex == -1)
            simpleProfileIndex = 0;
        return getMatchingIndex(simpleProfiles.get(simpleProfileIndex));
    }

    public void populateLinkeProfiles() {
        RFModeTableEntry rfModeTableEntry = null;
        String profile;
        if (Application.mConnectedReader != null && Application.mConnectedReader.isConnected() && Application.rfModeTable != null) {
            LinkProfileList.clear();
            for (int i = 0; i < Application.rfModeTable.length(); i++) {
                rfModeTableEntry = Application.rfModeTable.getRFModeTableEntryInfo(i);
                int bdr = rfModeTableEntry.getBdrValue();
                int miller = 1;
                switch (rfModeTableEntry.getModulation().getValue()) {
                    case 1:
                        miller = 2;
                        break;
                    case 2:
                        miller = 4;
                        break;
                    case 3:
                        miller = 8;
                        break;
                }
                if (miller == 1) {
                    if (rfModeTableEntry.getMaxTariValue() == 668)
                        profile = "AUTOMAC" + " " + "668";
                    else
                        profile = "FM0" + " " + bdr * miller / 1000 + "K";
                } else
                    profile = "M" + miller + " " + bdr * miller / 1000 + "K";

                if (!simpleProfiles.contains(profile))
                    simpleProfiles.add(profile);
                LinkProfileList.add(new LinkProfileItem(simpleProfiles.indexOf(profile), profile, rfModeTableEntry));
                if (rfModeTableEntry.getMinTariValue() == 12500)
                    minTari_1250 = true;
                if (rfModeTableEntry.getStepTariValue() == 6300)
                    stepTari_6300 = true;
            }
            powerLevels = Application.mConnectedReader.ReaderCapabilities.getTransmitPowerLevelValues();
        }

    }

    public int getPowerLevelIndex(int powerLevel) {
        for (int i = 0; i < powerLevels.length; i++) {
            if (powerLevel == powerLevels[i]) {
                return i;
            }
        }
        return -1;
    }

    public int getMaxPowerIndex() {
        return powerLevels.length - 1;
    }

    public boolean isMinTari_1250() {
        return minTari_1250;
    }

    public boolean isStepTari_6300() {
        return stepTari_6300;
    }

    public ArrayList<String> getSimpleProfiles() {
        return simpleProfiles;
    }

    public int getMatchingIndex(String profileName) {
        int modeID = 0;
        for (LinkProfileItem item : LinkProfileList) {
            if (item.getProfile().equals(profileName)) {
                modeID = item.getModeTableEntry().getModeIdentifer();
                break;
            }
        }
        return modeID;
    }

    public int getMatchingIndex(String profile, String sTari, String sPIE) {
        int modeIndex = 0;
        int tari = new Integer(sTari);
        int pie = new Integer(sPIE);
        for (LinkProfileItem item : LinkProfileList) {
            if (item.getProfile().equals(profile) && item.getModeTableEntry().getPieValue() == pie
                    && item.getModeTableEntry().getMinTariValue() <= tari && tari <= item.getModeTableEntry().getMaxTariValue()) {
                modeIndex = item.getModeTableEntry().getModeIdentifer();
                return modeIndex;
            }
        }
        return -1;
    }

}

class LinkProfileItem {
    int mIndex;
    String mProfile;
    RFModeTableEntry modeTableEntry;

    public LinkProfileItem(int index, String sProfile, RFModeTableEntry rfModeTableEntry) {
        mIndex = index;
        mProfile = sProfile;
        modeTableEntry = rfModeTableEntry;
    }

    public RFModeTableEntry getModeTableEntry() {
        return modeTableEntry;
    }

    public int getIndex() {
        return mIndex;
    }

    public String getProfile() {
        return mProfile;
    }
}
