package com.logickllc.pokemapper;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.logickllc.pokemapper.api.MapHelper;
import com.logickllc.pokemapper.api.NearbyPokemonGPS;
import com.logickllc.pokemapper.api.WildPokemonTime;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.exceptions.LoginFailedException;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass;
import POGOProtos.Map.Pokemon.WildPokemonOuterClass;

public class AndroidMapHelper extends MapHelper {
    private Activity act;

    public static final String PREF_SCAN_DISTANCE = "ScanDistance";
    public static final String PREF_SCAN_TIME = "ScanTime";
    public static final float DEFAULT_ZOOM = 16f;

    private Marker myMarker;
    private GoogleMap mMap;
    private ConcurrentHashMap<Long, Marker> pokeMarkers = new ConcurrentHashMap<Long, Marker>();
    private int paddingLeft, paddingRight, paddingTop, paddingBottom;
    private Circle scanCircle;
    private String scanDialogMessage;
    private Marker scanPoint;
    private Circle scanPointCircle;
    private BitmapDescriptor scanPointIcon;

    public AndroidMapHelper(Activity act) {
        this.act = act;
    }

    public Marker getMyMarker() {
        return myMarker;
    }

    public void setMyMarker(Marker myMarker) {
        this.myMarker = myMarker;
    }

    public ConcurrentHashMap<Long, Marker> getPokeMarkers() {
        return pokeMarkers;
    }

