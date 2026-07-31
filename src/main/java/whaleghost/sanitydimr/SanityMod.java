package whaleghost.sanitydimr;

import com.mojang.logging.LogUtils;
import whaleghost.sanitydimr.capability.InnerEntityCapImpl;
import whaleghost.sanitydimr.capability.Sanity;
import whaleghost.sanitydimr.capability.SanityLevelChunk;
import whaleghost.sanitydimr.client.GuiHandler;
import whaleghost.sanitydimr.config.ConfigManager;
import whaleghost.sanitydimr.entity.EntityRegistry;
import whaleghost.sanitydimr.event.BlockEventHandler;
import whaleghost.sanitydimr.event.ClientEventHandler;
import whaleghost.sanitydimr.event.EntityInteractionEventHandler;
import whaleghost.sanitydimr.event.ModEventHandler;
import whaleghost.sanitydimr.event.ServerSanityEventHandler;
import whaleghost.sanitydimr.item.ItemRegistry;
import whaleghost.sanitydimr.item.material.ModArmorMaterials;
import whaleghost.sanitydimr.net.PacketHandler;
import whaleghost.sanitydimr.sound.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(SanityMod.MODID)
public class SanityMod
{
    @OnlyIn(Dist.CLIENT)
    private GuiHandler m_gui;

    private static SanityMod m_inst;
    public static final String MODID = "sanitydimr";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public SanityMod(IEventBus modEventBus)
    {
        m_inst = this;

        ConfigManager.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModEventHandler::addEntityAttributes);
        modEventBus.addListener(ModEventHandler::onConfigLoading);
        modEventBus.addListener(ModEventHandler::registerOverlaysEvent);
        modEventBus.addListener(ModEventHandler::registerEntityRenderersEvent);
        modEventBus.addListener(PacketHandler::register);
        ATTACHMENT_TYPES.register(modEventBus);
        Sanity.ATTACHMENT = ATTACHMENT_TYPES.register(
                "sanity", () -> AttachmentType.serializable(Sanity::new).build());
        SanityLevelChunk.ATTACHMENT = ATTACHMENT_TYPES.register(
                "sanity_level_chunk", () -> AttachmentType.serializable(SanityLevelChunk::new).build());
        InnerEntityCapImpl.ATTACHMENT = ATTACHMENT_TYPES.register(
                "inner_entity_cap", () -> AttachmentType.builder(InnerEntityCapImpl::new).build());
        ModArmorMaterials.REGISTRY.register(modEventBus);
        NeoForge.EVENT_BUS.register(new ServerSanityEventHandler());
        NeoForge.EVENT_BUS.register(new EntityInteractionEventHandler());
        NeoForge.EVENT_BUS.register(new BlockEventHandler());
        NeoForge.EVENT_BUS.register(new ClientEventHandler());
        EntityRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        SoundRegistry.register(modEventBus);
    }

    static
    {
        ConfigManager.init();
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
    }

    @OnlyIn(Dist.CLIENT)
    public void initGui()
    {
        if (m_gui == null) m_gui = new GuiHandler();
    }

    @OnlyIn(Dist.CLIENT)
    public GuiHandler getGui()
    {
        return m_gui;
    }

    public static SanityMod getInstance()
    {
        return m_inst;
    }

    public static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}