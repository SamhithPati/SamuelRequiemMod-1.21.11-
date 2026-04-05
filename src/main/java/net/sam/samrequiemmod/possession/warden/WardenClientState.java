package net.sam.samrequiemmod.possession.warden;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WardenClientState {

    public static final int ATTACK_DURATION = 12;
    public static final int SONIC_DURATION = 60;
    public static final int ROAR_DURATION = 84;
    public static final int SNIFF_DURATION = 40;

    private static final Map<UUID, Integer> ATTACK_START = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SONIC_START = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ROAR_START = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SNIFF_START = new ConcurrentHashMap<>();

    private WardenClientState() {}

    public static void startAttack(UUID playerUuid, int startTick) {
        ATTACK_START.put(playerUuid, startTick);
    }

    public static void startSonic(UUID playerUuid, int startTick) {
        putStart(SONIC_START, playerUuid, startTick);
    }

    public static void startRoar(UUID playerUuid, int startTick) {
        putStart(ROAR_START, playerUuid, startTick);
    }

    public static void startSniff(UUID playerUuid, int startTick) {
        putStart(SNIFF_START, playerUuid, startTick);
    }

    public static boolean isAttacking(UUID playerUuid, int currentAge) {
        return isActive(ATTACK_START, playerUuid, currentAge, ATTACK_DURATION);
    }

    public static int getAttackStart(UUID playerUuid) {
        return ATTACK_START.getOrDefault(playerUuid, Integer.MIN_VALUE);
    }

    public static boolean isChargingSonic(UUID playerUuid, int currentAge) {
        return isActive(SONIC_START, playerUuid, currentAge, SONIC_DURATION);
    }

    public static boolean isRoaring(UUID playerUuid, int currentAge) {
        return isActive(ROAR_START, playerUuid, currentAge, ROAR_DURATION);
    }

    public static boolean isSniffing(UUID playerUuid, int currentAge) {
        return isActive(SNIFF_START, playerUuid, currentAge, SNIFF_DURATION);
    }

    public static void clear(UUID playerUuid) {
        ATTACK_START.remove(playerUuid);
        SONIC_START.remove(playerUuid);
        ROAR_START.remove(playerUuid);
        SNIFF_START.remove(playerUuid);
    }

    private static boolean isActive(Map<UUID, Integer> map, UUID playerUuid, int currentAge, int duration) {
        Integer start = map.get(playerUuid);
        if (start == null) return false;
        int elapsed = currentAge - start;
        if (elapsed < 0 || elapsed >= duration) {
            map.remove(playerUuid);
            return false;
        }
        return true;
    }

    private static void putStart(Map<UUID, Integer> map, UUID playerUuid, int startTick) {
        map.merge(playerUuid, startTick, Math::max);
    }
}
