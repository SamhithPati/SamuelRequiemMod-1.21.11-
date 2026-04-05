package net.sam.samrequiemmod.entity.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.entity.SoulBossEntity;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.BoneSnapshots;
import software.bernie.geckolib.renderer.base.RenderPassInfo;

public class SoulBossRenderer extends GeoEntityRenderer<SoulBossEntity, SoulBossRenderState> {
    public SoulBossRenderer(EntityRendererFactory.Context context) {
        super(context, new SoulBossModel());
        withScale(1.15f);
    }

    @Override
    public SoulBossRenderState createRenderState(SoulBossEntity animatable, Void relatedObject) {
        return new SoulBossRenderState();
    }

    @Override
    public void captureDefaultRenderState(SoulBossEntity animatable, Void relatedObject, SoulBossRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, relatedObject, renderState, partialTick);

        renderState.currentActionId = animatable.getCurrentActionId();
        renderState.currentActionTicks = animatable.getCurrentActionTicks();
        renderState.currentActionDuration = animatable.getCurrentActionDuration();
        populateGeckoData(animatable, renderState, partialTick);
    }

    @Override
    public void addRenderData(SoulBossEntity animatable, Void relatedObject, SoulBossRenderState renderState, float partialTick) {
        populateGeckoData(animatable, renderState, partialTick);
    }

    private void populateGeckoData(SoulBossEntity animatable, SoulBossRenderState renderState, float partialTick) {
        long instanceId = getInstanceId(animatable, null);
        AnimatableManager<?> manager = animatable.getAnimatableInstanceCache().getManagerForId(instanceId);
        boolean moving = animatable.getVelocity().horizontalLengthSquared() >= (double)(getMotionAnimThreshold(animatable) * getMotionAnimThreshold(animatable));

        renderState.getDataMap().put(DataTickets.TICK, (double)animatable.age + partialTick);
        renderState.getDataMap().put(DataTickets.PARTIAL_TICK, partialTick);
        renderState.getDataMap().put(DataTickets.ANIMATABLE_INSTANCE_ID, instanceId);
        renderState.getDataMap().put(DataTickets.ANIMATABLE_MANAGER, manager);
        renderState.getDataMap().put(DataTickets.ANIMATABLE_CLASS, animatable.getClass());
        renderState.getDataMap().put(DataTickets.IS_MOVING, moving);
        renderState.getDataMap().put(DataTickets.VELOCITY, animatable.getVelocity());
        renderState.getDataMap().put(DataTickets.POSITION, new Vec3d(animatable.getX(), animatable.getY(), animatable.getZ()));
        renderState.getDataMap().put(DataTickets.ENTITY_YAW, animatable.getYaw(partialTick));
        renderState.getDataMap().put(DataTickets.ENTITY_BODY_YAW, animatable.bodyYaw);
        renderState.getDataMap().put(DataTickets.ENTITY_PITCH, animatable.getPitch(partialTick));
        renderState.getDataMap().put(DataTickets.IS_DEAD_OR_DYING, animatable.isDead() || animatable.getHealth() <= 0.0f);
        renderState.getDataMap().put(DataTickets.SPRINTING, animatable.isSprinting());
        renderState.getDataMap().put(DataTickets.IS_CROUCHING, animatable.isSneaking());
        renderState.getDataMap().put(DataTickets.PACKED_OVERLAY, getPackedOverlay(animatable, null, 0, partialTick));
        renderState.getDataMap().put(DataTickets.RENDER_COLOR, getRenderColor(animatable, null, partialTick));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<SoulBossRenderState> renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        AnimatableManager<?> manager = renderPassInfo.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return;
        }

        AnimationController<?> mainController = manager.getAnimationControllers().get("main");
        String mainAnimation = mainController != null && mainController.getCurrentRawAnimation() != null
                ? mainController.getCurrentRawAnimation().toString().replace("RawAnimation{", "").replace("}", "")
                : null;
        boolean moving = Boolean.TRUE.equals(renderPassInfo.getOrDefaultGeckolibData(DataTickets.IS_MOVING, false));
        boolean dead = Boolean.TRUE.equals(renderPassInfo.getOrDefaultGeckolibData(DataTickets.IS_DEAD_OR_DYING, false));
        float mainTime = mainController != null ? (float)mainController.getCurrentTimelineTime() : 0.0f;
        SoulBossRenderState renderState = renderPassInfo.renderState();
        int actionId = renderState.currentActionId;
        float actionTime = renderState.currentActionDuration > 0
                ? renderState.currentActionDuration - renderState.currentActionTicks
                : 0.0f;

        if (dead || "death".equals(mainAnimation)) {
            applyDeathPose(snapshots, MathHelper.clamp(mainTime, 0.0f, 2.4f));
            return;
        }

        applyIdleOrWalk(snapshots, moving || "walking".equals(mainAnimation), mainTime);

        if (actionId == 1) {
            applySmashAttack(snapshots, actionTime);
        } else if (actionId == 2) {
            applyShootAttack(snapshots, actionTime);
        } else if (actionId == 3) {
            applyRoar(snapshots, actionTime);
        }

        snapshots.ifPresent("hitbox", snapshot -> snapshot.skipRender(true).skipChildrenRender(true));
    }

    private void applyIdleOrWalk(BoneSnapshots snapshots, boolean moving, float time) {
        if (moving) {
            float swing = MathHelper.sin(time * 1.5f) * 0.5f;
            float bob = MathHelper.sin(time * 3.0f) * 0.35f;

            snapshots.ifPresent("soul_boss", snapshot -> snapshot
                    .setTranslateY(bob)
                    .setRotX(0.05f + MathHelper.sin(time * 1.5f) * 0.03f)
                    .setRotZ(MathHelper.sin(time * 0.75f) * 0.03f));
            snapshots.ifPresent("body", snapshot -> snapshot.setRotX(0.08f + MathHelper.sin(time * 1.5f + 0.5f) * 0.05f));
            snapshots.ifPresent("h_head", snapshot -> snapshot.setRotX(-0.04f + MathHelper.sin(time * 1.5f + 1.0f) * 0.03f));
            snapshots.ifPresent("h_jaw", snapshot -> snapshot.setRotX(0.08f + MathHelper.sin(time * 3.0f + 0.5f) * 0.06f));
            snapshots.ifPresent("right_arm", snapshot -> snapshot.setRotX(-0.35f - swing));
            snapshots.ifPresent("right_arm2", snapshot -> snapshot.setRotX(-0.35f + swing));
            snapshots.ifPresent("right_leg", snapshot -> snapshot.setRotX(0.45f + swing * 0.8f));
            snapshots.ifPresent("left_leg", snapshot -> snapshot.setRotX(0.45f - swing * 0.8f));
            return;
        }

        float bob = MathHelper.sin(time * 1.4f) * 0.22f;
        float armDrift = MathHelper.sin(time * 0.9f) * 0.05f;

        snapshots.ifPresent("soul_boss", snapshot -> snapshot
                .setTranslateY(bob)
                .setRotX(MathHelper.sin(time * 0.7f) * 0.02f)
                .setRotZ(MathHelper.sin(time * 0.4f) * 0.015f));
        snapshots.ifPresent("body", snapshot -> snapshot.setRotX(0.03f + MathHelper.sin(time * 0.8f + 0.2f) * 0.03f));
        snapshots.ifPresent("h_head", snapshot -> snapshot.setRotX(MathHelper.sin(time * 0.8f + 1.2f) * 0.02f));
        snapshots.ifPresent("h_jaw", snapshot -> snapshot.setRotX(0.05f + MathHelper.sin(time * 1.8f + 0.7f) * 0.05f));
        snapshots.ifPresent("right_arm", snapshot -> snapshot.setRotX(-0.18f + armDrift));
        snapshots.ifPresent("right_arm2", snapshot -> snapshot.setRotX(-0.18f - armDrift));
        snapshots.ifPresent("right_leg", snapshot -> snapshot.setRotX(0.25f));
        snapshots.ifPresent("left_leg", snapshot -> snapshot.setRotX(0.25f));
    }

    private void applySmashAttack(BoneSnapshots snapshots, float time) {
        float charge = MathHelper.clamp(time / 0.55f, 0.0f, 1.0f);
        float slam = MathHelper.clamp((time - 0.55f) / 0.45f, 0.0f, 1.0f);
        float armRaise = charge * 1.9f - slam * 2.3f;

        snapshots.ifPresent("soul_boss", snapshot -> snapshot
                .setTranslateY(charge * 1.4f - slam * 0.9f)
                .setRotX(-charge * 0.2f + slam * 0.95f));
        snapshots.ifPresent("body", snapshot -> snapshot.setRotX(-charge * 0.25f + slam * 0.6f));
        snapshots.ifPresent("h_head", snapshot -> snapshot.setRotX(charge * 0.18f - slam * 0.35f));
        snapshots.ifPresent("h_jaw", snapshot -> snapshot.setRotX(0.12f + charge * 0.18f + slam * 0.12f));
        snapshots.ifPresent("right_arm", snapshot -> snapshot.setRotX(-0.2f - armRaise).setRotZ(-charge * 0.2f));
        snapshots.ifPresent("right_arm2", snapshot -> snapshot.setRotX(-0.2f - armRaise).setRotZ(charge * 0.2f));
    }

    private void applyShootAttack(BoneSnapshots snapshots, float time) {
        float charge = MathHelper.clamp(time / 0.7f, 0.0f, 1.0f);
        float pulse = MathHelper.sin(time * 10.0f) * 0.04f * charge;

        snapshots.ifPresent("soul_boss", snapshot -> snapshot.setTranslateY(charge * 0.55f + pulse));
        snapshots.ifPresent("body", snapshot -> snapshot.setRotX(-charge * 0.16f));
        snapshots.ifPresent("h_head", snapshot -> snapshot.setRotX(-charge * 0.22f));
        snapshots.ifPresent("h_jaw", snapshot -> snapshot.setRotX(0.08f + charge * 0.35f));
        snapshots.ifPresent("right_arm", snapshot -> snapshot.setRotX(-0.7f - charge * 0.55f).setRotZ(-0.18f));
        snapshots.ifPresent("right_arm2", snapshot -> snapshot.setRotX(-0.7f - charge * 0.55f).setRotZ(0.18f));
    }

    private void applyRoar(BoneSnapshots snapshots, float time) {
        float rise = MathHelper.clamp(time / 0.45f, 0.0f, 1.0f);
        float tremor = MathHelper.sin(time * 12.0f) * 0.02f * rise;

        snapshots.ifPresent("soul_boss", snapshot -> snapshot
                .setTranslateY(1.0f * rise + tremor)
                .setRotX(-0.12f * rise));
        snapshots.ifPresent("body", snapshot -> snapshot.setRotX(-0.22f * rise));
        snapshots.ifPresent("h_head", snapshot -> snapshot.setRotX(-0.35f * rise));
        snapshots.ifPresent("h_jaw", snapshot -> snapshot.setRotX(0.75f * rise));
        snapshots.ifPresent("right_arm", snapshot -> snapshot.setRotX(-1.15f * rise).setRotZ(-0.28f * rise));
        snapshots.ifPresent("right_arm2", snapshot -> snapshot.setRotX(-1.15f * rise).setRotZ(0.28f * rise));
    }

    private void applyDeathPose(BoneSnapshots snapshots, float time) {
        float progress = MathHelper.clamp(time / 1.8f, 0.0f, 1.0f);

        snapshots.ifPresent("soul_boss", snapshot -> snapshot
                .setTranslateY(-progress * 1.8f)
                .setRotX(progress * 1.35f)
                .setRotZ(progress * 0.15f));
        snapshots.ifPresent("body", snapshot -> snapshot.setRotX(progress * 0.45f));
        snapshots.ifPresent("h_head", snapshot -> snapshot.setRotX(-progress * 0.22f));
        snapshots.ifPresent("h_jaw", snapshot -> snapshot.setRotX(0.2f + progress * 0.35f));
        snapshots.ifPresent("right_arm", snapshot -> snapshot.setRotX(-0.45f + progress * 1.2f).setRotZ(-progress * 0.35f));
        snapshots.ifPresent("right_arm2", snapshot -> snapshot.setRotX(-0.45f + progress * 1.2f).setRotZ(progress * 0.35f));
    }
}
