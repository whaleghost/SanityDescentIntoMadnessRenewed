package whaleghost.sanitydimr.capability;

import whaleghost.sanitydimr.ICompoundTagSerializable;
import net.minecraft.core.BlockPos;

import java.util.List;

public interface ISanityLevelChunk extends ICompoundTagSerializable
{
    List<BlockPos> getArtificiallyPlacedBlocks();
}