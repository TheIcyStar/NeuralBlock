package dev.starfrost.neuralblock;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Utils {
    public static Vec3 slerp(Vec3 A, Vec3 B, double t) {
        A = A.normalize();
        B = B.normalize();

        // Compute the dot product (clamped to avoid numerical issues)
        double dot = Mth.clamp(A.dot(B), -1.0, 1.0);
        double theta = Math.acos(dot);

        // If the angle is small, fallback to linear interpolation
        if (Math.abs(theta) < 1e-6) {
            return A.scale(1 - t).add(B.scale(t)).normalize();
        }

        double sinTheta = Math.sin(theta);
        double factorA = Math.sin((1 - t) * theta) / sinTheta;
        double factorB = Math.sin(t * theta) / sinTheta;

        return A.scale(factorA).add(B.scale(factorB)).normalize();
    }
}
