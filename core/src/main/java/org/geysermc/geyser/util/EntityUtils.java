/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.util;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.ChestBoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.TextDisplayEntity;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.entity.type.living.animal.AnimalEntity;
import org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;

import java.util.Locale;

public final class EntityUtils {
    /**
     * A constant array of the two hands that a player can interact with an entity.
     */
    public static final Hand[] HANDS = Hand.values();

    /**
     * @return a new String array of all known effect identifiers
     */
    public static String[] getAllEffectIdentifiers() {
        String[] identifiers = new String[Effect.VALUES.length];
        for (int i = 0; i < Effect.VALUES.length; i++) {
            identifiers[i] = "minecraft:" + Effect.VALUES[i].name().toLowerCase(Locale.ROOT);
        }

        return identifiers;
    }

    /**
     * Convert Java edition effect IDs to Bedrock edition
     *
     * @param effect Effect to convert
     * @return The numeric ID for the Bedrock edition effect
     */
    public static int toBedrockEffectId(Effect effect) {
        return switch (effect) {
            case GLOWING, LUCK, UNLUCK, DOLPHINS_GRACE -> 0; // All Java-exclusive effects as of 1.16.2
            case LEVITATION -> 24;
            case CONDUIT_POWER -> 26;
            case SLOW_FALLING -> 27;
            case BAD_OMEN -> 28;
            case HERO_OF_THE_VILLAGE -> 29;
            case DARKNESS -> 30;
            case TRIAL_OMEN -> 31;
            case WIND_CHARGED -> 32;
            case WEAVING -> 33;
            case OOZING -> 34;
            case INFESTED -> 35;
            case RAID_OMEN -> 36;
            default -> effect.ordinal() + 1;
        };
    }

    private static float getMountedHeightOffset(Entity mount) {
        if (mount instanceof BoatEntity boat && boat.getVariant() != BoatEntity.BoatVariant.BAMBOO) {
            return -0.1f;
        }

        float height = mount.getBoundingBoxHeight();
        float mountedHeightOffset = height * 0.75f;
        switch (mount.getDefinition().entityType()) {
            case CAMEL -> {
                boolean isBaby = mount.getFlag(EntityFlag.BABY);
                mountedHeightOffset = height - (isBaby ? 0.35f : 0.6f);
            }
            case CAVE_SPIDER, CHICKEN, SPIDER -> mountedHeightOffset = height * 0.5f;
            case DONKEY, MULE -> mountedHeightOffset -= 0.25f;
            case TRADER_LLAMA, LLAMA -> mountedHeightOffset = height * 0.6f;
            case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                    COMMAND_BLOCK_MINECART -> mountedHeightOffset = 0;
            case BAMBOO_RAFT, BAMBOO_CHEST_RAFT -> mountedHeightOffset = 0.25f;
            case HOGLIN, ZOGLIN -> {
                boolean isBaby = mount.getFlag(EntityFlag.BABY);
                mountedHeightOffset = height - (isBaby ? 0.2f : 0.15f);
            }
            case PIGLIN -> mountedHeightOffset = height * 0.92f;
            case PHANTOM -> mountedHeightOffset = height * 0.35f;
            case RAVAGER -> mountedHeightOffset = 2.1f;
            case SKELETON_HORSE -> mountedHeightOffset -= 0.1875f;
            case SNIFFER -> mountedHeightOffset = 1.8f;
            case STRIDER -> mountedHeightOffset = height - 0.19f;
        }
        return mountedHeightOffset;
    }

    private static float getHeightOffset(Entity passenger) {
        boolean isBaby;
        switch (passenger.getDefinition().entityType()) {
            case ALLAY, VEX:
                return 0.4f;
            case SKELETON, STRAY, WITHER_SKELETON:
                return -0.6f;
            case ARMOR_STAND:
                if (((ArmorStandEntity) passenger).isMarker()) {
                    return 0.0f;
                } else {
                    return 0.1f;
                }
            case ENDERMITE, SILVERFISH:
                return 0.1f;
            case PIGLIN, PIGLIN_BRUTE, ZOMBIFIED_PIGLIN:
                isBaby = passenger.getFlag(EntityFlag.BABY);
                return isBaby ? -0.05f : -0.45f;
            case DROWNED, HUSK, ZOMBIE_VILLAGER, ZOMBIE:
                isBaby = passenger.getFlag(EntityFlag.BABY);
                return isBaby ? 0.0f : -0.45f;
            case EVOKER, ILLUSIONER, PILLAGER, RAVAGER, VINDICATOR, WITCH:
                return -0.45f;
            case PLAYER:
                return -0.35f;
            case SHULKER:
                Entity vehicle = passenger.getVehicle();
                if (vehicle instanceof BoatEntity || vehicle.getDefinition() == EntityDefinitions.MINECART) {
                    return 0.1875f - getMountedHeightOffset(vehicle);
                }
        }
        if (passenger instanceof AnimalEntity) {
            return 0.14f;
        }
        return 0f;
    }

