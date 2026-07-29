package whaleghost.sanitydimr;

public enum ActiveSanitySource {
    SLEEPING("sanity.sleeping"),
    SPAWNING_BABY_CHICKEN("sanity.baby_chicken_spawn"),
    BREEDING_ANIMALS("sanity.animal_breeding"),
    VILLAGER_TRADE("sanity.villager_trade"),
    SHEARING("sanity.shearing"),
    EATING("sanity.eating"),
    FISHING("sanity.fishing"),
    POTTING_FLOWER("sanity.potting_flower");

    private final String nbtKey;

    ActiveSanitySource(String nbtKey) {
        this.nbtKey = nbtKey;
    }

    public String getNbtKey() { return nbtKey; }
}
