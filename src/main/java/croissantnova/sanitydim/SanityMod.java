package croissantnova.sanitydim;

import com.mojang.logging.LogUtils;
import croissantnova.sanitydim.capability.InnerEntityCapImpl;
import croissantnova.sanitydim.capability.Sanity;
import croissantnova.sanitydim.capability.SanityLevelChunk;
import croissantnova.sanitydim.client.GuiHandler;
import croissantnova.sanitydim.config.ConfigManager;
import croissantnova.sanitydim.entity.EntityRegistry;
import croissantnova.sanitydim.event.EventHandler;
import croissantnova.sanitydim.event.ModEventHandler;
import croissantnova.sanitydim.item.ItemRegistry;
import croissantnova.sanitydim.net.PacketHandler;
import croissantnova.sanitydim.sound.SoundRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.ModLoadingContext;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.List;

@Mod(SanityMod.MODID)
public class SanityMod
{
    @OnlyIn(Dist.CLIENT)
    private GuiHandler m_gui;

    private static SanityMod m_inst;
    public static final String MODID = "sanitydim";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<ArmorMaterial, ArmorMaterial> FLOWER_ARMOR_MATERIAL =
            ARMOR_MATERIALS.register("flower", () -> new ArmorMaterial(
                    new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class) {{ put(ArmorItem.Type.HELMET, 0); }},
                    0,
                    SoundRegistry.FLOWERS_EQUIP,
                    () -> Ingredient.of(ItemTags.SMALL_FLOWERS),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "flower"))),
                    0f,
                    0f
            ));

    public SanityMod(IEventBus modEventBus)
    {
        m_inst = this;

        ConfigManager.register();

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(ModEventHandler::addEntityAttributes);
        modEventBus.addListener(ModEventHandler::onConfigLoading);
        modEventBus.addListener(ModEventHandler::registerOverlaysEvent);
        modEventBus.addListener(ModEventHandler::registerEntityRenderersEvent);
        modEventBus.addListener(PacketHandler::register);
        ATTACHMENT_TYPES.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        // Ensure attachment type classes are loaded so their static initializers run
        Sanity.ATTACHMENT.getClass();
        SanityLevelChunk.ATTACHMENT.getClass();
        InnerEntityCapImpl.ATTACHMENT.getClass();
        NeoForge.EVENT_BUS.register(new EventHandler());
        EntityRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        SoundRegistry.register(modEventBus);
    }

    static
    {
        ConfigManager.init();
    }

    private void setup(final FMLCommonSetupEvent event)
    {
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        initGui();
        //EntityRenderers.register(EntityRegistry.SHADE_CHOMPER.get(), RendererShadeChomper::new);
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
}