    /**
     * Adjust an entity's height if they have mounted/dismounted an entity.
     */
    public static void updateMountOffset(Entity passenger, Entity mount, boolean rider, boolean riding, int index, int passengers) {
        passenger.setFlag(EntityFlag.RIDING, riding);
        if (riding) {
            // Without the Y offset, Bedrock players will find themselves in the floor when mounting
            float mountedHeightOffset = getMountedHeightOffset(mount);
            float heightOffset = getHeightOffset(passenger);

            float xOffset = 0;
            float yOffset = mountedHeightOffset + heightOffset;
            float zOffset = 0;
            switch (mount.getDefinition().entityType()) {
                case CAMEL -> {
                    zOffset = 0.5f;
                    if (passengers > 1) {
                        if (!rider) {
                            zOffset = -0.7f;
                        }
                        if (passenger instanceof AnimalEntity) {
                            zOffset += 0.2f;
                        }
                    }
                    if (mount.getFlag(EntityFlag.SITTING)) {
                        if (mount.getFlag(EntityFlag.BABY)) {
                            yOffset += CamelEntity.SITTING_HEIGHT_DIFFERENCE * 0.5f;
                        } else {
                            yOffset += CamelEntity.SITTING_HEIGHT_DIFFERENCE;
                        }
                    }
                }
                case CHICKEN -> zOffset = -0.1f;
                case TRADER_LLAMA, LLAMA -> zOffset = -0.3f;
                case TEXT_DISPLAY -> {
                    if (passenger instanceof TextDisplayEntity textDisplay) {
                        Vector3f displayTranslation = textDisplay.getTranslation();
                        if (displayTranslation == null) {
                            return;
                        }

                        xOffset = displayTranslation.getX();
                        yOffset = displayTranslation.getY() + 0.2f;
                        zOffset = displayTranslation.getZ();
                    }
                }
                case PLAYER -> {
                    if (passenger instanceof TextDisplayEntity textDisplay) {
                        Vector3f displayTranslation = textDisplay.getTranslation();
                        int lines = textDisplay.getLineCount();
                        if (displayTranslation != null && lines != 0) {
                            float multiplier = .1414f;
                            xOffset = displayTranslation.getX();
                            yOffset += displayTranslation.getY() + multiplier * lines;
                            zOffset = displayTranslation.getZ();
                        }
                    }
                }
                case HAPPY_GHAST -> {
                    int seatingIndex = Math.min(index, 4);
                    xOffset = HappyGhastEntity.X_OFFSETS[seatingIndex];
                    yOffset = 3.4f;
                    zOffset = HappyGhastEntity.Z_OFFSETS[seatingIndex];
                }
            }
            if (mount instanceof ChestBoatEntity) {
                xOffset = 0.15F;
            } else if (mount instanceof BoatEntity) {
                // Without the X offset, more than one entity on a boat is stacked on top of each other
                if (passengers > 1) {
                    xOffset = rider ? 0.2f : -0.6f;
                    if (passenger instanceof AnimalEntity) {
                        xOffset += 0.2f;
                    }
                }
            }
            /*
             * Bedrock Differences
             * Zoglin & Hoglin seem to be taller in Bedrock edition
             * Horses are tinier
             * Players, Minecarts, and Boats have different origins
             */
            if (mount.getDefinition().entityType() == EntityType.PLAYER) {
                yOffset -= EntityDefinitions.PLAYER.offset();
            }
            if (passenger.getDefinition().entityType() == EntityType.PLAYER) {
                yOffset += EntityDefinitions.PLAYER.offset();
            }
            switch (mount.getDefinition().entityType()) {
                case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                        COMMAND_BLOCK_MINECART -> yOffset -= mount.getDefinition().height() * 0.5f;
            }
            switch (passenger.getDefinition().entityType()) {
                case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                        COMMAND_BLOCK_MINECART -> yOffset += passenger.getDefinition().height() * 0.5f;
                case FALLING_BLOCK -> yOffset += 0.5f;
            }
            if (mount instanceof BoatEntity) {
                yOffset -= mount.getDefinition().height() * 0.5f;
            }
            if (passenger instanceof BoatEntity) {
                yOffset += passenger.getDefinition().height() * 0.5f;
            }
            if (mount instanceof ArmorStandEntity armorStand) {
                yOffset -= armorStand.getYOffset();
            }
            passenger.setRiderSeatPosition(Vector3f.from(xOffset, yOffset, zOffset));
        }
    }

