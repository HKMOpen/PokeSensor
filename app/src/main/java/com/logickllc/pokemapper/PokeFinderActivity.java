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
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.logickllc.pokemapper.api.MapHelper;
import com.logickllc.pokemapper.api.NearbyPokemonGPS;
import com.logickllc.pokemapper.api.WildPokemonTime;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass;
import POGOProtos.Map.Pokemon.WildPokemonOuterClass;
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;
import okhttp3.OkHttpClient;

import android.view.ViewGroup.LayoutParams;

public class PokeFinderActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final String PREF_FIRST_LOAD = "FirstLoad";
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1776;
    private final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1337;
    private final String TAG = "PokeFinder";
    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest request;
    private Menu menu;
    //private TourGuide mTutorialHandler;
    private boolean abortLogin = false;
    private static final BigInteger BI_2_64 = BigInteger.ONE.shiftLeft(64);
    private AndroidMapHelper mapHelper = new AndroidMapHelper(this);
    private AndroidFeatures features = new AndroidFeatures(this);

    // This manages all the timers I use and only lets them count down while the activity is in the foreground
    private AndroidTimerManager timerManager = new AndroidTimerManager();
    @SuppressWarnings("unused")
    private boolean timersPaused = true;
    private boolean isActivityVisible = true;

    //These are all related to ads
    public static final boolean IS_AD_TESTING = true; // TODO Flag that determines whether to show test ads or live ads
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

        mapHelper.setFeatures(features);
        features.setMapHelper(mapHelper);

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
        if (IS_AD_TESTING) {
            AdRegistration.enableTesting(true);
            //AdRegistration.enableLogging(true);
        }


        initAds();
        showAds();

        mapHelper.setScanPointIcon(BitmapDescriptorFactory.fromResource(R.drawable.scan_point_icon));


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "PokeFinderActivity.onStart()");
        client.connect();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                features.loggedIn();
            }
        };
        runnable.run();
    }

    @Override
    protected void onResume() {
        super.onResume();


        resumeAds();
        //timerManager.resumeTimers();


        Log.d(TAG, "PokeFinderActivity.onResume()");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mapHelper.setScanDistance(preferences.getInt(AndroidMapHelper.PREF_SCAN_DISTANCE, MapHelper.DEFAULT_SCAN_DISTANCE));
        mapHelper.setScanTime(preferences.getInt(AndroidMapHelper.PREF_SCAN_TIME, MapHelper.DEFAULT_SCAN_TIME));
        if (mapHelper.getScanDistance() > MapHelper.MAX_SCAN_DISTANCE) mapHelper.setScanDistance(MapHelper.MAX_SCAN_DISTANCE);

        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            features.longMessage(R.string.noLocationDetected);
        }

        mapHelper.startCountdownTimer();
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
        // Make sure the timers are paused (the tasks are successfully cancelled)
        timersPaused = true;
        pauseAds();
        timersPaused = timerManager.pauseTimers();

        mapHelper.stopCountdownTimer();
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
            public void onAdCollapsed(Ad ad) {
            }

            @Override
            public void onAdDismissed(Ad ad) {
                startInterstitialTimer(); //start the countdown to show another interstitial
            }

            @Override
            public void onAdExpanded(Ad ad) {
            }

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
        //adNetworkPositions.put("Admob", ADMOB_DEFAULT_POSITION);
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
                        if (isActivityVisible)
                            loadNextBanner(null); // Try Amazon before anything else. They have the best CPM
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
        lastOrientation = newConfig.orientation;
    }

    public void loadNextBanner(String currentBanner) {
        // Determine which ad network comes next in the waterfall
        // Later the network positions will be dynamically set from a server
        int nextNetwork;
        if (currentBanner == null) {
            nextNetwork = 1;
        } else {
            nextNetwork = (adNetworkPositions.get(currentBanner).intValue() % adNetworkPositions.size()) + 1;
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
                    loadAmazonBanner();
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
            public void onAdCollapsed(Ad ad) {
            }

            @Override
            public void onAdDismissed(Ad ad) {
            }

            @Override
            public void onAdExpanded(Ad ad) {
            }

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
                mapHelper.wideScan();
                return true;

            case R.id.action_tuner:
                tuner();
                return true;

            case R.id.action_search:
                search();
                return true;

            case R.id.action_logout:
                if (IS_AD_TESTING) mMap.clear();
                else features.logout();
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
        mapHelper.setmMap(mMap);

        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int googleLogoPadding = Math.round(90 * metrics.density) + 2;
        mapHelper.setPaddingLeft(5);
        mapHelper.setPaddingTop(5);
        mapHelper.setPaddingRight(5);
        mapHelper.setPaddingBottom(googleLogoPadding);
        mMap.setPadding(mapHelper.getPaddingLeft(), mapHelper.getPaddingTop(), mapHelper.getPaddingRight(), mapHelper.getPaddingBottom());


        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                mapHelper.setLocationOverride(true);
                mapHelper.moveMe(latLng.latitude, latLng.longitude, true, false);
            }
        });

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                mapHelper.setLocationOverride(true);
                initLocation();
                return false;
            }
        });

        mMap.animateCamera(CameraUpdateFactory.zoomTo(AndroidMapHelper.DEFAULT_ZOOM));

        //mapHelper.moveMe(TEST_LAT, TEST_LON);
        //mapHelper.moveMe(FAIRPARK_LAT, FAIRPARK_LON);
        //test();
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
                                .setBoundsBias(new LatLngBounds(new LatLng(mapHelper.getCurrentLat() + 0.1f, mapHelper.getCurrentLon() + 0.1f), new LatLng(mapHelper.getCurrentLat() + 0.1f, mapHelper.getCurrentLon() + 0.1f)))
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
            features.longMessage("Search error. Make sure you are connected to the Internet");
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
            features.longMessage("Google Search not available at the moment. Please try again later.");
        }
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

        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://twitter.com/LogickLLC")));
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
                mapHelper.setLocationOverride(true);
                mapHelper.moveMe(coords.latitude, coords.longitude, true, true);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.e(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    // Courtesy of http://stackoverflow.com/questions/18204265/how-to-convert-unsigned-long-to-string-in-java
    public static String asString(long l) {
        return l >= 0 ? String.valueOf(l) : toBigInteger(l).toString();
    }

    // Courtesy of http://stackoverflow.com/questions/18204265/how-to-convert-unsigned-long-to-string-in-java
    public static BigInteger toBigInteger(long l) {
        final BigInteger bi = BigInteger.valueOf(l);
        return l >= 0 ? bi : bi.add(BI_2_64);
    }

    private static final BigInteger TWO_64 = BigInteger.ONE.shiftLeft(64);

    /*public String asString(long l) {
        BigInteger b = BigInteger.valueOf(l);
        if(b.signum() < 0) {
            b = b.add(TWO_64);
        }
        return b.toString();
    }*/

    public static long getUnsignedInt(long x) {
        return x & 0x00000000ffffffffL;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        features.longMessage("Failed to connect to Google Maps");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected to Google Maps");
        if (mapHelper.isLocationInitialized()) return;
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
            if (loc != null) mapHelper.moveMe(loc.getLatitude(), loc.getLongitude(), true, true);
            request = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(MapHelper.LOCATION_UPDATE_INTERVAL)
                    .setFastestInterval(MapHelper.LOCATION_UPDATE_INTERVAL);
            LocationServices.FusedLocationApi.requestLocationUpdates(client, request, this);
            mapHelper.setLocationInitialized(true);
            if (!mapHelper.isSearched()) mapHelper.wideScan();
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
        if (mapHelper.isLocationOverridden() == false)
            mapHelper.moveMe(location.getLatitude(), location.getLongitude(), false, false);
        if (!mapHelper.isSearched()) mapHelper.wideScan();
    }
}
