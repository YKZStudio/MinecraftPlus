package dev.yakezhou.minecraftplus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import java.util.List;

public final class MinecraftPlus implements ModInitializer {
	public static final String MOD_ID = "mcplus";

	private static final ResourceKey<Item> ENCHANTED_GOLDEN_CARROT_KEY = ResourceKey.create(
		Registries.ITEM,
		id("enchanted_golden_carrot")
	);
	private static final FoodProperties ENCHANTED_GOLDEN_CARROT_FOOD = new FoodProperties.Builder()
		.nutrition(6)
		.saturationModifier(1.2F)
		.alwaysEdible()
		.build();
	private static final Consumable ENCHANTED_GOLDEN_CARROT_CONSUMABLE = Consumables.defaultFood()
		.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
			new MobEffectInstance(MobEffects.ABSORPTION, 60 * 20, 3),
			new MobEffectInstance(MobEffects.REGENERATION, 15 * 20, 1),
			new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3 * 60 * 20),
			new MobEffectInstance(MobEffects.RESISTANCE, 3 * 60 * 20)
		)))
		.build();
	public static final Item ENCHANTED_GOLDEN_CARROT = new Item(new Item.Properties()
		.setId(ENCHANTED_GOLDEN_CARROT_KEY)
		.rarity(Rarity.EPIC)
		.food(ENCHANTED_GOLDEN_CARROT_FOOD, ENCHANTED_GOLDEN_CARROT_CONSUMABLE)
		.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

	@Override
	public void onInitialize() {
		Ultimine.initialize();

		Registry.register(
			BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
			Identifier.fromNamespaceAndPath(MOD_ID, "arm_explosion"),
			ArmExplosionEffect.CODEC
		);

		Registry.register(BuiltInRegistries.ITEM, ENCHANTED_GOLDEN_CARROT_KEY, ENCHANTED_GOLDEN_CARROT);
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (source.isBuiltin() && BuiltInLootTables.ANCIENT_CITY.equals(key)) {
				tableBuilder.withPool(LootPool.lootPool()
					.when(LootItemRandomChanceCondition.randomChance(0.08F))
					.add(LootItem.lootTableItem(ENCHANTED_GOLDEN_CARROT)));
			}
		});
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
