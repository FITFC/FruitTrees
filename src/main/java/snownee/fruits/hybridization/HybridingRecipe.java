package snownee.fruits.hybridization;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.fruits.FruitType;
import snownee.fruits.FruitsMod;
import snownee.kiwi.recipe.Simple;
import snownee.kiwi.util.Util;

public class HybridingRecipe extends Simple<HybridingContext> {

	protected final Either<FruitType, Block> result;
	public final ImmutableSet<Either<FruitType, Block>> ingredients;

	public HybridingRecipe(ResourceLocation id, Either<FruitType, Block> result, ImmutableSet<Either<FruitType, Block>> ingredients) {
		super(id);
		this.result = result;
		this.ingredients = ingredients;
	}

	@Override
	public boolean matches(HybridingContext inv, Level worldIn) {
		return ingredients.stream().allMatch(inv.ingredients::contains);
	}

	public Either<FruitType, Block> getResult(Collection<Either<FruitType, Block>> types) {
		return result;
	}

	public Block getResultAsBlock(Collection<Either<FruitType, Block>> types) {
		return getResult(types).map(t -> t.leaves.get(), b -> b);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return Hybridization.SERIALIZER;
	}

	@Override
	public RecipeType<?> getType() {
		return Hybridization.RECIPE_TYPE;
	}

	public static class Serializer implements RecipeSerializer<HybridingRecipe> {

		@Override
		public HybridingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
			Either<FruitType, Block> result = readIngredient(GsonHelper.getAsJsonObject(json, "result"));
			ImmutableSet.Builder<Either<FruitType, Block>> builder = ImmutableSet.builder();
			JsonArray ingredients = GsonHelper.getAsJsonArray(json, "ingredients");
			if (ingredients.size() < 2 || ingredients.size() > 4) {
				throw new JsonSyntaxException("Size of ingredients has to be in [2, 4]");
			}
			ingredients.forEach(e -> builder.add(readIngredient(e.getAsJsonObject())));
			return new HybridingRecipe(recipeId, result, builder.build());
		}

		protected static Either<FruitType, Block> readIngredient(JsonObject object) {
			if (object.has("block")) {
				Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(GsonHelper.getAsString(object, "block")));
				Preconditions.checkArgument(block != Blocks.AIR);
				return Either.right(block);
			} else if (object.has("fruit")) {
				FruitType type = FruitType.REGISTRY.getValue(Util.RL(GsonHelper.getAsString(object, "fruit"), FruitsMod.ID));
				Preconditions.checkNotNull(type);
				return Either.left(type);
			}
			throw new JsonSyntaxException("Expect key 'block' or 'fruit'");
		}

		@Override
		public HybridingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
			Either<FruitType, Block> result;
			if (buffer.readBoolean()) {
				result = Either.left(buffer.readRegistryIdUnsafe(FruitType.REGISTRY));
			} else {
				result = Either.right(buffer.readRegistryIdUnsafe(ForgeRegistries.BLOCKS));
			}
			ImmutableSet.Builder<Either<FruitType, Block>> builder = ImmutableSet.builder();
			int size = buffer.readByte(); // blocks
			for (int i = 0; i < size; i++) {
				Block block = buffer.readRegistryIdUnsafe(ForgeRegistries.BLOCKS);
				builder.add(Either.right(block));
			}
			size = buffer.readByte(); // types
			for (int i = 0; i < size; i++) {
				builder.add(Either.left(buffer.readRegistryIdUnsafe(FruitType.REGISTRY)));
			}
			return new HybridingRecipe(recipeId, result, builder.build());
		}

		@Override
		public void toNetwork(FriendlyByteBuf buffer, HybridingRecipe recipe) {
			recipe.result.ifLeft(type -> {
				buffer.writeBoolean(true);
				buffer.writeRegistryIdUnsafe(FruitType.REGISTRY, type);
			}).ifRight(block -> {
				buffer.writeBoolean(false);
				buffer.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS, block);
			});
			List<FruitType> types = Lists.newArrayList();
			List<Block> blocks = Lists.newArrayList();
			recipe.ingredients.forEach(e -> e.map(types::add, blocks::add));
			buffer.writeByte(blocks.size());
			for (Block block : blocks) {
				buffer.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS, block);
			}
			buffer.writeByte(types.size());
			for (FruitType type : types) {
				buffer.writeRegistryIdUnsafe(FruitType.REGISTRY, type);
			}
		}

	}

}
