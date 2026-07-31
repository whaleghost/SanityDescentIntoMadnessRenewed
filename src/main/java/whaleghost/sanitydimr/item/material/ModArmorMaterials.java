package whaleghost.sanitydimr.item.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.sound.SoundRegistry;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> REGISTRY =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, SanityMod.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FLOWER = REGISTRY.register("flower",
            () -> new ArmorMaterial(
                    new EnumMap<>(ArmorItem.Type.class) {{ put(ArmorItem.Type.HELMET, 0); }},
                    0,
                    SoundRegistry.FLOWERS_EQUIP,
                    () -> Ingredient.of(ItemTags.SMALL_FLOWERS),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "flower"))),
                    0f,
                    0f));

    private ModArmorMaterials() {}
}
