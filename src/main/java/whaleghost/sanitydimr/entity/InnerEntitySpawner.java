package whaleghost.sanitydimr.entity;

import whaleghost.sanitydimr.capability.ISanity;
import whaleghost.sanitydimr.capability.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class InnerEntitySpawner
{
    private static final RandomSource RAND = RandomSource.create();

    private static int m_spawnRadius = 20;
    private static int m_detectionRadius = 40;
    private static int m_spawnTimeout = 20 * 20;

    public static int getSpawnRadius() { return m_spawnRadius; }
    public static void setSpawnRadius(int r) { m_spawnRadius = r; }
    public static int getDetectionRadius() { return m_detectionRadius; }
    public static void setDetectionRadius(int r) { m_detectionRadius = r; }
    public static int getSpawnTimeout() { return m_spawnTimeout; }
    public static void setSpawnTimeout(int t) { m_spawnTimeout = t; }

    public static final float SPAWN_THRESHOLD = .75f;
    public static final Map<ServerPlayer, Integer> PLAYER_TO_SPAWN_TIMEOUT = new HashMap<ServerPlayer, Integer>();

    private static int getHeightForSpawning(Level level, BlockPos blockPos, int radius)
    {
        BlockPos.MutableBlockPos mutable = blockPos.mutable();
        for (int i = 0; i < radius; i++)
        {
            if (!level.getBlockState(mutable).isAir() && level.getBlockState(mutable.move(Direction.UP)).isAir())
            {
                return mutable.getY();
            }
        }
        for (int i = 0; i < radius; i++)
        {
            if (level.getBlockState(mutable).isAir() && !level.getBlockState(mutable.move(Direction.DOWN)).isAir())
            {
                return mutable.getY() - 1;
            }
        }
        return 0;
    }

    public static boolean trySpawnForPlayer(ServerPlayer player)
    {
        if (player == null || player.isCreative() || player.isSpectator() || player.level().getDifficulty().equals(Difficulty.PEACEFUL))
            return false;

        PLAYER_TO_SPAWN_TIMEOUT.putIfAbsent(player, 0);
        int t = PLAYER_TO_SPAWN_TIMEOUT.get(player);
        if (t > 0)
        {
            PLAYER_TO_SPAWN_TIMEOUT.put(player, t - 1);
            return false;
        }

        ISanity s = player.getData(Sanity.ATTACHMENT);
        if (s == null)
            return false;
        if (s.getSanity() < SPAWN_THRESHOLD || getInnerEntitiesInRadius(player.level(), player.blockPosition(), m_detectionRadius).size() >= 3)
            return false;

        int index = RAND.nextInt(EntityRegistry.INNER_ENTITIES.size());
        InnerEntity entity = EntityRegistry.INNER_ENTITIES.get(index).get().create(player.level());
        if (entity == null)
            return false;

        BlockPos trialPos = BlockPos.randomBetweenClosed(RAND, 1,
                player.blockPosition().getX() - m_spawnRadius,
                player.blockPosition().getY(),
                player.blockPosition().getZ() - m_spawnRadius,
                player.blockPosition().getX() + m_spawnRadius,
                player.blockPosition().getY(),
                player.blockPosition().getZ() + m_spawnRadius).iterator().next();
        int h = getHeightForSpawning(player.level(), trialPos, m_spawnRadius);

        if (h == 0)
            return false;

        trialPos = new BlockPos(trialPos.getX(), h, trialPos.getZ());
        entity.setPos(new Vec3(trialPos.getX() + .5f, trialPos.getY() + .5f, trialPos.getZ() + .5f));
        if (entity.checkSpawnObstruction(player.level()) &&
                player.level().noCollision(entity) &&
                ((ServerLevel)player.level()).tryAddFreshEntityWithPassengers(entity))
        {
            PLAYER_TO_SPAWN_TIMEOUT.put(player, m_spawnTimeout);
            return true;
        }

        return false;
    }

    public static List<InnerEntity> getInnerEntitiesInRadius(Level level, BlockPos blockPos, int radius)
    {
        return level.getEntitiesOfClass(InnerEntity.class, new AABB(blockPos).inflate(radius));
    }
}