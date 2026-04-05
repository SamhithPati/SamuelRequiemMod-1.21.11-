package net.sam.samrequiemmod.possession.illager;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side state for the illager captain system.
 * A captain is an illager-possessed player wearing an ominous banner on their head.
 */
public final class CaptainState {

    private CaptainState() {}

    /** Player UUID -> Set of follower mob UUIDs */
    private static final Map<UUID, Set<UUID>> FOLLOWERS = new ConcurrentHashMap<>();

    /** Mob UUID -> Captain player UUID (reverse lookup) */
    private static final Map<UUID, UUID> FOLLOWER_TO_CAPTAIN = new ConcurrentHashMap<>();

    /** Captain UUID -> commanded attack target UUID */
    private static final Map<UUID, UUID> COMMANDED_TARGET = new ConcurrentHashMap<>();

    /** Players who were captains last tick (for detecting banner removal) */
    static final Set<UUID> WAS_CAPTAIN = ConcurrentHashMap.newKeySet();

    public static boolean isCaptain(ServerPlayerEntity player) {
        if (!RavagerRidingHandler.isIllagerPossessed(player)) return false;
        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        return !head.isEmpty() && head.getItem() instanceof BannerItem;
    }

    public static void addFollower(UUID captainUuid, UUID mobUuid) {
        // Remove from old captain if transferring
        UUID oldCaptain = FOLLOWER_TO_CAPTAIN.get(mobUuid);
        if (oldCaptain != null && !oldCaptain.equals(captainUuid)) {
            Set<UUID> oldSet = FOLLOWERS.get(oldCaptain);
            if (oldSet != null) oldSet.remove(mobUuid);
        }
        FOLLOWERS.computeIfAbsent(captainUuid, k -> ConcurrentHashMap.newKeySet()).add(mobUuid);
        FOLLOWER_TO_CAPTAIN.put(mobUuid, captainUuid);
    }

    public static void removeFollower(UUID captainUuid, UUID mobUuid) {
        Set<UUID> set = FOLLOWERS.get(captainUuid);
        if (set != null) set.remove(mobUuid);
        FOLLOWER_TO_CAPTAIN.remove(mobUuid);
    }

    public static boolean isFollowing(UUID mobUuid) {
        return FOLLOWER_TO_CAPTAIN.containsKey(mobUuid);
    }

    public static boolean isFollowingCaptain(UUID captainUuid, UUID mobUuid) {
        return captainUuid.equals(FOLLOWER_TO_CAPTAIN.get(mobUuid));
    }

    public static Set<UUID> getFollowers(UUID captainUuid) {
        Set<UUID> set = FOLLOWERS.get(captainUuid);
        return set != null ? set : Set.of();
    }

    public static void setCommandedTarget(UUID captainUuid, UUID targetUuid) {
        COMMANDED_TARGET.put(captainUuid, targetUuid);
    }

    public static UUID getCommandedTarget(UUID captainUuid) {
        return COMMANDED_TARGET.get(captainUuid);
    }

    public static void clearCommandedTarget(UUID captainUuid) {
        COMMANDED_TARGET.remove(captainUuid);
    }

    /** Clears all captain state: followers, commanded target, was-captain flag. */
    public static void clearAll(UUID captainUuid) {
        Set<UUID> followers = FOLLOWERS.remove(captainUuid);
        if (followers != null) {
            for (UUID f : followers) FOLLOWER_TO_CAPTAIN.remove(f);
        }
        COMMANDED_TARGET.remove(captainUuid);
        WAS_CAPTAIN.remove(captainUuid);
    }
}






