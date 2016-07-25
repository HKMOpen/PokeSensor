package com.logickllc.pokemapper;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdTargetingOptions;
import com.amazon.device.ads.InterstitialAd;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.logickllc.pokemapper.api.NearbyPokemonGPS;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.Pokemon.CatchablePokemon;
import com.pokegoapi.auth.PTCLogin;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass;
import POGOProtos.Map.Pokemon.WildPokemonOuterClass;
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;
import okhttp3.OkHttpClient;

public class PokeFinderActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final String TAG = "PokeFinder";
    private final double FAIRPARK_LAT = 34.2563300;
    private final double FAIRPARK_LON = -88.7017310;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1776;
    private final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1337;
    private final String PREFS_NAME = "PokefinderPrefs";
    private final String PREF_TOKEN = "Token";
    public final static int NUM_SCAN_SECTORS = 9;
    private final String PREF_SCAN_DISTANCE = "ScanDistance";
    private final String PREF_SCAN_TIME = "ScanTime";
    private final String PREF_USERNAME = "ProfileName";
    private final String PREF_PASSWORD = "Nickname";
    private final String PREF_FIRST_LOAD = "FirstLoad";
    private final int DEFAULT_SCAN_DISTANCE = 50;
    private final int DEFAULT_SCAN_TIME = 15;
    private GoogleMap mMap;
    private GoogleApiClient client;
    private String token;
    private PokemonGo go;
    private ArrayList<NearbyPokemonGPS> totalNearbyPokemon = new ArrayList<NearbyPokemonGPS>();
    private HashSet<Long> totalEncounters = new HashSet<Long>();
    private HashSet<Long> totalWildEncounters = new HashSet<Long>();
    private double currentLat;
    private double currentLon;
    private ProgressDialog dialog;
    private String scanDialogMessage;
    private boolean searched = false;
    private LocationRequest request;
    private int LOCATION_UPDATE_INTERVAL = 5000;
    private Marker myMarker;
    private boolean loggingIn = false;
    private ProgressDialog progressDialog;
    private ArrayList<Marker> pokeMarkers = new ArrayList<Marker>();
    private int scanTime;
    private int scanDistance;
    private boolean locationOverride = false;
    private final int MAX_SCAN_RADIUS = 200;
    private Circle scanCircle;
    private boolean locationInitialized = false;
    private final float DEFAULT_ZOOM = 17f;
    private int failedScanLogins = 0;
    private Boolean serverAlive = null;
    private Menu menu;
    //private TourGuide mTutorialHandler;
    private boolean abortScan = false;
    private boolean abortLogin = false;


    // This manages all the timers I use and only lets them count down while the activity is in the foreground
    private AndroidTimerManager timerManager = new AndroidTimerManager();
    @SuppressWarnings("unused")
    private boolean timersPaused = true;
    private boolean isActivityVisible = true;

    //These are all related to ads
    public final boolean IS_AD_TESTING = false; // TODO Flag that determines whether to show test ads or live ads
    public static final boolean IS_AD_FREE = false; //Flag that determines whether or not to show ads. This will be true in ad-free version and false in the ad-enabled version
    public final String AMAZON_APP_ID = ""; //Need this for the ad impressions to be credited to me
    public final String ADMOB_BANNER_AD_ID = ""; //Need this to get credit for admob banner impressions
    public final String TEST_KINDLE_DEVICE_ID = ""; //This is just for testing with my tablet with admob
    public final String TEST_TABLET_DEVICE_ID = "";
    public final int AMAZON_BANNER_TIMEOUT = 10000; //Amount of time the banner will wait before the request expires and swaps to admob
    public boolean isPrimaryAdVisible = true; //Flag for if the Amazon banner is showing (not used currently)
    public boolean isSecondaryAdVisible = false; //Flag for if the Admob banner is showing (not used currently)
    //public Timer bannerUpdateTimer = new Timer(); //Reloads the primary banner periodically. Secondary banner refreshes itself
    public final long BANNER_REFRESH_RATE = 30000; //Amount of time it takes to reload banner. Should have enough ad providers to get ads filled at this rate
    public Timer interstitialTimer = new Timer(); //Sets flag that allows interstitial to be shown only after a certain time has passed
    public boolean primaryInterstitialFailed = false, secondaryInterstitialFailed = false; //Set when the interstitial fails to load
    public final long INTERSTITIAL_SHOW_RATE = 300000; //Controls how often interstitials will be allowed to show
    public boolean canShowInterstitial = false; //Flag for determining if interstitial can be shown when the app wants to show one
    public final String GOOGLE_INTERSTITIAL_AD_ID = ""; //Admob id for the interstitial. Need this to get credit for impressions
    public InterstitialAd primaryInterstitial; //Holds the Amazon interstitial
    public com.google.android.gms.ads.InterstitialAd secondaryInterstitial; //Holds the Admob interstitial
    public final int AMAZON_INTERSTITIAL_TIMEOUT = 30000; //How long it takes for an Amazon interstitial request to timeout
    public Timer retryInterstitialTimer = new Timer(); //Retries loading the interstitial after both fail
    public final long RETRY_INTERSTITIAL_DELAY = 90000; //Delay for interstitial retry timer
    public boolean primaryInterstitialLoaded = false, secondaryInterstitialLoaded = false; //Flag for whether interstitial has loaded yet
    public boolean isMiddleBannerLoaded = false; // Artifact from an early implementation attempt. Should just leave it alone to make sure not to break current implementation
    public Hashtable<String, Integer> adNetworkPositions = new Hashtable<String, Integer>();
    private static final Integer ADMOB_DEFAULT_POSITION = 2;
    private static final Integer AMAZON_DEFAULT_POSITION = 1;
    public long lastBannerLoad = 0;
    private int lastOrientation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lastOrientation = this.getResources().getConfiguration().orientation;
        Log.d(TAG, "PokeFinderActivity.onCreate()");
        setContentView(R.layout.activity_poke_finder);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapFragment.setRetainInstance(true);
        // Build Google API Location client
        client = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();

        // TODO Make sure the ad testing is set to false before submitting the app!
        if (IS_AD_TESTING && !IS_AD_FREE) {
            AdRegistration.enableTesting(true);
            //AdRegistration.enableLogging(true);
        }

        //Setup the app differently depending on if it's the ad-free version or not
        if (IS_AD_FREE)
            hideAds();
        else {
            initAds();
            showAds();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "PokeFinderActivity.onStart()");
        client.connect();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                loggedIn();
            }
        };
        runnable.run();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //turn the banners back on to refreshing if this is the ad version
        if (!IS_AD_FREE) {
            resumeAds();
            //timerManager.resumeTimers();
        }

        Log.d(TAG, "PokeFinderActivity.onResume()");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        scanDistance = preferences.getInt(PREF_SCAN_DISTANCE, DEFAULT_SCAN_DISTANCE);
        scanTime = preferences.getInt(PREF_SCAN_TIME, DEFAULT_SCAN_TIME);

        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            longMessage(R.string.noLocationDetected);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "PokeFinderActivity.onStop()");
        if (client != null && client.isConnected()) client.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "PokeFinderActivity.onPause()");
        if (!IS_AD_FREE) {
            // Make sure the timers are paused (the tasks are successfully cancelled)
            timersPaused = true;
            pauseAds();
            timersPaused = timerManager.pauseTimers();
        }
    }

    public void startInterstitialTimer() {
        //start the countdown until we can show another interstitial
        primaryInterstitialFailed = secondaryInterstitialFailed = false;
        primaryInterstitialLoaded = secondaryInterstitialLoaded = false;
        canShowInterstitial = false;

        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                canShowInterstitial = true;
            }

        };
        interstitialTimer = new Timer();
        interstitialTimer.schedule(task, INTERSTITIAL_SHOW_RATE);
        loadPrimaryInterstitial();
    }

    public void loadPrimaryInterstitial() {
        //load the Amazon interstitial
        primaryInterstitialLoaded = false;
        primaryInterstitialFailed = false;
        primaryInterstitial = new InterstitialAd(this);
        primaryInterstitial.setTimeout(AMAZON_INTERSTITIAL_TIMEOUT);
        primaryInterstitial.setListener(new AdListener() {

            @Override
            public void onAdCollapsed(Ad ad) { }

            @Override
            public void onAdDismissed(Ad ad) {
                startInterstitialTimer(); //start the countdown to show another interstitial
            }

            @Override
            public void onAdExpanded(Ad ad) { }

            @Override
            public void onAdFailedToLoad(Ad ad, AdError error) {
                primaryInterstitialFailed = true;
                loadSecondaryInterstitial();
            }

            @Override
            public void onAdLoaded(Ad ad, AdProperties adProperties) {
                primaryInterstitialLoaded = true;
            }

        });
        AdTargetingOptions options = new AdTargetingOptions();
        options.setAge(18);
        primaryInterstitial.loadAd(options);
    }

    public void loadSecondaryInterstitial() {
        //loads the Admob interstitial
        secondaryInterstitialLoaded = false;
        secondaryInterstitialFailed = false;
        secondaryInterstitial = new com.google.android.gms.ads.InterstitialAd(this);
        secondaryInterstitial.setAdUnitId(GOOGLE_INTERSTITIAL_AD_ID);
        secondaryInterstitial.setAdListener(new com.google.android.gms.ads.AdListener() {

            @Override
            public void onAdClosed() {
                startInterstitialTimer();
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                secondaryInterstitialFailed = true;
                startRetryInterstitialTimer(); //both interstitials failed so we'll retry again in a little bit
                super.onAdFailedToLoad(errorCode);
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
            }

            @Override
            public void onAdLoaded() {
                secondaryInterstitialLoaded = true;
                super.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
            }

        });
        // TODO Make sure this is set correctly before publishing
        AdRequest adRequest;
        if (!IS_AD_TESTING) {
            adRequest = new AdRequest.Builder()
                    .setBirthday(new GregorianCalendar(1998, 1, 1).getTime()) // Set it to target 18 year old males, just a wild guess
                    .setGender(AdRequest.GENDER_MALE)
                    .build();
        } else {
            adRequest = new AdRequest.Builder()
                    .setBirthday(new GregorianCalendar(1998, 1, 1).getTime()) // Set it to target 18 year old males, just a wild guess
                    .setGender(AdRequest.GENDER_MALE)
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR) // Emulator
                    .addTestDevice(TEST_TABLET_DEVICE_ID) // My test tablet
                    .addTestDevice(TEST_KINDLE_DEVICE_ID)
                    .build();
        }
        secondaryInterstitial.loadAd(adRequest);
    }

    public void startRetryInterstitialTimer() {
        //Log.e("Interstitial", "Starting interstitial retry timer for " + Long.toString(RETRY_INTERSTITIAL_DELAY) + " secs");
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                loadPrimaryInterstitial();
            }

        };
        retryInterstitialTimer = new Timer();
        retryInterstitialTimer.schedule(task, RETRY_INTERSTITIAL_DELAY);

    }

    public void showInterstitial() {
        //show interstitial if possible
        if (!canShowInterstitial) return;
        if (primaryInterstitialLoaded) {
            if (!primaryInterstitial.showAd()) {
                if (secondaryInterstitialLoaded) secondaryInterstitial.show();
            }
        } else if (secondaryInterstitialLoaded) {
            secondaryInterstitial.show();
        }
    }

    public void initAds() {
        adNetworkPositions.clear();
        adNetworkPositions.put("Admob", ADMOB_DEFAULT_POSITION);
        adNetworkPositions.put("Amazon", AMAZON_DEFAULT_POSITION);
        AdRegistration.setAppKey(AMAZON_APP_ID); //register this app so I get credit for my impressions
    }

    public void resumeAds() {
        //start the banner refreshing and try to load interstitial again if they both failed
        //if (primaryInterstitialFailed && secondaryInterstitialFailed) loadPrimaryInterstitial();
        loadNextBanner(null);
    }

    public void pauseAds() {
        //pause the ads so none of them are refreshing while the app is in the background
        //retryInterstitialTimer.cancel();
    }

    public void startBannerUpdateTimer() {
        final Activity act = this;
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // This line seems weird but it's just to make sure that the app is in the foreground.
                        // If the app tried to cancel the timers but it failed, timersPaused will be false and this
                        // will prevent an infinite cycle of ad-loading in the background. If the timers are successfully
                        // paused, this task will be cancelled so it will never reach this line.
                        if (isActivityVisible) loadNextBanner(null); // Try Amazon before anything else. They have the best CPM
                    }
                });
            }
        };
        AndroidTimer updateTimer = new AndroidTimer(task, BANNER_REFRESH_RATE, false);
        Log.d("Before clearing", Integer.toString(timerManager.getListSize()) + " timers");
        timerManager.clearInactiveTimers();
        Log.d("After clearing", Integer.toString(timerManager.getListSize()) + " timers");
        timerManager.addTimer(updateTimer);
        if (!isActivityVisible) timerManager.pauseTimers();
        lastBannerLoad = System.currentTimeMillis();
    }

    public void hideAds() {
        hideAmazonBanner();
        hideAdmobBanner();
        hideAdContainer();
    }

    public void hideAdContainer() {
        LinearLayout adContainer = (LinearLayout) findViewById(R.id.adContainer);
        adContainer.setVisibility(View.GONE);
    }

    public void showAds() {
        loadNextBanner(null); // This starts the cycle of Amazon and Admob. It's supposed to continue forever
        //startInterstitialTimer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation != lastOrientation) {
            if (!IS_AD_FREE) {
                hideAdmobBanner();
                hideAmazonBanner();
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                loadNextBanner(null);
                            }
                        };
                        runOnUiThread(r);
                    }
                };
                timer.schedule(task, 2000);
            }
        }
        lastOrientation = newConfig.orientation;
    }

    public void loadNextBanner(String currentBanner) {
        // Determine which ad network comes next in the waterfall
        // Later the network positions will be dynamically set from a server
        int nextNetwork;
        if (currentBanner == null) {
            nextNetwork = 1;
        } else {
            nextNetwork = (adNetworkPositions.get(currentBanner).intValue() % 2) + 1;
            if (nextNetwork == 1) {
                startBannerUpdateTimer();
                return;
            }
        }

        Set<String> networks = adNetworkPositions.keySet();
        for (String network : networks) {
            if (adNetworkPositions.get(network).intValue() == nextNetwork) {
                if (network.equals("Amazon")) {
                    loadAmazonBanner();
                } else if (network.equals("Admob")) {
                    loadAdmobBanner();
                } else {
                    loadAdmobBanner();
                }
                return;
            }
        }
        startBannerUpdateTimer();
    }

    public void loadAmazonBanner() {
        Log.d("AD", "Loading amazon");
        timerManager.cancelAllTimers();
        isPrimaryAdVisible = true;
        hideAdmobBanner();
        // Can use this to get a mix of Amazon and admob ads. Good for testing.
		/*if (new Random().nextBoolean()) {
			loadAdmobBanner();
			return;
		}*/
        AdLayout banner = getAmazonBannerInstance();
        AdTargetingOptions options = new AdTargetingOptions();
        options.setAge(18);
        banner.loadAd(options);
    }

    public AdLayout getAmazonBannerInstance() {
        //returns a fully configured instance of the primary banner
        final AdLayout banner = (AdLayout) findViewById(R.id.primaryBanner);
        Log.d("Amazon AD", "Banner height is " + Integer.toString(banner.getHeight()));
        //banner.setTimeout(1); // This forces the admob ads to show instead of amazon
        banner.bringToFront();
        banner.setVisibility(View.INVISIBLE); // We need to be able to get the width but don't want to see the previous ad
        //banner.requestLayout();
        banner.setListener(new AdListener() {

            @Override
            public void onAdCollapsed(Ad ad) {	}

            @Override
            public void onAdDismissed(Ad ad) {	}

            @Override
            public void onAdExpanded(Ad ad) {  }

            @Override
            public void onAdFailedToLoad(Ad ad, AdError error) {
                loadNextBanner("Amazon");
                //loadSecondaryBanner(); //try the secondary banner
            }

            @Override
            public void onAdLoaded(Ad ad, AdProperties adProperties) {
                banner.setVisibility(View.VISIBLE); // Now we can show the new ad that's been loaded
                //banner.requestLayout();
                startBannerUpdateTimer();
            }

        });

        return banner;
    }

    public void hideAmazonBanner() {
        AdLayout primaryBanner = (AdLayout) findViewById(R.id.primaryBanner);
        primaryBanner.setVisibility(View.GONE);
        isPrimaryAdVisible = false;
    }

    public void hideAdmobBanner() {
        AdView secondaryBanner = (AdView) findViewById(R.id.secondaryBanner);
        secondaryBanner.setVisibility(View.GONE);
        isSecondaryAdVisible = false;
    }

    public void loadAdmobBanner() {
        Log.d("AD", "Loading admob");
        timerManager.cancelAllTimers();
        isPrimaryAdVisible = false;
        hideAmazonBanner();
        AdView banner = getAdmobBannerInstance();
        // TODO Make sure this is set correctly before publishing
        AdRequest adRequest;
        if (!IS_AD_TESTING) {
            adRequest = new AdRequest.Builder()
                    .setBirthday(new GregorianCalendar(1998, 1, 1).getTime()) // Set it to target 18 year old males, just a wild guess
                    .setGender(AdRequest.GENDER_MALE)
                    .build();
        } else {
            adRequest = new AdRequest.Builder()
                    .setBirthday(new GregorianCalendar(1998, 1, 1).getTime()) // Set it to target 18 year old males, just a wild guess
                    .setGender(AdRequest.GENDER_MALE)
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR) // Emulator
                    .addTestDevice(TEST_TABLET_DEVICE_ID) // My test tablet
                    .addTestDevice(TEST_KINDLE_DEVICE_ID)
                    .build();
        }
        banner.loadAd(adRequest);
    }

    public AdView getAdmobBannerInstance() {
        //returns a fully configured instance of the secondary banner
        final AdView banner = (AdView) findViewById(R.id.secondaryBanner);
        banner.bringToFront();
        banner.setVisibility(View.INVISIBLE);
        //banner.requestLayout();
        banner.setAdListener(new com.google.android.gms.ads.AdListener() {

            @Override
            public void onAdClosed() {
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                loadNextBanner("Admob");
                super.onAdFailedToLoad(errorCode);
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
            }

            @Override
            public void onAdLoaded() {
                banner.setVisibility(View.VISIBLE);
                banner.requestLayout();
                startBannerUpdateTimer();
                super.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
            }

        });
        return banner;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_scan:
                wideScan();
                return true;

            case R.id.action_tuner:
                tuner();
                return true;

            case R.id.action_search:
                search();
                return true;

            case R.id.action_logout:
                logout();
                return true;

            /*case R.id.action_help:
                help();
                return true;*/

            case R.id.action_about:
                about();
                return true;

            case R.id.action_contactus:
                contactUs();
                return true;

            case R.id.action_twitter:
                twitter();
                return true;

            case R.id.action_facebook:
                facebook();
                return true;

            case R.id.action_moreapps:
                moreApps();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        this.menu = menu;
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (!IS_AD_FREE) {
            DisplayMetrics metrics = this.getResources().getDisplayMetrics();
            float screenWidth = metrics.widthPixels / metrics.density;
            float screenHeight = metrics.heightPixels / metrics.density;
            int googleLogoPadding = Math.round(90 * metrics.density) + 2;
            mMap.setPadding(5, 5, 5, googleLogoPadding);
        }

        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                locationOverride = true;
                moveMe(latLng.latitude, latLng.longitude, true);
            }
        });

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                locationOverride = false;
                initLocation();
                return false;
            }
        });

        mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));

        //moveMe(FAIRPARK_LAT, FAIRPARK_LON);
        //test();
    }

    public synchronized void moveMe(double lat, double lon, boolean repositionCamera) {
        // Add a marker in Sydney and move the camera
        LatLng me = new LatLng(lat, lon);
        if (myMarker != null) myMarker.remove();
        myMarker = mMap.addMarker(new MarkerOptions().position(me).title("Me"));
        if (repositionCamera) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, DEFAULT_ZOOM));
        currentLat = lat;
        currentLon = lon;
    }

    public synchronized void lockLogin() {
        if (!loginLocked()) loggingIn = true;
    }

    public synchronized void unlockLogin() {
        loggingIn = false;
    }

    public synchronized boolean loginLocked() {
        return loggingIn;
    }

    public void login() {
        if (!loginLocked()) lockLogin();
        else return;
        final Activity act = this;
        Thread loginThread = new Thread() {
            public void run() {
                Log.d(TAG, "Attempting to login...");
                try {
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(act);
                    token = preferences.getString(PREF_TOKEN, "");
                    if (token != "") {
                        final ProgressDialog tryingDialog = showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                        boolean trying = true;
                        int failCount = 0;
                        final int MAX_TRIES = 3;
                        while (trying) {
                            try {
                                Log.d(TAG, "Attempting to login with token: " + token);

                                OkHttpClient httpClient = new OkHttpClient();
                                RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth = null;
                                auth = new PTCLogin(httpClient).login(token);
                                go = new PokemonGo(auth, httpClient);
                                tryTalkingToServer(); // This will error if we can't reach the server
                                shortMessage(R.string.loginSuccessfulMessage);
                                unlockLogin();
                                progressDialog.dismiss();
                                return;
                            } catch (Exception e) {
                                if (++failCount < MAX_TRIES) {
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e1) {
                                        e1.printStackTrace();
                                    }
                                } else {
                                    e.printStackTrace();
                                    token = "";
                                    Log.d(TAG, "Erasing token because it seems to be expired.");
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString(PREF_TOKEN, token);
                                    editor.commit();
                                    //longMessage(R.string.loginFailedMessage);
                                    unlockLogin();
                                    progressDialog.dismiss();
                                    login();
                                    return;
                                }
                            }
                        }
                    } else {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                String pastUsername = preferences.getString(PREF_USERNAME, "");
                                String pastPassword = preferences.getString(PREF_PASSWORD, "");

                                if (!pastUsername.equals("") && !pastPassword.equals("")) {
                                    final String username = decode(pastUsername);
                                    final String password = decode(pastPassword);

                                    if (username.equals("") || password.equals("")) {
                                        // Erase username and pass and prompt for login again
                                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(act);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString(PREF_USERNAME, "");
                                        editor.putString(PREF_PASSWORD, "");
                                        editor.commit();
                                        unlockLogin();
                                        login();
                                        return;
                                    }

                                    Thread thread = new Thread() {
                                        @Override
                                        public void run() {
                                            final ProgressDialog tryingDialog = showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                                            boolean trying = true;
                                            int failCount = 0;
                                            final int MAX_TRIES = 10;
                                            while (trying) {
                                                OkHttpClient httpClient = new OkHttpClient();
                                                RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth = null;
                                                try {
                                                    Log.d(TAG, "Attempting to login with Username: " + username + " and password: " + password);

                                                    auth = new PTCLogin(httpClient).login(username, password);
                                                    go = new PokemonGo(auth, httpClient);
                                                    shortMessage(R.string.loginSuccessfulMessage);
                                                    token = auth.getToken().getContents();
                                                    Log.d(TAG, "Token: " + token);
                                                    SharedPreferences.Editor editor = preferences.edit();
                                                    editor.putString(PREF_TOKEN, token);
                                                    editor.commit();
                                                    unlockLogin();
                                                    progressDialog.dismiss();
                                                    return;
                                                } catch (LoginFailedException e) {
                                                    if (++failCount < MAX_TRIES) {
                                                        try {
                                                            Thread.sleep(3000);
                                                        } catch (InterruptedException e1) {
                                                            e1.printStackTrace();
                                                        }
                                                    } else {
                                                        e.printStackTrace();
                                                        longMessage(R.string.loginFailedMessage);
                                                        unlockLogin();
                                                        progressDialog.dismiss();
                                                        return;
                                                    }
                                                }
                                            }
                                        }
                                    };
                                    thread.start();
                                } else {

                                    // Show login screen
                                    AlertDialog.Builder builder = new AlertDialog.Builder(act);
                                    builder.setTitle(R.string.loginTitle);
                                    builder.setMessage(R.string.loginMessage);
                                    View view = getLayoutInflater().inflate(R.layout.login, null);
                                    builder.setView(view);

                                    final EditText username = (EditText) view.findViewById(R.id.username);
                                    final EditText password = (EditText) view.findViewById(R.id.password);
                                    final CheckBox rememberLogin = (CheckBox) view.findViewById(R.id.rememberLogin);
                                    final TextView createAccount = (TextView) view.findViewById(R.id.createAccountLink);
                                    createAccount.setText(getResources().getText(R.string.createAccountMessage));
                                    createAccount.setMovementMethod(LinkMovementMethod.getInstance());

                                    builder.setPositiveButton(R.string.loginButton, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Thread thread = new Thread() {
                                                @Override
                                                public void run() {
                                                    if (rememberLogin.isChecked()) {
                                                        // Boss gave us permission to store the credentials
                                                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(act);
                                                        SharedPreferences.Editor editor = preferences.edit();
                                                        editor.putString(PREF_USERNAME, encode(username.getText().toString()));
                                                        editor.putString(PREF_PASSWORD, encode(password.getText().toString()));
                                                        editor.commit();
                                                    }
                                                    final ProgressDialog tryingDialog = showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                                                    boolean trying = true;
                                                    int failCount = 0;
                                                    final int MAX_TRIES = 10;
                                                    while (trying) {
                                                        OkHttpClient httpClient = new OkHttpClient();
                                                        RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth = null;
                                                        try {
                                                            Log.d(TAG, "Attempting to login with Username: " + username.getText().toString() + " and password: " + password.getText().toString());

                                                            auth = new PTCLogin(httpClient).login(username.getText().toString(), password.getText().toString());
                                                            go = new PokemonGo(auth, httpClient);
                                                            shortMessage(R.string.loginSuccessfulMessage);
                                                            token = auth.getToken().getContents();
                                                            Log.d(TAG, "Token: " + token);
                                                            SharedPreferences.Editor editor = preferences.edit();
                                                            editor.putString(PREF_TOKEN, token);
                                                            editor.commit();
                                                            unlockLogin();
                                                            progressDialog.dismiss();
                                                            return;
                                                        } catch (LoginFailedException e) {
                                                            if (++failCount < MAX_TRIES) {
                                                                try {
                                                                    Thread.sleep(3000);
                                                                } catch (InterruptedException e1) {
                                                                    e1.printStackTrace();
                                                                }
                                                            } else {
                                                                e.printStackTrace();
                                                                longMessage(R.string.loginFailedMessage);
                                                                unlockLogin();
                                                                progressDialog.dismiss();
                                                                return;
                                                            }
                                                        }
                                                    }
                                                }
                                            };
                                            thread.start();
                                        }
                                    });
                                    builder.setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            unlockLogin();
                                        }
                                    });

                                    builder.create().show();
                                }
                            }
                        };
                        runOnUiThread(runnable);

                    }
                } catch (Exception e) {
                    Log.d(TAG, "Login failed...");
                    e.printStackTrace();
                    unlockLogin();
                }
            }
        };
        loginThread.start();
    }

    public boolean loggedIn() {
        if (go != null) {
            try {
                tryTalkingToServer();
                return true;
            } catch (RemoteServerException e) {
                // Looks like we're logged in but the server is cranky
                e.printStackTrace();
                return true;
            } catch (LoginFailedException e) {
                // Not logged in. Try it now
                e.printStackTrace();
                login();
                return false;
            }
        }
        else {
            login();
            return false;
        }
    }

    public String encode(String value) {
        int length = value.length();
        String result = "";
        for (int n = length - 1; n >= 0; n--) {
            result += String.format("%010d", (int) value.charAt(n));
        }

        Log.d(TAG, "Encoded \"" + value + "\" as \"" + result + "\"");
        return result;
    }

    public String decode(String value) {
        try {
            String result = "";
            int digits = 10;

            for (int n = value.length() / digits - 1; n >= 0; n--) {
                result += Character.valueOf((char) Integer.parseInt(value.substring(n * digits, (n + 1) * digits))).toString();
            }

            Log.d(TAG, "Decoded \"" + value + "\" as \"" + result + "\"");
            return result;
        } catch(Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public ProgressDialog showProgressDialog(int titleid, int messageid) {
        return showProgressDialog(getResources().getString(titleid), getResources().getString(messageid));
    }

    public ProgressDialog showProgressDialog(final String title, final String message) {
        final Context con = this;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(con);
                progressDialog.setTitle(title);
                progressDialog.setMessage(message);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
            }
        };
        runOnUiThread(runnable);
        return progressDialog;
    }

    public void shortMessage(int resid) {
        shortMessage(getResources().getString(resid));
    }

    public void shortMessage(final String message) {
        final Activity act = this;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(act, message, Toast.LENGTH_SHORT).show();
            }
        };
        runOnUiThread(r);
    }

    public void longMessage(int resid) {
        longMessage(getResources().getString(resid));
    }

    public void longMessage(final String message) {
        final Activity act = this;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(act, message, Toast.LENGTH_LONG).show();
            }
        };
        runOnUiThread(r);
    }

    public void test() {
        final Activity act = this;
        Thread thread = new Thread() {

            @Override
            public void run() {
                login();
                wideScan();
            }
        };

        thread.start();
    }

    public void tuner() {
        Intent intent = new Intent(this, TunerActivity.class);
        startActivity(intent);
    }

    public void search() {
        try {
            try {
                Intent intent =
                        new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                .setBoundsBias(new LatLngBounds(new LatLng(currentLat + 0.1f, currentLon + 0.1f), new LatLng(currentLat + 0.1f, currentLon + 0.1f)))
                                .build(this);
                startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
            } catch (Exception e) {
                Intent intent =
                        new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                .build(this);
                startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
            }
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
            longMessage("Search error. Make sure you are connected to the Internet");
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
            longMessage("Google Search not available at the moment. Please try again later.");
        }
    }

    public void logout() {
        final Context con = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.logoutTitle);
        builder.setMessage(R.string.logoutMessage);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Erase login creds so we can try again
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(con);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PREF_TOKEN, "");
                editor.putString(PREF_USERNAME, "");
                editor.putString(PREF_PASSWORD, "");
                editor.apply();

                login();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        builder.create().show();
    }

    public void about() {
        final Context con = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.aboutTitle);
        builder.setMessage(R.string.aboutMessage);

        builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        builder.create().show();
    }

    public void help() {

    }

    /*public void help() {
        *//*Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.STATUS_BAR, true);
        startActivity(intent);*//*

        ArrayList<ImageView> buttons = new ArrayList<ImageView>();
        buttons.add(setupButton(menu.getItem(0), R.drawable.ic_action_scan));
        buttons.add(setupButton(menu.getItem(1), R.drawable.ic_action_tuner));
        buttons.add(setupButton(menu.getItem(2), R.drawable.ic_action_search));

        ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();
        menuItems.add(menu.getItem(0));
        menuItems.add(menu.getItem(1));
        menuItems.add(menu.getItem(2));

        ArrayList<String> titles = new ArrayList<String>();
        titles.add(getResources().getString(R.string.scanTitle));
        titles.add(getResources().getString(R.string.scanDetailsTitle));
        titles.add(getResources().getString(R.string.searchTitle));

        ArrayList<String> messages = new ArrayList<String>();
        messages.add(getResources().getString(R.string.scanMessage));
        messages.add(getResources().getString(R.string.scanDetailsMessage));
        messages.add(getResources().getString(R.string.searchMessage));

        setUpButtonTutorial(menuItems, buttons, titles, messages);
    }

    public ImageView setupButton(MenuItem menuItem, int drawableID) {
        menuItem.setActionView(new ImageView(this, null, android.R.attr.actionButtonStyle));
        ImageView button = (ImageView) menuItem.getActionView();
        button.setImageResource(drawableID);

        // just adding some padding to look better
        float density = this.getResources().getDisplayMetrics().density;
        int padding = (int) (5 * density);
        //button.setPadding(padding, padding, padding, padding);

        return button;
    }

    public void setUpButtonTutorial(final ArrayList<MenuItem> icons, final ArrayList<ImageView> buttons, final ArrayList<String> titles, final ArrayList<String> messages) {
        for (int n = 0; n < icons.size(); n++) {
            MenuItem menuItem = icons.get(n);

            if (n == 0) {
                ToolTip toolTip = new ToolTip()
                        .setTitle(titles.get(n))
                        .setDescription(messages.get(n))
                        .setGravity(Gravity.LEFT | Gravity.BOTTOM);

                mTutorialHandler = TourGuide.init(this).with(TourGuide.Technique.Click)
                        .motionType(TourGuide.MotionType.ClickOnly)
                        .setPointer(new Pointer())
                        .setToolTip(toolTip)
                        .setOverlay(new Overlay())
                        .playOn(buttons.get(n));
            }

            final int index = n;

            buttons.get(n).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mTutorialHandler.cleanUp();
                    icons.get(index).setActionView(null);
                    if (index < icons.size() - 1) mTutorialHandler.setToolTip(new ToolTip().setTitle(titles.get(index + 1)).setDescription(messages.get(index + 1)).setGravity(Gravity.BOTTOM|Gravity.LEFT)).playOn(buttons.get(index + 1));
                }
            });
        }
    }*/

    public void twitter() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("twitter://user?screen_name=LogickLLC"));
            startActivity(intent);

        }catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://twitter.com/#!/LogickLLC")));
        }
    }

    public void facebook() {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.facebook.com/Logick-LLC-984234335029611/")));
    }

    public void contactUs() {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("mailto:logickllc@gmail.com")));
    }

    public void moreApps() {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/developer?id=Logick+LLC")));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.d(TAG, "Place: " + place.getName());
                LatLng coords = place.getLatLng();
                locationOverride = true;
                moveMe(coords.latitude, coords.longitude, true);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.e(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }


    public void wideScan() {
        if (!loggedIn()) return;
        searched = true;
        abortScan = false;
        final Context con = this;
        final ArrayList<Marker> tempMarkers = new ArrayList<Marker>(pokeMarkers);

        final Thread scanThread = new Thread() {
            public void run() {
                Runnable progressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        for (Marker marker : tempMarkers) {
                            Log.d(TAG, "Removed poke marker!");
                            marker.remove();
                        }
                        dialog = new ProgressDialog(con);
                        dialog.setTitle("Scanning for Pokemon (" + NUM_SCAN_SECTORS + " sectors at " + scanDistance + "m radius)");
                        scanDialogMessage = "Scanning sector 1 of " + NUM_SCAN_SECTORS;
                        //dialog.setMessage(scanDialogMessage);
                        dialog.setMax(NUM_SCAN_SECTORS);
                        dialog.setProgress(0);
                        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        //dialog.setIndeterminate(true);
                        dialog.setCancelable(true);
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                abortScan = true;
                            }
                        });
                        dialog.show();
                    }
                };
                runOnUiThread(progressRunnable);

                // TODO Pull this from GPS later (or Location Services)
                double lat = currentLat;
                double lon = currentLon;
                int offsetMeters = scanDistance;
                final long METERS_PER_SECOND = 50;
                final long SCAN_INTERVAL = Math.round(scanTime * 1000 / (float) (NUM_SCAN_SECTORS - 1));
                failedScanLogins = 0;

                Runnable circleRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (scanCircle != null) scanCircle.remove();
                        scanCircle = mMap.addCircle(new CircleOptions().center(new LatLng(currentLat, currentLon)).strokeWidth(1).radius(scanDistance + MAX_SCAN_RADIUS).strokeColor(Color.argb(128, 0, 0, 255)));
                    }
                };
                runOnUiThread(circleRunnable);

                Log.d(TAG, "Scan distance: " + scanDistance);
                Log.d(TAG, "Scan interval: " + SCAN_INTERVAL);

                totalNearbyPokemon.clear();
                totalEncounters.clear();
                totalWildEncounters.clear();
                pokeMarkers.clear();

                //scanForPokemon(lat, lon);

                // Calculate bounding box of this point at certain intervals and poll them
                // all for a complete mapping. Pause a few seconds between polling to not agitate the servers

                int negOffsetMeters = -1 * offsetMeters;
                LatLng[] boundingBox = getBoundingBox(lat, lon, offsetMeters);
                ArrayList<LatLng> boxList = new ArrayList<LatLng>(Arrays.asList(boundingBox));
                Vector2D[] boxPoints = new Vector2D[]{Vector2D.ZERO,
                        new Vector2D(negOffsetMeters, negOffsetMeters),
                        new Vector2D(negOffsetMeters, 0),
                        new Vector2D(negOffsetMeters, offsetMeters),
                        new Vector2D(0, offsetMeters),
                        new Vector2D(offsetMeters, offsetMeters),
                        new Vector2D(offsetMeters, 0),
                        new Vector2D(offsetMeters, negOffsetMeters),
                        new Vector2D(0, negOffsetMeters)};

                int failedSectors = 0;

                boolean first = true;
                for (int n = 0; n < boundingBox.length; n++) {
                    if (abortScan) {
                        longMessage(R.string.abortScan);
                        return;
                    }
                    try {
                        if (!first) Thread.sleep(Math.max(SCAN_INTERVAL, 500));
                        else first = false;

                        final int sector = n + 1;
                        progressRunnable = new Runnable() {
                            @Override
                            public void run() {
                                scanDialogMessage = "Scanning sector " + sector + " of " + NUM_SCAN_SECTORS;
                                //dialog.setMessage(scanDialogMessage);
                                dialog.setProgress(sector);
                            }
                        };
                        runOnUiThread(progressRunnable);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    LatLng loc = boundingBox[n];
                    if (!scanForPokemon(loc.latitude, loc.longitude)) failedSectors++;
                }

                try {
                    // Trilaterate everything we don't have on the map yet
                    for (long encounter : totalEncounters) {
                        if (totalWildEncounters.contains(encounter)) continue;
                        String name = "Unknown";
                        ArrayList<NearbyPokemonGPS> triPoints = new ArrayList<NearbyPokemonGPS>();
                        float minDistance = Float.POSITIVE_INFINITY;
                        for (NearbyPokemonGPS poke : totalNearbyPokemon) {
                            if (poke.getPokemon().getEncounterId() == encounter) {
                                minDistance = Math.min(minDistance, poke.getPokemon().getDistanceInMeters());
                                name = poke.getPokemon().getPokemonId().name();
                                if (poke.getPokemon().getDistanceInMeters() == 200) continue;
                                int index = boxList.indexOf(poke.getCoords());
                                poke.setCartesianCoords(boxPoints[index]);
                                triPoints.add(poke);
                            }
                        }
                        if (triPoints.size() >= 3) {
                            // TODO We can trilaterate with these points and distances
                            // Center location is (0,0)
                            int size = triPoints.size();
                            double[][] positions = new double[size][2];
                            double[] distances = new double[size];

                            for (int n = 0; n < size; n++) {
                                positions[n][0] = triPoints.get(n).getCartesianCoords().getX();
                                positions[n][1] = triPoints.get(n).getCartesianCoords().getY();
                                distances[n] = triPoints.get(n).getPokemon().getDistanceInMeters();
                            }

                            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                            LeastSquaresOptimizer.Optimum optimum = solver.solve();

                            double[] centroid = optimum.getPoint().toArray();
                            double offsetX = centroid[1];
                            double offsetY = centroid[0];

                            // TODO Convert to Lat/Lon somehow
                            final double latRadian = Math.toRadians(lat);

                            final double metersPerLatDegree = 110574.235;
                            final double metersPerLonDegree = 110572.833 * Math.cos(latRadian);
                            final LatLng target = new LatLng(offsetY / metersPerLatDegree + lat, offsetX / metersPerLonDegree + lon);
                            final String finalName = name;

                            Runnable markerRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "Adding marker for " + finalName + " at " + target.toString());
                                    showPokemonAt(finalName, target);
                                }
                            };
                            runOnUiThread(markerRunnable);
                        } else {
                            final String finalName = name;
                            final float finalMinDistance = minDistance;
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(con, finalName + " is " + finalMinDistance + "m away but can't be pinpointed", Toast.LENGTH_SHORT).show();
                                }
                            };
                            runOnUiThread(r);
                        }
                    }

                    Runnable dismissRunnable = new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                        }
                    };
                    runOnUiThread(dismissRunnable);

                    if (failedSectors > 0) {
                        if (failedScanLogins == NUM_SCAN_SECTORS) login();
                        else
                            shortMessage(failedSectors + " out of " + boundingBox.length + " sectors failed to scan");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    longMessage("Trilateration error. Please inform the developer.");
                }
            }
        };

        scanThread.start();
    }

    private LatLng[] getBoundingBox(final double lat, final double lon, final int distanceInMeters) {

        LatLng[] points = new LatLng[NUM_SCAN_SECTORS];

        final double latRadian = Math.toRadians(lat);

        final double metersPerLatDegree = 110574.235;
        final double metersPerLonDegree = 110572.833 * Math.cos(latRadian);
        final double deltaLat = distanceInMeters / metersPerLatDegree;
        final double deltaLong = distanceInMeters / metersPerLonDegree;

        final double minLat = lat - deltaLat;
        final double minLong = lon - deltaLong;
        final double maxLat = lat + deltaLat;
        final double maxLong = lon + deltaLong;

        points[0] = new LatLng(lat, lon);
        points[1] = new LatLng(minLat, minLong);
        points[2] = new LatLng(lat, minLong);
        points[3] = new LatLng(maxLat, minLong);
        points[4] = new LatLng(maxLat, lon);
        points[5] = new LatLng(maxLat, maxLong);
        points[6] = new LatLng(lat, maxLong);
        points[7] = new LatLng(minLat, maxLong);
        points[8] = new LatLng(minLat, lon);

        return points;
    }

    public boolean scanForPokemon(double lat, double lon) {
        try {
            Log.d(TAG, "Scanning (" + lat + "," + lon + ")...");
            go.setLocation(lat, lon, 0);
            final List<CatchablePokemon> pokes = getCatchablePokemon(go, 9);
            final List<NearbyPokemonOuterClass.NearbyPokemon> nearbyPokes = getNearbyPokemon(go, 9);
            for (NearbyPokemonOuterClass.NearbyPokemon poke : nearbyPokes) {
                totalNearbyPokemon.add(new NearbyPokemonGPS(poke, new LatLng(lat, lon)));
                totalEncounters.add(poke.getEncounterId());
            }
            final List<WildPokemonOuterClass.WildPokemon> wildPokes = getWildPokemon(go, 9);
            for (WildPokemonOuterClass.WildPokemon poke : wildPokes) {
                totalWildEncounters.add(poke.getEncounterId());
            }

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (pokes.isEmpty()) Log.d("PokeFinder", "No catchable pokes :(");
                    for (CatchablePokemon poke : pokes) {
                        Log.d("PokeFinder", "Found CatchablePokemon: " + poke.toString());
                        showPokemonAt(poke.getPokemonId().name(), new LatLng(poke.getLatitude(), poke.getLongitude()));
                    }

                    if (nearbyPokes.isEmpty()) Log.d("PokeFinder", "No nearby pokes :(");
                    for (NearbyPokemonOuterClass.NearbyPokemon poke : nearbyPokes) {
                        Log.d("PokeFinder", "Found NearbyPokemon: " + poke.toString());
                        //mMap.addCircle(new CircleOptions().center(new LatLng(go.getLatitude(), go.getLongitude())).radius(poke.getDistanceInMeters()));
                    }

                    if (wildPokes.isEmpty()) Log.d("PokeFinder", "No wild pokes :(");
                    for (WildPokemonOuterClass.WildPokemon poke : wildPokes) {
                        Log.d("PokeFinder", "Found WildPokemon: " + poke.toString());
                        showPokemonAt(poke.getPokemonData().getPokemonId().name(), new LatLng(poke.getLatitude(), poke.getLongitude()));
                    }
                }
            };
            runOnUiThread(r);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof LoginFailedException) failedScanLogins++;
            return false;
        }

    }

    public synchronized void showPokemonAt(String name, LatLng loc) {
        name = name.replaceAll("\\-", "");
        name = name.replaceAll("\\'", "");
        name = name.replaceAll("\\.", "");
        name = name.replaceAll(" ", "_");
        if (name.equals("CHARMENDER")) name = "CHARMANDER";
        if (name.equals("ALAKHAZAM")) name = "ALAKAZAM";
        try {
            int resourceID = getResources().getIdentifier(name.toLowerCase(), "drawable", getPackageName());
            name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(resourceID);
            pokeMarkers.add(mMap.addMarker(new MarkerOptions().position(loc).title(name).icon(icon)));
        } catch (Exception e) {
            longMessage("Cannot find image for \"" + name + "\". Please alert the developer.");
        }
    }

    public List<CatchablePokemon> getCatchablePokemon(PokemonGo go, int cells) throws LoginFailedException, RemoteServerException {
        List<CatchablePokemon> catchablePokemons = new ArrayList<CatchablePokemon>();
        MapObjects objects = go.getMap().getMapObjects(cells);

        for (MapPokemonOuterClass.MapPokemon mapPokemon : objects.getCatchablePokemons()) {
            catchablePokemons.add(new CatchablePokemon(go, mapPokemon));
        }

        return catchablePokemons;
    }

    public List<NearbyPokemonOuterClass.NearbyPokemon> getNearbyPokemon(PokemonGo go, int cells) throws LoginFailedException, RemoteServerException {
        List<NearbyPokemonOuterClass.NearbyPokemon> nearbyPokemons = new ArrayList<NearbyPokemonOuterClass.NearbyPokemon>();
        MapObjects objects = go.getMap().getMapObjects(cells);

        for (NearbyPokemonOuterClass.NearbyPokemon mapPokemon : objects.getNearbyPokemons()) {
            nearbyPokemons.add(mapPokemon);
        }

        return nearbyPokemons;
    }

    public List<WildPokemonOuterClass.WildPokemon> getWildPokemon(PokemonGo go, int cells) throws LoginFailedException, RemoteServerException {
        List<WildPokemonOuterClass.WildPokemon> wildPokemons = new ArrayList<WildPokemonOuterClass.WildPokemon>();
        MapObjects objects = go.getMap().getMapObjects(cells);

        for (WildPokemonOuterClass.WildPokemon mapPokemon : objects.getWildPokemons()) {
            wildPokemons.add(mapPokemon);
        }

        return wildPokemons;
    }

    public synchronized void tryTalkingToServer() throws LoginFailedException, RemoteServerException {
        serverAlive = null;

        Thread thread = new Thread() {
            public void run() {
                try {
                    go.getMap().getCatchablePokemon();
                    serverAlive = true;
                } catch (LoginFailedException e) {
                    serverAlive = false;
                } catch (RemoteServerException e) {
                    serverAlive = true;
                }
            }
        };

        thread.start();

        while (serverAlive == null) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!serverAlive) throw new LoginFailedException("You are not logged in!");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        longMessage("Failed to connect to Google Maps");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected to Google Maps");
        if (locationInitialized) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(PREF_FIRST_LOAD, true)) firstLoad();
        else initLocation();
    }

    public void firstLoad() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_FIRST_LOAD, false);
        editor.apply();

        final Context con = this;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(con);
                builder.setTitle(R.string.welcomeTitle);
                builder.setMessage(R.string.welcomeMessage);
                builder.setPositiveButton(R.string.getStarted, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        initLocation();
                    }
                });
                builder.create().show();
            }
        };
        runOnUiThread(r);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // We can now move forward with our location finding
                    initLocation();
                } else {
                    deniedLocationPermission();
                }
                break;
        }
    }

    public void initLocation() {
        try {
            Location loc = LocationServices.FusedLocationApi.getLastLocation(client);
            if (loc != null) moveMe(loc.getLatitude(), loc.getLongitude(), true);
            request = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(LOCATION_UPDATE_INTERVAL)
                    .setFastestInterval(LOCATION_UPDATE_INTERVAL);
            LocationServices.FusedLocationApi.requestLocationUpdates(client, request, this);
            locationInitialized = true;
            if (!searched) wideScan();
            Log.d(TAG, "Location initialized");
        } catch (SecurityException e) {
            e.printStackTrace();
            deniedLocationPermission();
        }
    }

    public void deniedLocationPermission() {
        Toast.makeText(this, "Location permissions denied. You will have to search for your area", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (locationOverride == false) moveMe(location.getLatitude(), location.getLongitude(), false);
        if (!searched) wideScan();
    }
}
