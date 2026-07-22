package whaleghost.sanitydimr.capability;

import whaleghost.sanitydimr.ActiveSanitySources;
import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.util.MathHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Sanity implements ISanity, IPassiveSanity, IPersistentSanity
{
    public static final Supplier<AttachmentType<Sanity>> ATTACHMENT = SanityMod.ATTACHMENT_TYPES.register(
            "sanity", () -> AttachmentType.serializable(Sanity::new).build()
    );
    private boolean m_dirty = true;
    private int m_emAngerTimer;
    private float m_sanityVal;
    private float m_passive;
    private Vec3 m_stuckMultiplier;

    private final int[] m_cds = new int[ActiveSanitySources.AMOUNT];
    private final Map<Integer, Integer> m_itemCds = new HashMap<>();
    private final Map<Integer, Integer> m_brokenBlocksCds = new HashMap<>();

    public Sanity()
    {
    }

    private static final String[] CD_KEYS = {
            "sanity.sleeping",
            "sanity.baby_chicken_spawn",
            "sanity.animal_breeding",
            "sanity.villager_trade",
            "sanity.shearing",
            "sanity.eating",
            "sanity.fishing",
            "sanity.potting_flower"
    };

    @Override
    public void serializeNBT(CompoundTag tag)
    {
        tag.putFloat("sanity.sanity", m_sanityVal);
        tag.putInt("sanity.ender_man_anger_timer", m_emAngerTimer);

        for (int i = 0; i < m_cds.length; i++)
        {
            if (m_cds[i] != 0)
                tag.putInt(CD_KEYS[i], m_cds[i]);
        }

        serializeItemCds(tag);
        serializeBrokenBlocksCds(tag);
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        setSanity(tag.getFloat("sanity.sanity"));
        setEnderManAngerTimer(tag.getInt("sanity.ender_man_anger_timer"));

        for (int i = 0; i < m_cds.length; i++)
            m_cds[i] = tag.getInt(CD_KEYS[i]);

        deserializeItemCds(tag);
        deserializeBrokenBlocksCooldowns(tag);
    }

    public void serialize(FriendlyByteBuf buf)
    {
        buf.writeFloat(m_sanityVal);
        buf.writeFloat(m_passive);
    }

    public void deserialize(FriendlyByteBuf buf)
    {
        m_sanityVal = buf.readFloat();
        m_passive = buf.readFloat();
    }

    @Override
    public float getSanity()
    {
        return m_sanityVal;
    }

    @Override
    public void setSanity(float value)
    {
        m_sanityVal = MathHelper.clampNorm(value);
        m_dirty = true;
    }

    @Override
    public float getPassiveIncrease()
    {
        return m_passive;
    }

    @Override
    public void setPassiveIncrease(float value)
    {
        m_passive = value;
        m_dirty = true;
    }

    public boolean getDirty()
    {
        return m_dirty;
    }

    public void setDirty(boolean value)
    {
        m_dirty = value;
    }

    @Override
    public int[] getActiveSourcesCooldowns()
    {
        return m_cds;
    }

    @Override
    public Map<Integer, Integer> getItemCooldowns()
    {
        return m_itemCds;
    }

    @Override
    public Map<Integer, Integer> getBrokenBlocksCooldowns()
    {
        return m_brokenBlocksCds;
    }

    @Override
    public void setEnderManAngerTimer(int value)
    {
        m_emAngerTimer = value;
    }

    @Override
    public int getEnderManAngerTimer()
    {
        return m_emAngerTimer;
    }

    @Override
    public void setStuckMotionMultiplier(Vec3 multiplier)
    {
        m_stuckMultiplier = multiplier;
    }

    @Override
    public Vec3 getStuckMotionMultiplier()
    {
        return m_stuckMultiplier;
    }

    private void serializeItemCds(CompoundTag tag)
    {
        long[] itemCds = new long[m_itemCds.size()];
        int i = 0;

        for (Map.Entry<Integer, Integer> entry : m_itemCds.entrySet())
        {
            long val = entry.getKey();
            val <<= Long.SIZE / 2;
            val |= entry.getValue();

            itemCds[i] = val;

            i++;
        }

        tag.putLongArray("sanity.item_cooldowns", itemCds);
    }

    private void deserializeItemCds(CompoundTag tag)
    {
        long[] itemCds = tag.getLongArray("sanity.item_cooldowns");
        m_itemCds.clear();

        for (long itemCd : itemCds)
        {
            m_itemCds.put((int)(itemCd >> Long.SIZE / 2), (int)itemCd);
        }
    }

    private void serializeBrokenBlocksCds(CompoundTag tag)
    {
        long[] brokenBlocksCds = new long[m_brokenBlocksCds.size()];
        int i = 0;

        for (Map.Entry<Integer, Integer> entry : m_brokenBlocksCds.entrySet())
        {
            long val = entry.getKey();
            val <<= Long.SIZE / 2;
            val |= entry.getValue();

            brokenBlocksCds[i] = val;

            i++;
        }

        tag.putLongArray("sanity.broken_blocks_cooldowns", brokenBlocksCds);
    }

    private void deserializeBrokenBlocksCooldowns(CompoundTag tag)
    {
        long[] brokenBlocksCds = tag.getLongArray("sanity.broken_blocks_cooldowns");
        m_brokenBlocksCds.clear();

        for (long blockCd : brokenBlocksCds)
        {
            m_itemCds.put((int)(blockCd >> Long.SIZE / 2), (int)blockCd);
        }
    }
}