    public static void updateRiderRotationLock(Entity passenger, Entity mount, boolean isRiding) {
        if (isRiding && mount instanceof BoatEntity) {
            // Head rotation is locked while riding in a boat
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION, true);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, 90f);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_HAS_ROTATION, true);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_ROTATION_OFFSET_DEGREES, -90f);
        } else {
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION, false);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, 0f);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_HAS_ROTATION, false);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_ROTATION_OFFSET_DEGREES, 0f);
        }
    }

    /**
     * Determine if an action would result in a successful bucketing of the given entity.
     */
    public static boolean attemptToBucket(GeyserItemStack itemInHand) {
        return itemInHand.asItem() == Items.WATER_BUCKET;
    }

    /**
     * Attempt to determine the result of saddling the given entity.
     */
    public static InteractionResult attemptToSaddle(Entity entityToSaddle, GeyserItemStack itemInHand) {
        if (itemInHand.asItem() == Items.SADDLE) {
            if (!entityToSaddle.getFlag(EntityFlag.SADDLED) && !entityToSaddle.getFlag(EntityFlag.BABY)) {
                // Saddle
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Convert Java GameMode to Bedrock GameType
     * Needed to account for ordinal differences (spectator is 3 in Java, 6 in Bedrock)
     */
    @SuppressWarnings("deprecation") // Must use survival_viewer due to limitations on Bedrock's spectator gamemode
    public static GameType toBedrockGamemode(GameMode gamemode) {
        return switch (gamemode) {
            case CREATIVE -> GameType.CREATIVE;
            case ADVENTURE -> GameType.ADVENTURE;
            case SPECTATOR -> GameType.SURVIVAL_VIEWER;
            default -> GameType.SURVIVAL;
        };
    }

    private static String translatedEntityName(@NonNull String namespace, @NonNull String name, @NonNull GeyserSession session) {
        // MinecraftLocale would otherwise invoke getBootstrap (which doesn't exist) and create some folders,
        // so use the default fallback value as used in Minecraft Java
        if (EnvironmentUtils.IS_UNIT_TESTING) {
            return "entity." + namespace + "." + name;
        }
        return MinecraftLocale.getLocaleString("entity." + namespace + "." + name, session.locale());
    }

    public static String translatedEntityName(@NonNull Key type, @NonNull GeyserSession session) {
        return translatedEntityName(type.namespace(), type.value(), session);
    }

    public static String translatedEntityName(@Nullable EntityType type, @NonNull GeyserSession session) {
        if (type == EntityType.PLAYER) {
            return "Player"; // the player's name is always shown instead
        }
        // default fallback value as used in Minecraft Java
        if (type == null) {
            return "entity.unregistered_sadface";
        }
        // this works at least with all 1.20.5 entities, except the killer bunny since that's not an entity type.
        String typeName = type.name().toLowerCase(Locale.ROOT);
        return translatedEntityName("minecraft", typeName, session);
    }

    public static boolean equipmentUsableByEntity(GeyserSession session, Equippable equippable, EntityType entity) {
        if (equippable.allowedEntities() == null) {
            return true;
        }

        GeyserHolderSet<EntityType> holderSet = GeyserHolderSet.fromHolderSet(JavaRegistries.ENTITY_TYPE, equippable.allowedEntities());
        return session.getTagCache().is(holderSet, entity);
    }

    private EntityUtils() {
    }
}
