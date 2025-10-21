package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.IScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Adds a no-background render helper directly inside Screen.
 * Provides access for modded screens that want to reuse it.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin implements IScreen {

    @Final
    @Shadow private List<Renderable> renderables;

    @Shadow protected Minecraft minecraft;

    // --- Invokers for protected instance methods ---
    @Invoker("renderMenuBackground")
    protected abstract void sereneseasonsplus$invokeRenderMenuBackground(GuiGraphics guiGraphics);

    @Invoker("renderPanorama")
    protected abstract void sereneseasonsplus$invokeRenderPanorama(GuiGraphics guiGraphics, float partialTick);

    /**
     * Renders all widgets without drawing any background.
     */
    @Unique
    public void sereneseasonsplus$renderNoBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        sereneseasonsplus$renderBackground(g, mouseX, mouseY, partialTick);
        for (Renderable renderable : this.renderables) {
            renderable.render(g, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Renders the blurred background + panorama when needed.
     */
    @Unique
    public void sereneseasonsplus$renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft.level == null) {
            // ✅ Just call the invoker normally — no cast needed
            this.sereneseasonsplus$invokeRenderPanorama(guiGraphics, partialTick);
        }

        this.sereneseasonsplus$invokeRenderMenuBackground(guiGraphics);
    }

    @Unique
    protected void sereneseasonsplus$renderBlurredBackground(float partialTick) {
        this.minecraft.gameRenderer.processBlurEffect(partialTick);
        this.minecraft.getMainRenderTarget().bindWrite(false);
    }
}
