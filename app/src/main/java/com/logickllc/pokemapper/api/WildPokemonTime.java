package com.logickllc.pokemapper.api;

import POGOProtos.Map.Pokemon.WildPokemonOuterClass;

public class WildPokemonTime {
    private WildPokemonOuterClass.WildPokemon poke;
    private long despawnTimeMs;
    private long encounterID;

    public WildPokemonTime(WildPokemonOuterClass.WildPokemon poke, long despawnTimeMs) {
        this.poke = poke;
        this.despawnTimeMs = despawnTimeMs;
    }

    public WildPokemonTime(long encounterID, long despawnTimeMs) {
        this.encounterID = encounterID;
        this.despawnTimeMs = despawnTimeMs;
    }

    public WildPokemonOuterClass.WildPokemon getPoke() {
        return poke;
    }

    public long getDespawnTimeMs() {
        return despawnTimeMs;
    }

    public long getEncounterID() {
        if (poke != null) return poke.getEncounterId();
        else return encounterID;
    }
}
