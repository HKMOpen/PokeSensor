package com.logickllc.pokemapper.api;


import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass;

public class NearbyPokemonGPS {
    private NearbyPokemonOuterClass.NearbyPokemon pokemon;
    private LatLng coords;
    private Vector2D cartesianCoords;

    public NearbyPokemonGPS(NearbyPokemonOuterClass.NearbyPokemon pokemon, LatLng coords) {
        this.pokemon = pokemon;
        this.coords = coords;
    }

    public NearbyPokemonOuterClass.NearbyPokemon getPokemon() {
        return pokemon;
    }

    public LatLng getCoords() {
        return coords;
    }

    public Vector2D getCartesianCoords() {
        return cartesianCoords;
    }

    public void setCartesianCoords(Vector2D cartesianCoords) {
        this.cartesianCoords = cartesianCoords;
    }
}
