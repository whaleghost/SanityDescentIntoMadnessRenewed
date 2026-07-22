package whaleghost.sanitydimr.event;

import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.client.render.RendererRottingStalker;
import whaleghost.sanitydimr.client.render.RendererSneakingTerror;
import whaleghost.sanitydimr.config.ConfigManager;
import whaleghost.sanitydimr.entity.EntityRegistry;
import whaleghost.sanitydimr.entity.RottingStalker;
import whaleghost.sanitydimr.entity.SneakingTerror;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;

public class ModEventHandler
{
    @SubscribeEvent
    public static void addEntityAttributes(final EntityAttributeCreationEvent event)
    {
        event.put(EntityRegistry.ROTTING_STALKER.get(), RottingStalker.buildAttributes());
        event.put(EntityRegistry.SNEAKING_TERROR.get(), SneakingTerror.buildAttributes());
    }

    @SubscribeEvent
    public static void onConfigLoading(final ModConfigEvent.Loading event)
    {
        ConfigManager.onConfigLoading(event);
    }

    @SubscribeEvent
    public static void onConfigReloading(final ModConfigEvent.Reloading event)
    {
        ConfigManager.onConfigReloading(event);
    }

    @SubscribeEvent
    public static void registerOverlaysEvent(final RegisterGuiLayersEvent event)
    {
        SanityMod.getInstance().initGui();
        SanityMod.getInstance().getGui().initOverlays(event);
    }

    @SubscribeEvent
    public static void registerEntityRenderersEvent(final EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(EntityRegistry.ROTTING_STALKER.get(), RendererRottingStalker::new);
        event.registerEntityRenderer(EntityRegistry.SNEAKING_TERROR.get(), RendererSneakingTerror::new);
    }
}