package croissantnova.sanitydim.item;

import croissantnova.sanitydim.SanityMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ItemRegistry
{
    public static final DeferredRegister<Item> DEFERRED_REGISTER = DeferredRegister.create(Registries.ITEM, SanityMod.MODID);

    public static final DeferredHolder<Item, Item> GARLAND = DEFERRED_REGISTER.register("garland", GarlandItem::new);

    public static void register(IEventBus eventBus)
    {
        DEFERRED_REGISTER.register(eventBus);
    }
}