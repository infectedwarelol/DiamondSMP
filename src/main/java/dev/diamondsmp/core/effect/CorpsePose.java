package dev.diamondsmp.core.effect;

import org.bukkit.entity.ArmorStand;
import org.bukkit.util.EulerAngle;

public enum CorpsePose {

    HANGING(new EulerAngle(0.75D, 0.0D, 0.2D), new EulerAngle(0.1D, 0.0D, 0.0D),
            new EulerAngle(-2.6D, 0.0D, -0.35D), new EulerAngle(-2.6D, 0.0D, 0.35D),
            new EulerAngle(0.15D, 0.0D, -0.05D), new EulerAngle(0.05D, 0.0D, 0.05D)),
    SLUMPED(new EulerAngle(0.55D, 0.3D, 0.45D), new EulerAngle(-0.15D, 0.0D, 0.0D),
            new EulerAngle(-0.2D, 0.0D, -0.3D), new EulerAngle(0.3D, 0.0D, 0.35D),
            new EulerAngle(-1.5D, -0.2D, 0.0D), new EulerAngle(-1.45D, 0.25D, 0.0D)),
    KNEELING(new EulerAngle(0.8D, 0.0D, 0.0D), new EulerAngle(0.35D, 0.0D, 0.0D),
            new EulerAngle(-0.5D, 0.0D, -0.15D), new EulerAngle(-0.5D, 0.0D, 0.15D),
            new EulerAngle(1.45D, 0.0D, -0.05D), new EulerAngle(1.45D, 0.0D, 0.05D)),
    STANDING(new EulerAngle(0.6D, 0.0D, 0.15D), new EulerAngle(0.05D, 0.0D, 0.0D),
            new EulerAngle(0.1D, 0.0D, -0.1D), new EulerAngle(0.1D, 0.0D, 0.1D),
            new EulerAngle(0.0D, 0.0D, -0.03D), new EulerAngle(0.0D, 0.0D, 0.03D)),
    FLOATING(new EulerAngle(-0.45D, 0.0D, 0.0D), new EulerAngle(-0.1D, 0.0D, 0.0D),
            new EulerAngle(0.2D, 0.0D, -0.9D), new EulerAngle(0.2D, 0.0D, 0.9D),
            new EulerAngle(0.2D, 0.0D, -0.08D), new EulerAngle(0.2D, 0.0D, 0.08D));

    private final EulerAngle head;
    private final EulerAngle body;
    private final EulerAngle leftArm;
    private final EulerAngle rightArm;
    private final EulerAngle leftLeg;
    private final EulerAngle rightLeg;

    CorpsePose(EulerAngle head, EulerAngle body, EulerAngle leftArm, EulerAngle rightArm, EulerAngle leftLeg,
               EulerAngle rightLeg) {
        this.head = head;
        this.body = body;
        this.leftArm = leftArm;
        this.rightArm = rightArm;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
    }

    public double floorOffset() {
        return switch (this) {
            case SLUMPED -> -0.68D;
            case KNEELING -> -0.55D;
            default -> 0.0D;
        };
    }

    void apply(ArmorStand stand) {
        stand.setHeadPose(head);
        stand.setBodyPose(body);
        stand.setLeftArmPose(leftArm);
        stand.setRightArmPose(rightArm);
        stand.setLeftLegPose(leftLeg);
        stand.setRightLegPose(rightLeg);
    }
}
