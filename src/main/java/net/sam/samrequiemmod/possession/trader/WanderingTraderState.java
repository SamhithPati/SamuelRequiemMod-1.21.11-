package net.sam.samrequiemmod.possession.trader;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WanderingTraderState {

    public static final int DRINK_NONE = 0;
    public static final int DRINK_INVIS = 1;
    public static final int DRINK_MILK = 2;

    private static final Map<UUID, Long> SERVER_TEMP_INVIS_UNTIL = new ConcurrentHashMap<>();
    private static final Set<UUID> SERVER_NIGHT_INVIS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> CLIENT_DRINK_TYPE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CLIENT_DRINK_UNTIL = new ConcurrentHashMap<>();

    private WanderingTraderState() {
    }

    public static void setServerTempInvisUntil(UUID uuid, long untilTick) {
        if (untilTick <= 0L) {
            SERVER_TEMP_INVIS_UNTIL.remove(uuid);
        } else {
            SERVER_TEMP_INVIS_UNTIL.put(uuid, untilTick);
        }
    }

    public static long getServerTempInvisUntil(UUID uuid) {
        return SERVER_TEMP_INVIS_UNTIL.getOrDefault(uuid, 0L);
    }

    public static void setServerNightInvis(UUID uuid, boolean active) {
        if (active) {
            SERVER_NIGHT_INVIS.add(uuid);
        } else {
            SERVER_NIGHT_INVIS.remove(uuid);
        }
    }

    public static boolean isServerNightInvis(UUID uuid) {
        return SERVER_NIGHT_INVIS.contains(uuid);
    }

    public static void startClientDrink(UUID uuid, int drinkType, long untilTick) {
        if (drinkType == DRINK_NONE || untilTick <= 0L) {
            CLIENT_DRINK_TYPE.remove(uuid);
            CLIENT_DRINK_UNTIL.remove(uuid);
            return;
        }
        CLIENT_DRINK_TYPE.put(uuid, drinkType);
        CLIENT_DRINK_UNTIL.put(uuid, untilTick);
    }

    public static int getClientDrinkType(UUID uuid, long currentTick) {
        long until = CLIENT_DRINK_UNTIL.getOrDefault(uuid, 0L);
        if (currentTick > until) {
            CLIENT_DRINK_TYPE.remove(uuid);
            CLIENT_DRINK_UNTIL.remove(uuid);
            return DRINK_NONE;
        }
        return CLIENT_DRINK_TYPE.getOrDefault(uuid, DRINK_NONE);
    }

    public static boolean isClientDrinking(UUID uuid, long currentTick) {
        return getClientDrinkType(uuid, currentTick) != DRINK_NONE;
    }

    public static ItemStack getClientDrinkStack(UUID uuid, long currentTick) {
        return switch (getClientDrinkType(uuid, currentTick)) {
            case DRINK_INVIS -> Items.POTION.getDefaultStack();
            case DRINK_MILK -> Items.MILK_BUCKET.getDefaultStack();
            default -> ItemStack.EMPTY;
        };
    }

    public static void clear(UUID uuid) {
        SERVER_TEMP_INVIS_UNTIL.remove(uuid);
        SERVER_NIGHT_INVIS.remove(uuid);
        CLIENT_DRINK_TYPE.remove(uuid);
        CLIENT_DRINK_UNTIL.remove(uuid);
    }

    public static void clearAllClient() {
        CLIENT_DRINK_TYPE.clear();
        CLIENT_DRINK_UNTIL.clear();
    }
}
