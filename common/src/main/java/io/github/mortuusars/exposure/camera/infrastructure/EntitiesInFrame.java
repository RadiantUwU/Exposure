package io.github.mortuusars.exposure.camera.infrastructure;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.util.Fov;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EntitiesInFrame {
    public static List<Entity> get(Player player, double fov, int limit) {
        double currentFov = fov / Exposure.CROP_FACTOR;
        double currentFocalLength = Fov.fovToFocalLength(currentFov);

        List<Entity> entities = player.getLevel().getEntities(player, new AABB(player.blockPosition()).inflate(128),
                entity -> entity instanceof LivingEntity);
        entities.sort((entity, entity2) -> {
            float dist1 = player.distanceTo(entity);
            float dist2 = player.distanceTo(entity2);
            if (dist1 == dist2) return 0;
            return dist1 > dist2 ? 1 : -1;
        });

        List<Entity> entitiesInFrame = new ArrayList<>();

        for (Entity entity : entities) {
            if (entitiesInFrame.size() >= limit)
                break;

            // Valid angles form a circle instead of square.
            // Due to this, entities in the corners of a frame are not considered "in frame".
            // I'm too dumb at maths to fix this.
            double relativeAngleDegrees = getRelativeAngle(player, entity);
            if (relativeAngleDegrees > currentFov / 2f)
                continue; // Not in frame

            double distanceInBlocks = Math.sqrt(player.distanceToSqr(entity));

            AABB boundingBox = entity.getBoundingBoxForCulling();
            double size = boundingBox.getSize();
            if (Double.isNaN(size) || size == 0.0)
                size = 0.1;

            double sizeModifier = (size - 1.0) * 0.6 + 1.0;
            double modifiedDistance = (distanceInBlocks / sizeModifier) / Exposure.CROP_FACTOR;

            if (modifiedDistance > currentFocalLength)
                continue; // Too far to be in frame

            if (!hasLineOfSight(player, entity))
                continue; // Not visible

            entitiesInFrame.add(entity);
        }

        return entitiesInFrame;
    }

    public static double getRelativeAngle(LivingEntity origin, Entity target) {
        Vec3 lookAngle = origin.getLookAngle();
        Vec3 originEyePos = origin.position().add(0, origin.getEyeHeight(), 0);
        Vec3 targetEyePos = target.position().add(0, target.getEyeHeight(), 0);
        Vec3 originToTargetAngle = targetEyePos.subtract(originEyePos).normalize();
        return Math.toDegrees(Math.acos(lookAngle.dot(originToTargetAngle)));
    }

    public static boolean hasLineOfSight(LivingEntity origin, Entity target) {
        boolean visible = origin.hasLineOfSight(target);

//        Vec3 originEyePos = new Vec3(origin.getX(), origin.getEyeY(), origin.getZ());
//        origin.level.clip(new ClipContext(originEyePos, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;

        return visible;
    }
}
