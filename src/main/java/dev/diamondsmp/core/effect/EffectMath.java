package dev.diamondsmp.core.effect;

import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EffectMath {

    public static final double TAU = Math.PI * 2.0D;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));

    private EffectMath() {
    }

    public static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    public static double easeOut(double progress) {
        double t = 1.0D - clamp01(progress);
        return 1.0D - t * t * t;
    }

    public static double easeIn(double progress) {
        double t = clamp01(progress);
        return t * t * t;
    }

    public static double easeInOut(double progress) {
        double t = clamp01(progress);
        return t < 0.5D ? 4.0D * t * t * t : 1.0D - Math.pow(-2.0D * t + 2.0D, 3.0D) / 2.0D;
    }

    public static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    public static Vector3f sphere(int index, int total) {
        double y = 1.0D - (index + 0.5D) / total * 2.0D;
        double radius = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        double theta = GOLDEN_ANGLE * index;
        return new Vector3f((float) (Math.cos(theta) * radius), (float) y, (float) (Math.sin(theta) * radius));
    }

    public static Quaternionf spin(double yaw, double pitch, double roll) {
        return new Quaternionf().rotateY((float) yaw).rotateX((float) pitch).rotateZ((float) roll);
    }

    public static Transformation cube(double x, double y, double z, double size, Quaternionf rotation) {
        return box(x, y, z, size, size, size, rotation);
    }

    public static Transformation box(double x, double y, double z, double width, double height, double depth,
                                     Quaternionf rotation) {
        Vector3f half = new Vector3f((float) width / 2.0F, (float) height / 2.0F, (float) depth / 2.0F);
        rotation.transform(half);
        return new Transformation(new Vector3f((float) x - half.x, (float) y - half.y, (float) z - half.z),
                new Quaternionf(rotation), new Vector3f((float) width, (float) height, (float) depth),
                new Quaternionf());
    }

    public static Transformation segment(Vector3f from, Vector3f to, float width) {
        Vector3f direction = new Vector3f(to).sub(from);
        float length = Math.max(0.01F, direction.length());
        Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), direction.normalize());
        Vector3f offset = new Vector3f(width / 2.0F, 0.0F, width / 2.0F);
        rotation.transform(offset);
        return new Transformation(new Vector3f(from).sub(offset), rotation, new Vector3f(width, length, width),
                new Quaternionf());
    }

    public static Transformation item(double x, double y, double z, double scale, Quaternionf rotation) {
        return new Transformation(new Vector3f((float) x, (float) y, (float) z), new Quaternionf(rotation),
                new Vector3f((float) scale), new Quaternionf());
    }
}
