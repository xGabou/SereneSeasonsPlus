package com.Gabou.sereneseasonsplus.client;

import com.Gabou.sereneseasonsplus.config.ConfigResetManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class ConfigResetWarning extends Screen {
    private final Path configDirectory;
    private final String backupFileName;

    public ConfigResetWarning(Path configDirectory) {
        super(Component.literal("Serene Seasons Plus Config Reset"));
        this.configDirectory = configDirectory;
        this.backupFileName = ConfigResetManager.getBackupFileName(configDirectory);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("I Understand"), button -> closeWarning())
                .bounds(this.width / 2 - 75, this.height / 2 + 35, 150, 20).build());
    }

    private void closeWarning() {
        ConfigResetManager.markWarningShown(configDirectory);
        this.minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        closeWarning();
    }

    protected void renderBlurredBackground(GuiGraphics graphics) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font,
                "This update renamed the configuration settings, so the active config was reset.",
                this.width / 2, this.height / 2 - 45, 0xFFFF55);
        graphics.drawCenteredString(this.font,
                "Your previous config was saved and will not be used by the mod:",
                this.width / 2, this.height / 2 - 25, 0xFFFFFF);
        graphics.drawCenteredString(this.font, backupFileName,
                this.width / 2, this.height / 2 - 10, 0x55FFFF);
        graphics.drawCenteredString(this.font,
                "Review the new config and re-enter any settings you still want.",
                this.width / 2, this.height / 2 + 10, 0xFFFFFF);
    }
}
