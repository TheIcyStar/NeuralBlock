package dev.starfrost.neuralblock;

import org.joml.Vector4f;
import org.slf4j.Logger;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NeuralBlock.MODID)
public class NeuralBlock {
    public static final String MODID = "neuralblock";
    private static final Logger LOGGER = LogUtils.getLogger();

    private Minecraft mcInstance;
    private Window mcWindow;

    private int ticks = 0;
    private static final int TICKS_TO_WAIT = 40;
    private static final int MAX_RAYCAST_DISTANCE = 64;
    private static final float DEG_TO_RAD = Mth.PI/180;
    private static final float RAD_TO_DEG = 180/Mth.PI;


    public NeuralBlock(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        mcInstance = Minecraft.getInstance();
        mcWindow = mcInstance.getWindow();
    }

    @SubscribeEvent
    public void handleRenderFrame(PlayerTickEvent.Post event){
        ticks++;
        if(ticks % TICKS_TO_WAIT != 0){
            return;
        }
        if(mcInstance.player == null){
            return;
        }

        double fov = mcInstance.options.fov().get();

        getBlockFromScreenPos(
            mcInstance.level,
            mcInstance.player,
            new Vec2(-1f, 0f),
            fov,
            2*Math.atan(Math.tan(fov*DEG_TO_RAD/2)*mcWindow.getHeight()/mcWindow.getWidth())*RAD_TO_DEG, //https://github.com/themetalmuncher/fov-calc/blob/gh-pages/index.html#L22
            MAX_RAYCAST_DISTANCE
        );

        getBlockFromScreenPos(
            mcInstance.level,
            mcInstance.player,
            new Vec2(0, 0),
            fov,
            2*Math.atan(Math.tan(fov*DEG_TO_RAD/2)*mcWindow.getHeight()/mcWindow.getWidth())*RAD_TO_DEG, //https://github.com/themetalmuncher/fov-calc/blob/gh-pages/index.html#L22
            MAX_RAYCAST_DISTANCE
        );

        getBlockFromScreenPos(
            mcInstance.level,
            mcInstance.player,
            new Vec2(1f, 0f),
            fov,
            2*Math.atan(Math.tan(fov*DEG_TO_RAD/2)*mcWindow.getHeight()/mcWindow.getWidth())*RAD_TO_DEG, //https://github.com/themetalmuncher/fov-calc/blob/gh-pages/index.html#L22
            MAX_RAYCAST_DISTANCE
        );

    }

    /**
     *
     * @param normalizedScreenCoords Vec2 with a range of [-1,1]
     */
    public static Block getBlockFromScreenPos(Level level, Player player, Vec2 normalizedScreenCoords, double fov_h, double fov_v, int maxDistance){
        normalizedScreenCoords = normalizedScreenCoords.scale(0.5f);
        Vec2 viewAngles = new Vec2(
            player.getXRot() + (float)(normalizedScreenCoords.x * fov_h),
            player.getYRot() + (float)(normalizedScreenCoords.y * fov_v)
        );

        Vec3 startPos = player.getEyePosition(1.0F);
        Vec3 toPosDirection = new Vec3(
            Mth.sin(-viewAngles.y * DEG_TO_RAD - Mth.PI) * -Mth.cos(-viewAngles.x * DEG_TO_RAD),
            Mth.sin(-viewAngles.x * DEG_TO_RAD),
            Mth.cos(-viewAngles.y * DEG_TO_RAD - Mth.PI) * -Mth.cos(-viewAngles.x * DEG_TO_RAD)
        );
        Vec3 toPos = startPos.add(toPosDirection.scale(maxDistance));

        ClipContext clipContext = new ClipContext(startPos, toPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult hitResult = level.clip(clipContext);
        BlockState blockState = level.getBlockState(hitResult.getBlockPos());

        LOGGER.info("("+hitResult.getBlockPos()+"): "+BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));

        return blockState.getBlock();
    }


    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
