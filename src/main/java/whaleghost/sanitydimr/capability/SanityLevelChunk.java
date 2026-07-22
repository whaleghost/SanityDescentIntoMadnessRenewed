package whaleghost.sanitydimr.capability;

import whaleghost.sanitydimr.SanityMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SanityLevelChunk implements ISanityLevelChunk
{
    public static final Supplier<AttachmentType<SanityLevelChunk>> ATTACHMENT = SanityMod.ATTACHMENT_TYPES.register(
            "sanity_level_chunk", () -> AttachmentType.serializable(SanityLevelChunk::new).build()
    );
    private final List<BlockPos> m_blocksPlacedByPlayer = new ArrayList<>();

    @Override
    public List<BlockPos> getArtificiallyPlacedBlocks()
    {
        return m_blocksPlacedByPlayer;
    }

    @Override
    public void serializeNBT(CompoundTag tag)
    {
        int size = m_blocksPlacedByPlayer.size();
        long[] arr = new long[size];
        for (int i = 0; i < size; ++i)
        {
            arr[i] = m_blocksPlacedByPlayer.get(i).asLong();
        }
        tag.putLongArray("sanity.blocks_placed_by_player", arr);
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        long[] arr = tag.getLongArray("sanity.blocks_placed_by_player");
        m_blocksPlacedByPlayer.clear();
        for (int i = 0; i < arr.length / 3; ++i)
        {
            m_blocksPlacedByPlayer.add(BlockPos.of(arr[i]));
        }
    }
}