package net.sam.samrequiemmod.possession;

public class PossessionProfile {

    private final float width;
    private final float height;
    private final float eyeHeight;

    private final double healthBonus;
    private final double speedModifier;

    public PossessionProfile(
            float width,
            float height,
            float eyeHeight,
            double healthBonus,
            double speedModifier
    ) {
        this.width = width;
        this.height = height;
        this.eyeHeight = eyeHeight;
        this.healthBonus = healthBonus;
        this.speedModifier = speedModifier;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getEyeHeight() {
        return eyeHeight;
    }

    public double getHealthBonus() {
        return healthBonus;
    }

    public double getSpeedModifier() {
        return speedModifier;
    }
}





