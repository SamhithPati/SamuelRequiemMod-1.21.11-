package net.sam.samrequiemmod.entity.renderer;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;

import java.util.Map;

public class SoulBossRenderState extends LivingEntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> geckoData = new Reference2ObjectOpenHashMap<>();
    public int currentActionId;
    public int currentActionTicks;
    public int currentActionDuration;

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckoData;
    }
}
