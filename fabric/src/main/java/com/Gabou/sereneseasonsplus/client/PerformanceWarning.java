package com.Gabou.sereneseasonsplus.client;

import com.Gabou.sereneseasonsplus.util.IScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


public class PerformanceWarning extends Screen {
    public PerformanceWarning() {
        super(Component.literal("Performance Warning"));
    }

    @Override
    protected void init() {
        String text = "I Understand and I will not bother the devs with performance issues";
        int textWidth = this.font.width(text) + 20; // +20 for padding

        this.addRenderableWidget(
                Button.builder(Component.literal(text),
                                btn -> this.minecraft.setScreen(null))
                        .bounds(this.width / 2 - textWidth / 2, this.height / 2, textWidth, 20)
                        .build()
        );
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font,
                "This mod is optimized to work on the Sodium or Embeddium renderer",
                this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                "No performance mod detected (Ex: Embeddium, Xenon, Chloride, Sodium, etc).",
                this.width / 2, this.height / 2 - 35, 0xFF5555);
        graphics.drawCenteredString(this.font,
                "Install one for better FPS!",
                this.width / 2, this.height / 2 - 25, 0xFF5555);
        ((IScreen)(Object)this).sereneseasonsplus$renderNoBackground(graphics, mouseX, mouseY, partialTick);
    }
}

