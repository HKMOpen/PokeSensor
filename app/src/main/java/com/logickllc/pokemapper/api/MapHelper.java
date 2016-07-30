package com.logickllc.pokemapper.api;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MapHelper {
    protected Features features;
    public final static int NUM_SCAN_SECTORS = 9;
    protected double currentLat;
    protected double currentLon;
    protected boolean searched = false;
    protected boolean abortScan = false;
    protected int scanTime;
    protected int scanDistance;
    protected ArrayList<NearbyPokemonGPS> totalNearbyPokemon = new ArrayList<NearbyPokemonGPS>();
    protected HashSet<Long> totalEncounters = new HashSet<Long>();
    protected HashSet<Long> totalWildEncounters = new HashSet<Long>();
    protected ArrayList<Long> noTimes = new ArrayList<Long>();
    protected final String TAG = "PokeFinder";
    protected int failedScanLogins = 0;
    public static final int LOCATION_UPDATE_INTERVAL = 5000;
    protected boolean locationOverride = false;
    public static final int MAX_SCAN_RADIUS = 70;
    public static final int MAX_SCAN_DISTANCE = 120;
    public static final int DEFAULT_SCAN_DISTANCE = 50;
    public static final int DEFAULT_SCAN_TIME = 15;
    protected boolean locationInitialized = false;
    protected Timer countdownTimer;
    protected ConcurrentHashMap<Long, WildPokemonTime> pokeTimes = new ConcurrentHashMap<Long, WildPokemonTime>();

    public boolean isLocationOverridden() {
        return locationOverride;
    }

    public void setLocationOverride(boolean locationOverride) {
        this.locationOverride = locationOverride;
    }

    public boolean isLocationInitialized() {
        return locationInitialized;
    }

    public void setLocationInitialized(boolean locationInitialized) {
        this.locationInitialized = locationInitialized;
    }

    public boolean isSearched() {
        return searched;
    }

    public void setSearched(boolean searched) {
        this.searched = searched;
    }

    public double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(double currentLat) {
        this.currentLat = currentLat;
    }

    public double getCurrentLon() {
        return currentLon;
    }

    public void setCurrentLon(double currentLon) {
        this.currentLon = currentLon;
    }

    public boolean isAbortScan() {
        return abortScan;
    }

    public void setAbortScan(boolean abortScan) {
        this.abortScan = abortScan;
    }

    public int getScanTime() {
        return scanTime;
    }

    public void setScanTime(int scanTime) {
        this.scanTime = scanTime;
    }

    public int getScanDistance() {
        return scanDistance;
    }

    public void setScanDistance(int scanDistance) {
        this.scanDistance = scanDistance;
    }

    public int getFailedScanLogins() {
        return failedScanLogins;
    }

    public void setFailedScanLogins(int failedScanLogins) {
        this.failedScanLogins = failedScanLogins;
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }


    public String getTimeString(long time) {
        String timeString = (time / 60) + ":" + String.format("%02d", time % 60);
        return timeString;
    }

    public synchronized void moveMe(double lat, double lon, boolean repositionCamera, boolean reZoom) {

    }

    public synchronized void showPokemonAt(String name, LatLng loc, long encounterid, boolean hasTime) {

    }

    public abstract void wideScan();
    public abstract boolean scanForPokemon(double lat, double lon);
}
