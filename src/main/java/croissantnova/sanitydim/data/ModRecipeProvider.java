package croissantnova.sanitydim.data;

import croissantnova.sanitydim.SanityMod;
import croissantnova.sanitydim.item.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider
{
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output)
    {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.GARLAND.get())
                .pattern("xxx")
                .pattern("x x")
                .pattern("xxx")
                .define('x', ItemTags.SMALL_FLOWERS)
                .unlockedBy("has_small_flowers", has(ItemTags.SMALL_FLOWERS))
                .save(output, SanityMod.id("garland"));
    }
}
