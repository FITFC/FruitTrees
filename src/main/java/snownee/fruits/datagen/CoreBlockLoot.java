package snownee.fruits.datagen;

import java.util.Set;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import snownee.fruits.FruitType;
import snownee.fruits.block.FruitLeavesBlock;
import snownee.kiwi.datagen.provider.KiwiBlockLoot;
import snownee.kiwi.util.Util;

public class CoreBlockLoot extends KiwiBlockLoot {

	public CoreBlockLoot() {
		this(Util.RL("fruittrees:core"));
	}

	public CoreBlockLoot(ResourceLocation moduleId) {
		super(moduleId, Set.of(), FeatureFlags.REGISTRY.allFlags());
	}

	@Override
	protected void addTables() {
		handleDefault($ -> createSingleItemTable($));
		handle(DoorBlock.class, $ -> createDoorTable($));
		handle(SlabBlock.class, $ -> createSlabItemTable($));
		handle(FlowerPotBlock.class, $ -> createPotFlowerItemTable(((FlowerPotBlock) $).getContent()));
		handle(FruitLeavesBlock.class, $ -> createFruitLeaves($));
	}

	private static final float[] NORMAL_LEAVES_SAPLING_CHANCES = new float[] { 0.05F, 0.0625F, 0.083333336F, 0.1F };

	public LootTable.Builder createFruitLeaves(Block block) {
		FruitLeavesBlock leavesBlock = (FruitLeavesBlock) block;
		FruitType type = leavesBlock.type.get();
		LootTable.Builder loot = createLeavesDrops(leavesBlock, type.sapling.get(), NORMAL_LEAVES_SAPLING_CHANCES);

		LootPool.Builder pool = LootPool.lootPool();
		pool.when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(FruitLeavesBlock.AGE, 3))).add(LootItem.lootTableItem(type.fruit.get()));
		loot.withPool(pool);

		return loot;
	}

}
