package whaleghost.sanitydimr.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import whaleghost.sanitydimr.ActiveSanitySource;
import whaleghost.sanitydimr.util.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Sanity implements ISanity, IPassiveSanity, IPersistentSanity {

    // Assigned by SanityMod during construction — deferred to avoid class-loading races
    public static Supplier<AttachmentType<Sanity>> ATTACHMENT;

    private boolean m_dirty = true;
    private int m_emAngerTimer;
    private float m_sanityVal;
    private float m_passive;
    private Vec3 m_stuckMultiplier;

    private final int[] m_cds = new int[ActiveSanitySource.values().length];
    private final Map<Integer, Integer> m_itemCds = new HashMap<>();
    private final Map<Integer, Integer> m_brokenBlocksCds = new HashMap<>();

    // ── NBT serialization ────────────────────────────────────────

    @Override
    public void serializeNBT(CompoundTag tag) {
        tag.putFloat("sanity.sanity", m_sanityVal);
        tag.putInt("sanity.ender_man_anger_timer", m_emAngerTimer);
        for (ActiveSanitySource src : ActiveSanitySource.values()) {
            int i = src.ordinal();
            if (m_cds[i] != 0)
                tag.putInt(src.getNbtKey(), m_cds[i]);
        }
        SanityCooldownSerializer.serialize(tag, "sanity.item_cooldowns", m_itemCds);
        SanityCooldownSerializer.serialize(tag, "sanity.broken_blocks_cooldowns", m_brokenBlocksCds);
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        setSanity(tag.getFloat("sanity.sanity"));
        setEnderManAngerTimer(tag.getInt("sanity.ender_man_anger_timer"));
        for (ActiveSanitySource src : ActiveSanitySource.values())
            m_cds[src.ordinal()] = tag.getInt(src.getNbtKey());
        SanityCooldownSerializer.deserialize(tag, "sanity.item_cooldowns", m_itemCds);
        SanityCooldownSerializer.deserialize(tag, "sanity.broken_blocks_cooldowns", m_brokenBlocksCds);
    }

    // ── Network serialization ────────────────────────────────────

    public void serialize(FriendlyByteBuf buf) {
        buf.writeFloat(m_sanityVal);
        buf.writeFloat(m_passive);
    }

    public void deserialize(FriendlyByteBuf buf) {
        m_sanityVal = buf.readFloat();
        m_passive = buf.readFloat();
    }

    // ── ISanity ───────────────────────────────────────────────────

    @Override
    public float getSanity() { return m_sanityVal; }

    @Override
    public void setSanity(float value) {
        m_sanityVal = MathHelper.clampNorm(value);
        m_dirty = true;
    }

    // ── IPassiveSanity ────────────────────────────────────────────

    @Override
    public float getPassiveIncrease() { return m_passive; }

    @Override
    public void setPassiveIncrease(float value) {
        m_passive = value;
        m_dirty = true;
    }

    // ── IPersistentSanity ─────────────────────────────────────────

    @Override
    public int[] getActiveSourcesCooldowns() { return m_cds; }

    @Override
    public Map<Integer, Integer> getItemCooldowns() { return m_itemCds; }

    @Override
    public Map<Integer, Integer> getBrokenBlocksCooldowns() { return m_brokenBlocksCds; }

    @Override
    public void setEnderManAngerTimer(int value) { m_emAngerTimer = value; }

    @Override
    public int getEnderManAngerTimer() { return m_emAngerTimer; }

    @Override
    public void setStuckMotionMultiplier(Vec3 multiplier) { m_stuckMultiplier = multiplier; }

    @Override
    public Vec3 getStuckMotionMultiplier() { return m_stuckMultiplier; }

    // ── Dirty flag ───────────────────────────────────────────────

    public boolean getDirty() { return m_dirty; }

    public void setDirty(boolean value) { m_dirty = value; }
}