    public void setPokeMarkers(ConcurrentHashMap<Long, Marker> pokeMarkers) {
        this.pokeMarkers = pokeMarkers;
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    public Circle getScanCircle() {
        return scanCircle;
    }

    public void setScanCircle(Circle scanCircle) {
        this.scanCircle = scanCircle;
    }

    public String getScanDialogMessage() {
        return scanDialogMessage;
    }

    public void setScanDialogMessage(String scanDialogMessage) {
        this.scanDialogMessage = scanDialogMessage;
    }

    public Marker getScanPoint() {
        return scanPoint;
    }

    public void setScanPoint(Marker scanPoint) {
        this.scanPoint = scanPoint;
    }

    public Circle getScanPointCircle() {
        return scanPointCircle;
    }

    public void setScanPointCircle(Circle scanPointCircle) {
        this.scanPointCircle = scanPointCircle;
    }

    public BitmapDescriptor getScanPointIcon() {
        return scanPointIcon;
    }

    public void setScanPointIcon(BitmapDescriptor scanPointIcon) {
        this.scanPointIcon = scanPointIcon;
    }

    public GoogleMap getmMap() {
        return mMap;
    }

    public void setmMap(GoogleMap mMap) {
        this.mMap = mMap;
    }



    public synchronized void moveMe(double lat, double lon, boolean repositionCamera, boolean reZoom) {
        // Add a marker in Sydney and move the camera
        LatLng me = new LatLng(lat, lon);
        if (myMarker != null) myMarker.remove();
        myMarker = mMap.addMarker(new MarkerOptions().position(me).title("Me"));
        if (repositionCamera) {
            if (reZoom) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, DEFAULT_ZOOM));
            else mMap.animateCamera(CameraUpdateFactory.newLatLng(me));
        }
        currentLat = lat;
        currentLon = lon;
    }

    public void wideScan() {
        if (!features.loggedIn()) return;
        searched = true;
        abortScan = false;
        if (scanDistance > MAX_SCAN_DISTANCE) scanDistance = MAX_SCAN_DISTANCE;
        final Context con = act;
        final LinearLayout scanLayout = (LinearLayout) act.findViewById(R.id.scanLayout);
        final ProgressBar scanBar = (ProgressBar) act.findViewById(R.id.scanBar);
        final TextView scanText = (TextView) act.findViewById(R.id.scanText);

        Runnable main = new Runnable() {
            @Override
            public void run() {
                final ArrayList<Long> ids = new ArrayList<Long>(noTimes);

                for (Long id : ids) {
                    Log.d(TAG, "Removed poke marker!");
                    Marker marker = pokeMarkers.remove(id);
                    marker.remove();
                }

                scanBar.setProgress(0);
                scanBar.setMax(NUM_SCAN_SECTORS);
                scanText.setText("Scanning for Pokemon (" + NUM_SCAN_SECTORS + " sectors at " + scanDistance + "m radius)");

                scanLayout.setVisibility(View.VISIBLE);
                scanBar.setVisibility(View.VISIBLE);
                scanLayout.requestLayout();
                scanLayout.bringToFront();
                scanLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);


                DisplayMetrics metrics = act.getResources().getDisplayMetrics();
                paddingTop = Math.round(scanLayout.getMeasuredHeight() * metrics.density) + 2;
                Log.d(TAG, "Padding top: " + paddingTop);
                mMap.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

                noTimes.clear();

                final Thread scanThread = new Thread() {
                    public void run() {
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
                                scanCircle = mMap.addCircle(new CircleOptions().center(new LatLng(currentLat, currentLon)).strokeWidth(1).radius(0.85f * (scanDistance + MAX_SCAN_RADIUS)).strokeColor(Color.argb(128, 0, 0, 255)));
                            }
                        };
                        features.runOnMainThread(circleRunnable);

                        Log.d(TAG, "Scan distance: " + scanDistance);
                        Log.d(TAG, "Scan interval: " + SCAN_INTERVAL);

                        totalNearbyPokemon.clear();
                        totalEncounters.clear();
                        totalWildEncounters.clear();
                        //pokeMarkers.clear();

                        //scanForPokemon(lat, lon);

                        // Calculate bounding box of this point at certain intervals and poll them
                        // all for a complete mapping. Pause a few seconds between polling to not agitate the servers

                        int negOffsetMeters = -1 * offsetMeters;
                        float offsetDiagonal = (float) Math.sin(Math.toRadians(45));
                        float negOffsetDiagonal = -1 * offsetDiagonal;
                        LatLng[] boundingBox = getBoundingBox(lat, lon, offsetMeters);
                        ArrayList<LatLng> boxList = new ArrayList<LatLng>(Arrays.asList(boundingBox));
                        Vector2D[] boxPoints = new Vector2D[]{Vector2D.ZERO,
                                new Vector2D(negOffsetDiagonal, negOffsetDiagonal),
                                new Vector2D(negOffsetMeters, 0),
                                new Vector2D(negOffsetDiagonal, offsetDiagonal),
                                new Vector2D(0, offsetMeters),
                                new Vector2D(offsetDiagonal, offsetDiagonal),
                                new Vector2D(offsetMeters, 0),
                                new Vector2D(offsetDiagonal, negOffsetDiagonal),
                                new Vector2D(0, negOffsetMeters)};

                        int failedSectors = 0;

                        boolean first = true;
                        for (int n = 0; n < boundingBox.length; n++) {
                            // TODO Any changes to this should be reflected in the below identical abort block
                            if (abortScan) {
                                features.longMessage(R.string.abortScan);
                                return;
                            }
                            final LatLng loc = boundingBox[n];
                            try {
                                if (!first) Thread.sleep(Math.max(SCAN_INTERVAL, 500));
                                else first = false;

                                if (abortScan) {
                                    features.longMessage(R.string.abortScan);
                                    return;
                                }

                                final int sector = n + 1;
                                Runnable progressRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        scanDialogMessage = "Scanning for Pokemon (" + NUM_SCAN_SECTORS + " sectors at " + scanDistance + "m radius)";
                                        //dialog.setMessage(scanDialogMessage);
                                        //dialog.setProgress(sector);
                                        scanText.setText(scanDialogMessage);
                                        scanBar.setProgress(sector);
                                        if (scanPoint != null) scanPoint.remove();
                                        if (PokeFinderActivity.IS_AD_TESTING) {

                                            scanPointCircle = mMap.addCircle(new CircleOptions().radius(MAX_SCAN_RADIUS).strokeWidth(2).fillColor(Color.GREEN).center(loc).zIndex(-1));
                                        } else {
                                                if (scanPointCircle != null) scanPointCircle.remove();
                                                scanPointCircle = mMap.addCircle(new CircleOptions().radius(MAX_SCAN_RADIUS).strokeWidth(2).center(loc).zIndex(-1));
                                            }

                                            scanPoint = mMap.addMarker(new MarkerOptions().position(loc).title("Sector " + sector).icon(scanPointIcon).anchor(0.32f, 0.32f).zIndex(10000f));
                                    }
                                };
                                features.runOnMainThread(progressRunnable);
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (abortScan) {
                                    features.longMessage(R.string.abortScan);
                                    return;
                                }
                            }
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
                                        if (poke.getPokemon().getDistanceInMeters() == 200)
                                            continue;
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
                                            showPokemonAt(finalName, target, System.currentTimeMillis(), false);
                                        }
                                    };
                                    features.runOnMainThread(markerRunnable);
                                } else {
                                    final String finalName = name;
                                    final float finalMinDistance = minDistance;
                                    Runnable r = new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(con, finalName + " is " + finalMinDistance + "m away but can't be pinpointed", Toast.LENGTH_SHORT).show();
                                        }
                                    };
                                    //features.runOnMainThread(r);
                                }
                            }

                            Runnable dismissRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    //dialog.dismiss();
                                    if (scanPoint != null) scanPoint.remove();
                                    if (scanPointCircle != null) scanPointCircle.remove();

                                    scanLayout.setVisibility(View.GONE);
                                    paddingTop = 5;
                                    mMap.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
                                }
                            };
                            features.runOnMainThread(dismissRunnable);

                            if (failedSectors > 0) {
                                if (failedScanLogins == NUM_SCAN_SECTORS) features.login();
                                else
                                    features.shortMessage(failedSectors + " out of " + boundingBox.length + " sectors failed to scan");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            features.longMessage("Trilateration error. Please inform the developer.");
                        }
                    }
                };

                scanLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scanThread.interrupt();
                        abortScan = true;
                        scanLayout.setVisibility(View.GONE);
                        paddingTop = 5;
                        mMap.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

                        if (scanPoint != null) scanPoint.remove();
                        if (scanPointCircle != null) scanPointCircle.remove();
                    }
                });

                scanThread.start();
            }
        };

        features.runOnMainThread(main);
    }

    public boolean scanForPokemon(double lat, double lon) {
        try {
            Log.d(TAG, "Scanning (" + lat + "," + lon + ")...");
            features.go.setLocation(lat, lon, 0);
            final List<CatchablePokemon> pokes = features.getCatchablePokemon(features.go, 9);
            final List<NearbyPokemonOuterClass.NearbyPokemon> nearbyPokes = features.getNearbyPokemon(features.go, 9);
            for (NearbyPokemonOuterClass.NearbyPokemon poke : nearbyPokes) {
                totalNearbyPokemon.add(new NearbyPokemonGPS(poke, new LatLng(lat, lon)));
                totalEncounters.add(poke.getEncounterId());
            }
            final List<WildPokemonOuterClass.WildPokemon> wildPokes = features.getWildPokemon(features.go, 9);
            for (WildPokemonOuterClass.WildPokemon poke : wildPokes) {
                totalWildEncounters.add(poke.getEncounterId());
                if (!pokeTimes.containsKey(poke.getEncounterId()) && !noTimes.contains(poke.getEncounterId())) {
                    long timeMs = poke.getTimeTillHiddenMs();
                    if (timeMs > 0) {
                        long despawnTime = System.currentTimeMillis() + timeMs;
                        pokeTimes.put(poke.getEncounterId(), new WildPokemonTime(poke, despawnTime));
                        Log.d(TAG, poke.getPokemonData().getPokemonId() + " will despawn at " + despawnTime);
                    } else if (timeMs < 0) {
                        noTimes.add(poke.getEncounterId());
                    }
                }

            }

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    /*if (pokes.isEmpty()) Log.d("PokeFinder", "No catchable pokes :(");
                    for (CatchablePokemon poke : pokes) {
                        Log.d("PokeFinder", "Found CatchablePokemon: " + poke.toString());
                        // TODO Figure out expiration timestamp
                        //showPokemonAt(poke.getPokemonId().name(), new LatLng(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), true);
                    }*/

                    if (nearbyPokes.isEmpty()) Log.d("PokeFinder", "No nearby pokes :(");
                    for (NearbyPokemonOuterClass.NearbyPokemon poke : nearbyPokes) {
                        //Log.d("PokeFinder", "Found NearbyPokemon: " + poke.toString());
                        //mMap.addCircle(new CircleOptions().center(new LatLng(features.go.getLatitude(), features.go.getLongitude())).radius(poke.getDistanceInMeters()));
                    }

                    if (wildPokes.isEmpty()) Log.d("PokeFinder", "No wild pokes :(");
                    for (WildPokemonOuterClass.WildPokemon poke : wildPokes) {
                        Log.d("PokeFinder", "Found WildPokemon: " + poke.toString());
                        //Log.d(TAG, "Most recent way of finding time till hidden: " +  (poke.getTimeTillHiddenMs() & 0xffffffffL));
                        //Log.d(TAG, "BigDecimal: " + asString(poke.getTimeTillHiddenMs()));
                        //Log.d(TAG, "Integer shift: " + Integer.toString(poke.getTimeTillHiddenMs() >> 16));
                        //Log.d(TAG, "Long shift: " + Long.toString(poke.getTimeTillHiddenMs() >> 16));
                        /*String time = asString(poke.getTimeTillHiddenMs());

                        if (time.length() < 6) {
                            time = String.format("%06d", Long.parseLong(time));
                        }

                        String ms = time.substring(time.length() - 6);
                        int sec = Integer.parseInt(ms.substring(0, 3));
                        Log.d(TAG, "Time til hidden ms: " + asString(poke.getTimeTillHiddenMs()));
                        if (poke.getTimeTillHiddenMs() < 0) Log.d(TAG, "Time approximation ms: " + (Math.abs(Integer.MIN_VALUE) - Math.abs(poke.getTimeTillHiddenMs())));*/
                        long time = poke.getTimeTillHiddenMs();
                        if (time > 0) {
                            String ms = String.format("%06d", time);
                            int sec = Integer.parseInt(ms.substring(0, 3));
                            //Log.d(TAG, "Time string: " + time);
                            //Log.d(TAG, "Time shifted: " + (Long.parseLong(time) >> 16));
                            Log.d(TAG, "Time till hidden seconds: " + sec + "s");
                            //Log.d(TAG, "Data for " + poke.getPokemonData().getPokemonId() + ":\n" + poke.getPokemonData());
                            showPokemonAt(poke.getPokemonData().getPokemonId().name(), new LatLng(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), true);
                        } else if (time < 0) {
                            Log.d(TAG, "No valid expiry time given");
                            showPokemonAt(poke.getPokemonData().getPokemonId().name(), new LatLng(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), false);
                        }
                    }
                }
            };
            features.runOnMainThread(r);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof LoginFailedException) failedScanLogins++;
            return false;
        }

    }



    public synchronized void showPokemonAt(String name, LatLng loc, long encounterid, boolean hasTime) {
        if (pokeMarkers.containsKey(encounterid)) return;

        name = name.replaceAll("\\-", "");
        name = name.replaceAll("\\'", "");
        name = name.replaceAll("\\.", "");
        name = name.replaceAll(" ", "_");
        if (name.equals("CHARMENDER")) name = "CHARMANDER";
        if (name.equals("ALAKHAZAM")) name = "ALAKAZAM";
        if (name.equals("CLEFARY")) name = "CLEFAIRY";
        if (name.equals("GEODUGE")) name = "GEODUDE";
        if (name.equals("SANDLASH")) name = "SANDSLASH";
        try {
            int resourceID = act.getResources().getIdentifier(name.toLowerCase(), "drawable", act.getPackageName());
            name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(resourceID);
            if (hasTime)
                pokeMarkers.put(encounterid, mMap.addMarker(new MarkerOptions().position(loc).title(name).icon(icon)));
            else
                pokeMarkers.put(encounterid, mMap.addMarker(new MarkerOptions().position(loc).title(name).icon(icon).snippet(act.getResources().getString(R.string.timeNotGiven))));
        } catch (Exception e) {
            features.longMessage("Cannot find image for \"" + name + "\". Please alert the developer.");
            name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            if (hasTime)
                pokeMarkers.put(encounterid, mMap.addMarker(new MarkerOptions().position(loc).title(name)));
            else
                pokeMarkers.put(encounterid, mMap.addMarker(new MarkerOptions().position(loc).title(name).snippet(act.getResources().getString(R.string.timeNotGiven))));
        }
    }



    private LatLng[] getBoundingBox(final double lat, final double lon, final int distanceInMeters) {

        LatLng[] points = new LatLng[MapHelper.NUM_SCAN_SECTORS];

        final double latRadian = Math.toRadians(lat);

        final double metersPerLatDegree = 110574.235;
        final double metersPerLonDegree = 110572.833 * Math.cos(latRadian);
        final double deltaLat = distanceInMeters / metersPerLatDegree;
        final double deltaLong = distanceInMeters / metersPerLonDegree;

        final double minLat = lat - deltaLat;
        final double minLong = lon - deltaLong;
        final double maxLat = lat + deltaLat;
        final double maxLong = lon + deltaLong;

        final double deltaLatDiagonal = Math.sin(Math.toRadians(45)) * deltaLat;
        final double deltaLongDiagonal = Math.cos(Math.toRadians(45)) * deltaLong;

        final double minDiagonalLat = lat - deltaLatDiagonal;
        final double minDiagonalLong = lon - deltaLongDiagonal;
        final double maxDiagonalLat = lat + deltaLatDiagonal;
        final double maxDiagonalLong = lon + deltaLongDiagonal;

        points[0] = new LatLng(lat, lon);
        points[1] = new LatLng(minDiagonalLat, minDiagonalLong);
        points[2] = new LatLng(lat, minLong);
        points[3] = new LatLng(maxDiagonalLat, minDiagonalLong);
        points[4] = new LatLng(maxLat, lon);
        points[5] = new LatLng(maxDiagonalLat, maxDiagonalLong);
        points[6] = new LatLng(lat, maxLong);
        points[7] = new LatLng(minDiagonalLat, maxDiagonalLong);
        points[8] = new LatLng(minLat, lon);

        return points;
    }

    public void startCountdownTimer() {
        if (countdownTimer != null) countdownTimer.cancel();
        countdownTimer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<Long> removables = new ArrayList<Long>();
                        for (WildPokemonTime poke : pokeTimes.values()) {
                            long timeLeftMs = poke.getDespawnTimeMs() - System.currentTimeMillis();
                            if (timeLeftMs < 0) {
                                pokeMarkers.remove(poke.getPoke().getEncounterId()).remove();
                                removables.add(poke.getPoke().getEncounterId());
                            } else {
                                Marker marker = pokeMarkers.get(poke.getPoke().getEncounterId());
                                marker.setSnippet("Leaves in " + getTimeString(timeLeftMs / 1000 + 1));
                                if (marker.isInfoWindowShown()) marker.showInfoWindow();
                            }
                        }
                        for (Long id : removables) {
                            pokeTimes.remove(id);
                        }
                    }
                };
                features.runOnMainThread(r);
            }
        };

        countdownTimer.schedule(task, 0, 1000);
    }

    public void stopCountdownTimer() {
        if (countdownTimer != null) countdownTimer.cancel();
    }
}
