package croissantnova.sanitydim.sound;

import croissantnova.sanitydim.SanityMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SoundRegistry
{
    public static DeferredRegister<SoundEvent> DEFERRED_REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, SanityMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> INSANITY             = registerSoundEvent("insanity");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT            = registerSoundEvent("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWISH                = registerSoundEvent("swish");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOWERS_EQUIP        = registerSoundEvent("flowers_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> INNER_ENTITY_HURT    = registerSoundEvent("inner_entity_hurt");

    public static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name)
    {
        return DEFERRED_REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, name)));
    }

    public static void register(IEventBus eventBus)
    {
        DEFERRED_REGISTER.register(eventBus);
    }
}