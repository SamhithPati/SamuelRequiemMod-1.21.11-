package net.sam.samrequiemmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.world.World;
import net.sam.samrequiemmod.item.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CorruptedMerchantEntity extends PathAwareEntity implements Merchant {

    private static final int MAX_PURCHASES_PER_PLAYER = 2;
    private final Map<UUID, Integer> purchaseCounts = new HashMap<>();
    private PlayerEntity customer;
    private TradeOfferList offers;
    private int merchantExperience;

    public CorruptedMerchantEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        // Use System.out so it's always visible in console regardless of log config
        this.setCustomName(Text.translatable("entity.samrequiemmod.corrupted_merchant"));
        this.setCustomNameVisible(true);
    }

    // Required: register default attributes so LivingEntity can initialize health etc.
    public static net.minecraft.entity.attribute.DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(2, new LookAroundGoal(this));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.35D));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient()) return ActionResult.SUCCESS;
        int bought = purchaseCounts.getOrDefault(player.getUuid(), 0);
        if (bought >= MAX_PURCHASES_PER_PLAYER) {
            player.sendMessage(Text.literal("I have nothing more to offer you...").formatted(net.minecraft.util.Formatting.DARK_PURPLE), true);
            return ActionResult.SUCCESS;
        }
        this.setCustomer(player);
        this.sendOffers(player, this.getDisplayName(), 1);
        return ActionResult.SUCCESS;
    }

    public void setCustomer(PlayerEntity player) { this.customer = player; }
    public PlayerEntity getCustomer() { return this.customer; }
    public boolean isCustomer(PlayerEntity player) { return this.customer == player; }

    public TradeOfferList getOffers() {
        if (this.offers == null) {
            this.offers = new TradeOfferList();
            int bought = customer != null ? purchaseCounts.getOrDefault(customer.getUuid(), 0) : 0;
            if (bought < 1) this.offers.add(new TradeOffer(new TradedItem(Items.DIAMOND, 5), java.util.Optional.of(new TradedItem(Items.EMERALD, 4)), new ItemStack(ModItems.POSSESSION_RELIC_SHARD, 1), 1, 0, 0.0F));
            if (bought < 2) this.offers.add(new TradeOffer(new TradedItem(Items.DIAMOND, 10), java.util.Optional.of(new TradedItem(Items.EMERALD, 5)), new ItemStack(ModItems.POSSESSION_RELIC_SHARD, 1), 1, 0, 0.0F));
        }
        return this.offers;
    }

    public void onSellingItem(ItemStack stack) {}
    public void trade(TradeOffer offer) { afterUsing(offer); }
    public void afterUsing(TradeOffer offer) {
        if (this.customer != null) { purchaseCounts.merge(customer.getUuid(), 1, Integer::sum); this.offers = null; }
    }
    public void setOffersFromServer(TradeOfferList offers) { this.offers = offers; }
    public int getExperience() { return this.merchantExperience; }
    public void setExperienceFromServer(int experience) { this.merchantExperience = experience; }
    public boolean isLeveledMerchant() { return false; }
    public SoundEvent getYesSound() { return SoundEvents.ENTITY_WITCH_CELEBRATE; }
    public boolean isClient() { return this.getWorld().isClient(); }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_WITCH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) { return SoundEvents.ENTITY_WITCH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WITCH_DEATH; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        NbtCompound counts = new NbtCompound();
        for (Map.Entry<UUID, Integer> entry : purchaseCounts.entrySet()) counts.putInt(entry.getKey().toString(), entry.getValue());
        nbt.put("PurchaseCounts", counts);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        purchaseCounts.clear();
        if (nbt.contains("PurchaseCounts")) {
            NbtCompound counts = nbt.getCompound("PurchaseCounts");
            for (String key : counts.getKeys()) {
                try { purchaseCounts.put(UUID.fromString(key), counts.getInt(key)); } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}