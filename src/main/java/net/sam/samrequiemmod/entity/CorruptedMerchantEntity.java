package net.sam.samrequiemmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
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
        this.setCustomName(Text.translatable("entity.samrequiemmod.corrupted_merchant"));
        this.setCustomNameVisible(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
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
        if (this.getEntityWorld().isClient()) {
            return ActionResult.SUCCESS;
        }

        int bought = this.purchaseCounts.getOrDefault(player.getUuid(), 0);
        if (bought >= MAX_PURCHASES_PER_PLAYER) {
            player.sendMessage(Text.literal("I have nothing more to offer you...").formatted(Formatting.DARK_PURPLE), true);
            return ActionResult.SUCCESS;
        }

        this.setCustomer(player);
        this.sendOffers(player, this.getDisplayName(), 1);
        return ActionResult.SUCCESS;
    }

    @Override
    public void setCustomer(PlayerEntity player) {
        this.customer = player;
    }

    @Override
    public PlayerEntity getCustomer() {
        return this.customer;
    }

    @Override
    public TradeOfferList getOffers() {
        if (this.offers == null) {
            this.offers = new TradeOfferList();
            int bought = this.customer != null ? this.purchaseCounts.getOrDefault(this.customer.getUuid(), 0) : 0;
            if (bought < 1) {
                this.offers.add(new TradeOffer(
                        new TradedItem(Items.DIAMOND, 5),
                        java.util.Optional.of(new TradedItem(Items.EMERALD, 4)),
                        new ItemStack(ModItems.POSSESSION_RELIC_SHARD, 1),
                        1, 0, 0.0F
                ));
            }
            if (bought < 2) {
                this.offers.add(new TradeOffer(
                        new TradedItem(Items.DIAMOND, 10),
                        java.util.Optional.of(new TradedItem(Items.EMERALD, 5)),
                        new ItemStack(ModItems.POSSESSION_RELIC_SHARD, 1),
                        1, 0, 0.0F
                ));
            }
        }
        return this.offers;
    }

    @Override
    public void setOffersFromServer(TradeOfferList offers) {
        this.offers = offers;
    }

    @Override
    public void trade(TradeOffer offer) {
        this.afterUsing(offer);
    }

    @Override
    public void onSellingItem(ItemStack stack) {
    }

    public void afterUsing(TradeOffer offer) {
        if (this.customer != null) {
            this.purchaseCounts.merge(this.customer.getUuid(), 1, Integer::sum);
            this.offers = null;
        }
    }

    @Override
    public int getExperience() {
        return this.merchantExperience;
    }

    @Override
    public void setExperienceFromServer(int experience) {
        this.merchantExperience = experience;
    }

    @Override
    public boolean isLeveledMerchant() {
        return false;
    }

    @Override
    public SoundEvent getYesSound() {
        return SoundEvents.ENTITY_WITCH_CELEBRATE;
    }

    @Override
    public boolean isClient() {
        return this.getEntityWorld().isClient();
    }

    @Override
    public boolean canInteract(PlayerEntity player) {
        return this.isAlive() && player.isAlive();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITCH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITCH_DEATH;
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putInt("MerchantExperience", this.merchantExperience);
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        this.merchantExperience = view.getInt("MerchantExperience", 0);
    }
}





