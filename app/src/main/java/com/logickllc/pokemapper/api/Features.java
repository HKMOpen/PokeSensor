package com.logickllc.pokemapper.api;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass;
import POGOProtos.Map.Pokemon.WildPokemonOuterClass;

public abstract class Features {
    private MapHelper mapHelper;
    public PokemonGo go;
    protected final String TAG = "PokeFinder";
    private Boolean serverAlive = null;
    protected boolean loggingIn = false;
    protected String token;


    public abstract void runOnMainThread(Runnable r);
    public abstract Object showProgressDialog(int titleid, int messageid);
    public abstract Object showProgressDialog(final String title, final String message);
    public abstract void shortMessage(int resid);
    public abstract void shortMessage(final String message);
    public abstract void longMessage(int resid);
    public abstract void longMessage(final String message);
    public abstract void login();
    public abstract void logout();

    public MapHelper getMapHelper() {
        return mapHelper;
    }

    public void setMapHelper(MapHelper mapHelper) {
        this.mapHelper = mapHelper;
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
        } else {
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

        //Log.d(TAG, "Encoded \"" + value + "\" as \"" + result + "\"");
        return result;
    }

    public String decode(String value) {
        try {
            String result = "";
            int digits = 10;

            for (int n = value.length() / digits - 1; n >= 0; n--) {
                result += Character.valueOf((char) Integer.parseInt(value.substring(n * digits, (n + 1) * digits))).toString();
            }

            //Log.d(TAG, "Decoded \"" + value + "\" as \"" + result + "\"");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
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
}
