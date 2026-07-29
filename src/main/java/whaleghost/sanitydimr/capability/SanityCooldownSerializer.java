package whaleghost.sanitydimr.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for serializing {@code Map<Integer, Integer>} cooldowns as long arrays in NBT.
 * Each entry is packed as: upper 32 bits = key, lower 32 bits = value.
 */
public final class SanityCooldownSerializer {

    private SanityCooldownSerializer() {}

    public static void serialize(CompoundTag tag, String key, Map<Integer, Integer> cooldowns) {
        if (cooldowns.isEmpty())
            return;
        long[] packed = new long[cooldowns.size()];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : cooldowns.entrySet()) {
            packed[i++] = ((long) entry.getKey() << Integer.SIZE) | (entry.getValue() & 0xFFFFFFFFL);
        }
        tag.putLongArray(key, packed);
    }

    public static void deserialize(CompoundTag tag, String key, Map<Integer, Integer> target) {
        long[] packed = tag.getLongArray(key);
        target.clear();
        for (long entry : packed) {
            target.put((int) (entry >> Integer.SIZE), (int) entry);
        }
    }
}
