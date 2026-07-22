package whaleghost.sanitydimr;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public interface ICompoundTagSerializable extends INBTSerializable<CompoundTag>
{
    @Override
    default CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();
        serializeNBT(tag);
        return tag;
    }

    @Override
    default void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag)
    {
        deserializeNBT(tag);
    }

    void serializeNBT(CompoundTag tag);

    void deserializeNBT(CompoundTag tag);
}