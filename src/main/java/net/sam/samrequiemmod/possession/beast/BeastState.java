package net.sam.samrequiemmod.possession.beast;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BeastState {

    private static final Map<UUID, Integer> SERVER_HORSE_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CLIENT_HORSE_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SERVER_RABBIT_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CLIENT_RABBIT_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SERVER_AXOLOTL_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CLIENT_AXOLOTL_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CLIENT_SHULKER_OPEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SERVER_SHULKER_OPEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, double[]> SERVER_SHULKER_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SERVER_AXOLOTL_PLAY_DEAD_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CLIENT_AXOLOTL_PLAY_DEAD_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SERVER_BEE_ANGRY = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CLIENT_BEE_ANGRY = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SERVER_PARROT_FLYING = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CLIENT_PARROT_FLYING = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SERVER_ARMADILLO_CURLED = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CLIENT_ARMADILLO_CURLED = new ConcurrentHashMap<>();

    private BeastState() {}

    public static void setServerHorseVariant(UUID uuid, int variant) {
        SERVER_HORSE_VARIANTS.put(uuid, variant);
    }

    public static int getServerHorseVariant(UUID uuid) {
        return SERVER_HORSE_VARIANTS.getOrDefault(uuid, 0);
    }

    public static void setClientHorseVariant(UUID uuid, int variant) {
        CLIENT_HORSE_VARIANTS.put(uuid, variant);
    }

    public static int getClientHorseVariant(UUID uuid) {
        return CLIENT_HORSE_VARIANTS.getOrDefault(uuid, 0);
    }

    public static void setServerRabbitVariant(UUID uuid, int variant) {
        SERVER_RABBIT_VARIANTS.put(uuid, variant);
    }

    public static int getServerRabbitVariant(UUID uuid) {
        return SERVER_RABBIT_VARIANTS.getOrDefault(uuid, 0);
    }

    public static void setClientRabbitVariant(UUID uuid, int variant) {
        CLIENT_RABBIT_VARIANTS.put(uuid, variant);
    }

    public static int getClientRabbitVariant(UUID uuid) {
        return CLIENT_RABBIT_VARIANTS.getOrDefault(uuid, 0);
    }

    public static void setServerAxolotlVariant(UUID uuid, int variant) {
        SERVER_AXOLOTL_VARIANTS.put(uuid, variant);
    }

    public static int getServerAxolotlVariant(UUID uuid) {
        return SERVER_AXOLOTL_VARIANTS.getOrDefault(uuid, 0);
    }

    public static void setClientAxolotlVariant(UUID uuid, int variant) {
        CLIENT_AXOLOTL_VARIANTS.put(uuid, variant);
    }

    public static int getClientAxolotlVariant(UUID uuid) {
        return CLIENT_AXOLOTL_VARIANTS.getOrDefault(uuid, 0);
    }

    public static void setServerBeeAngry(UUID uuid, boolean angry) {
        if (angry) SERVER_BEE_ANGRY.put(uuid, true);
        else SERVER_BEE_ANGRY.remove(uuid);
    }

    public static boolean isServerBeeAngry(UUID uuid) {
        return SERVER_BEE_ANGRY.getOrDefault(uuid, false);
    }

    public static void setClientBeeAngry(UUID uuid, boolean angry) {
        if (angry) CLIENT_BEE_ANGRY.put(uuid, true);
        else CLIENT_BEE_ANGRY.remove(uuid);
    }

    public static boolean isClientBeeAngry(UUID uuid) {
        return CLIENT_BEE_ANGRY.getOrDefault(uuid, false);
    }

    public static void setServerParrotFlying(UUID uuid, boolean flying) {
        if (flying) SERVER_PARROT_FLYING.put(uuid, true);
        else SERVER_PARROT_FLYING.remove(uuid);
    }

    public static boolean isServerParrotFlying(UUID uuid) {
        return SERVER_PARROT_FLYING.getOrDefault(uuid, false);
    }

    public static void setClientParrotFlying(UUID uuid, boolean flying) {
        if (flying) CLIENT_PARROT_FLYING.put(uuid, true);
        else CLIENT_PARROT_FLYING.remove(uuid);
    }

    public static boolean isClientParrotFlying(UUID uuid) {
        return CLIENT_PARROT_FLYING.getOrDefault(uuid, false);
    }

    public static void setServerArmadilloCurled(UUID uuid, boolean curled) {
        if (curled) SERVER_ARMADILLO_CURLED.put(uuid, true);
        else SERVER_ARMADILLO_CURLED.remove(uuid);
    }

    public static boolean isServerArmadilloCurled(UUID uuid) {
        return SERVER_ARMADILLO_CURLED.getOrDefault(uuid, false);
    }

    public static void setClientArmadilloCurled(UUID uuid, boolean curled) {
        if (curled) CLIENT_ARMADILLO_CURLED.put(uuid, true);
        else CLIENT_ARMADILLO_CURLED.remove(uuid);
    }

    public static boolean isClientArmadilloCurled(UUID uuid) {
        return CLIENT_ARMADILLO_CURLED.getOrDefault(uuid, false);
    }

    public static void setServerShulkerOpenUntil(UUID uuid, long untilTick) {
        if (untilTick <= 0L) SERVER_SHULKER_OPEN_UNTIL.remove(uuid);
        else SERVER_SHULKER_OPEN_UNTIL.put(uuid, untilTick);
    }

    public static void setServerShulkerAnchor(UUID uuid, double x, double y, double z) {
        SERVER_SHULKER_ANCHORS.put(uuid, new double[]{x, y, z});
    }

    public static double[] getServerShulkerAnchor(UUID uuid) {
        return SERVER_SHULKER_ANCHORS.get(uuid);
    }

    public static void setServerAxolotlPlayDeadUntil(UUID uuid, long untilTick) {
        if (untilTick <= 0L) SERVER_AXOLOTL_PLAY_DEAD_UNTIL.remove(uuid);
        else SERVER_AXOLOTL_PLAY_DEAD_UNTIL.put(uuid, untilTick);
    }

    public static boolean isServerAxolotlPlayingDead(UUID uuid, long currentTick) {
        long until = SERVER_AXOLOTL_PLAY_DEAD_UNTIL.getOrDefault(uuid, 0L);
        if (until <= currentTick) {
            SERVER_AXOLOTL_PLAY_DEAD_UNTIL.remove(uuid);
            return false;
        }
        return true;
    }

    public static void setClientAxolotlPlayDeadUntil(UUID uuid, long untilTick) {
        if (untilTick <= 0L) CLIENT_AXOLOTL_PLAY_DEAD_UNTIL.remove(uuid);
        else CLIENT_AXOLOTL_PLAY_DEAD_UNTIL.put(uuid, untilTick);
    }

    public static boolean isClientAxolotlPlayingDead(UUID uuid, long currentTick) {
        long until = CLIENT_AXOLOTL_PLAY_DEAD_UNTIL.getOrDefault(uuid, 0L);
        if (until <= currentTick) {
            CLIENT_AXOLOTL_PLAY_DEAD_UNTIL.remove(uuid);
            return false;
        }
        return true;
    }

    public static boolean isServerShulkerOpen(UUID uuid, long currentTick) {
        long until = SERVER_SHULKER_OPEN_UNTIL.getOrDefault(uuid, 0L);
        if (until <= currentTick) {
            SERVER_SHULKER_OPEN_UNTIL.remove(uuid);
            return false;
        }
        return true;
    }

    public static void setClientShulkerOpenUntil(UUID uuid, long untilTick) {
        if (untilTick <= 0L) CLIENT_SHULKER_OPEN_UNTIL.remove(uuid);
        else CLIENT_SHULKER_OPEN_UNTIL.put(uuid, untilTick);
    }

    public static boolean isClientShulkerOpen(UUID uuid, long currentTick) {
        long until = CLIENT_SHULKER_OPEN_UNTIL.getOrDefault(uuid, 0L);
        if (until <= currentTick) {
            CLIENT_SHULKER_OPEN_UNTIL.remove(uuid);
            return false;
        }
        return true;
    }

    public static void clear(UUID uuid) {
        SERVER_HORSE_VARIANTS.remove(uuid);
        CLIENT_HORSE_VARIANTS.remove(uuid);
        SERVER_RABBIT_VARIANTS.remove(uuid);
        CLIENT_RABBIT_VARIANTS.remove(uuid);
        SERVER_AXOLOTL_VARIANTS.remove(uuid);
        CLIENT_AXOLOTL_VARIANTS.remove(uuid);
        SERVER_SHULKER_OPEN_UNTIL.remove(uuid);
        CLIENT_SHULKER_OPEN_UNTIL.remove(uuid);
        SERVER_SHULKER_ANCHORS.remove(uuid);
        SERVER_AXOLOTL_PLAY_DEAD_UNTIL.remove(uuid);
        CLIENT_AXOLOTL_PLAY_DEAD_UNTIL.remove(uuid);
        SERVER_BEE_ANGRY.remove(uuid);
        CLIENT_BEE_ANGRY.remove(uuid);
        SERVER_PARROT_FLYING.remove(uuid);
        CLIENT_PARROT_FLYING.remove(uuid);
        SERVER_ARMADILLO_CURLED.remove(uuid);
        CLIENT_ARMADILLO_CURLED.remove(uuid);
    }
}






