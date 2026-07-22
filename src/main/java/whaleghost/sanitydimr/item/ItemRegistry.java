package whaleghost.sanitydimr.item;

import whaleghost.sanitydimr.SanityMod;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ItemRegistry
{
    public static final DeferredRegister<Item> DEFERRED_REGISTER = DeferredRegister.create(Registries.ITEM, SanityMod.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SanityMod.MODID);

    public static final DeferredHolder<Item, Item> GARLAND = DEFERRED_REGISTER.register("garland", GarlandItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SANITY_TAB = CREATIVE_TABS.register("sanity_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sanitydimr"))
                    .icon(() -> new ItemStack(GARLAND.get()))
                    .displayItems((params, output) -> {
                        output.accept(GARLAND.get());
                    })
                    .build());

    public static void register(IEventBus eventBus)
    {
        DEFERRED_REGISTER.register(eventBus);
        CREATIVE_TABS.register(eventBus);
    }
}