package com.Gabou.sereneseasonsplus.client.config;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Simple in-game configuration screen for Project Atmosphere.
 */
public class SereneExtendedScreen extends Screen {
    private final Screen parent;
    // Snow feature toggle and settings
    private boolean snowFeatureEnabled;
    private int tickSnowReplacerThreshold;
    private int maxSnowHeight;

    private EditBox maxReplacerBox;
    private EditBox maxSnowHeightBox;

    private EditBox dayLengthBox;

    private EditBox nightLengthBox;

    private boolean seasonalDaylightCycle;

    private boolean betterDaysDynamicTimeCompat;

    private boolean customDayCycle;

    private double customDayLength;

    private double customNightLength;

    private boolean grassFlowersEnabled;

    private Component replacerLabel = Component.literal("Common Feature Threshold:");
    private Component snowHeightLabel = Component.literal("Max Snow Height (layers):");
    private Component nightLabel = Component.literal("Custom Night Speed:");
    private Component dayLabel = Component.literal("Custom Day Speed:");

    private SereneExtendedList list;



    /**
     * Constructs the Serene Seasons Plus configuration screen.
     *
     * @param parent screen to return to when closed
     */
    public SereneExtendedScreen(Screen parent) {
        super(Component.literal("Serene Seasons Plus Config"));
        this.parent = parent;
    }

    /**
     * Initializes UI widgets and populates them from current config values.
     */
    @Override
    protected void init() {

        this.snowFeatureEnabled = SereneExtendedConfig.SNOWSTORM_ENABLED.get();
        this.tickSnowReplacerThreshold = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        this.maxSnowHeight = SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get();
        this.seasonalDaylightCycle = SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get();
        this.betterDaysDynamicTimeCompat = SereneExtendedConfig.ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.get();
        this.customDayCycle = SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get();
        this.customDayLength = SereneExtendedConfig.CUSTOM_DAY_LENGTH.get();
        this.customNightLength = SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get();
        this.grassFlowersEnabled = SereneExtendedConfig.GRASSFLOWER_GROWTH_ENABLED.get();

        int panelW = 420;
        int panelX = (this.width - panelW) / 2;
        int top = 40;
        int bottom = this.height - 40;


        this.list = new SereneExtendedList(this.minecraft, panelW, this.height, top + 20, 24);
        try {
            this.list.getClass().getMethod("setX", int.class).invoke(this.list, panelX);
        } catch (Throwable t) {

        }
        this.addRenderableWidget(this.list);



        // Snow feature toggle
        var snowFeatureBtn = Button.builder(toggleLabel("Snow Features", snowFeatureEnabled), b -> {
            snowFeatureEnabled = !snowFeatureEnabled;
            b.setMessage(toggleLabel("Snow Features", snowFeatureEnabled));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Snow Features"), snowFeatureBtn);

        var seasonBtn = Button.builder(toggleLabel("Seasonal Daylight Cycle", seasonalDaylightCycle), b -> {
            seasonalDaylightCycle = !seasonalDaylightCycle;
            customDayCycle = false;
            b.setMessage(toggleLabel("Seasonal Daylight Cycle", seasonalDaylightCycle));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Seasonal Daylight Cycle"), seasonBtn);

        var betterDaysCompatBtn = Button.builder(toggleLabel("Better Days Time Compat", betterDaysDynamicTimeCompat), b -> {
            betterDaysDynamicTimeCompat = !betterDaysDynamicTimeCompat;
            b.setMessage(toggleLabel("Better Days Time Compat", betterDaysDynamicTimeCompat));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Better Days Time Compat"), betterDaysCompatBtn);

        var customBtn = Button.builder(toggleLabel("Custom Daylight Cycle", customDayCycle), b -> {
            customDayCycle = !customDayCycle;
            seasonalDaylightCycle = false;
            b.setMessage(toggleLabel("Custom Daylight Cycle", customDayCycle));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Custom Daylight Cycle"), customBtn);

        var grassFlowersBtn = Button.builder(toggleLabel("Grass and Flower Growth", grassFlowersEnabled), b -> {
            grassFlowersEnabled = !grassFlowersEnabled;
            b.setMessage(toggleLabel("Grass and Flower Growth", grassFlowersEnabled));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Grass and Flower Growth"), grassFlowersBtn);


        this.maxReplacerBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.maxReplacerBox.setValue(Integer.toString(tickSnowReplacerThreshold));
        this.list.addRow(Component.literal("Common Feature Threshold"), this.maxReplacerBox);

        // Max snow height (layers)
        this.maxSnowHeightBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.maxSnowHeightBox.setValue(Integer.toString(maxSnowHeight));
        this.list.addRow(Component.literal("Max Snow Height (layers)"), this.maxSnowHeightBox);

        // Removed pillar threshold in favor of common feature threshold

        this.nightLengthBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.nightLengthBox.setValue(Double.toString(customNightLength));
        this.list.addRow(Component.literal("Custom Night Speed"), this.nightLengthBox);

        this.dayLengthBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.dayLengthBox.setValue(Double.toString(customDayLength));
        this.list.addRow(Component.literal("Custom Day Speed"), this.dayLengthBox);


        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), b -> {
                    saveChanges();
                    this.minecraft.setScreen(parent);
                }).bounds(panelX + (panelW - 200) / 2, bottom - 30, 200, 20).build()
        );
    }

    /**
     * Renders the screen background, title, and child widgets.
     *
     * @param g           GUI graphics context
     * @param mouseX      mouse x
     * @param mouseY      mouse y
     * @param partialTick partial tick time
     */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        int panelW = 480;
        int panelX = (this.width - panelW) / 2;
        int top = 40;
        int bottom = this.height - 40;
        g.fill(panelX - 4, top - 4, panelX + panelW + 4, bottom, 0xAA000000);
        g.drawString(this.font, "Serene Seasons Plus", panelX + 6, top - 14, 0xFFFFFF, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    /**
     * Builds a simple ON/OFF label for a toggleable option.
     *
     * @param name    option label
     * @param enabled current state
     * @return the composed label component
     */
    private Component toggleLabel(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    /**
     * Reads field values, validates and persists them to the COMMON config.
     */
    private void saveChanges() {
        Component errorMessage;
        int parsed2 = this.tickSnowReplacerThreshold;
        int parsedSnowHeight = this.maxSnowHeight;
        double parsed3 = this.customDayLength;
        double parsed4 = this.customNightLength;
        boolean parsed5 = this.grassFlowersEnabled;

        try {
            parsed2 = Integer.parseInt(this.maxReplacerBox.getValue());
            parsedSnowHeight = Integer.parseInt(this.maxSnowHeightBox.getValue());
            errorMessage = null;
        } catch (NumberFormatException ignored) {
            errorMessage = Component.literal("Invalid number for a Snow setting.");
        }

        try {
            parsed3 = Double.parseDouble(this.dayLengthBox.getValue());
            parsed4 = Double.parseDouble(this.nightLengthBox.getValue());
            errorMessage = null;
        } catch (NumberFormatException ignored) {
            errorMessage = Component.literal("Invalid number for one of the DayCycle Speeds.");
        }
        SereneExtendedConfig.TICK_SNOW_REPLACER.set(parsed2);
        SereneExtendedConfig.SNOWSTORM_ENABLED.set(snowFeatureEnabled);
        SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.set(parsedSnowHeight);
        SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.set(seasonalDaylightCycle);
        SereneExtendedConfig.ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.set(betterDaysDynamicTimeCompat);
        SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.set(customDayCycle);
        SereneExtendedConfig.CUSTOM_DAY_LENGTH.set(parsed3);
        SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.set(parsed4);
        SereneExtendedConfig.GRASSFLOWER_GROWTH_ENABLED.set(parsed5);

        // Persist to Fabric config JSON
        SereneExtendedConfig.save();
    }


    /**
     * Restores focus and returns to the parent screen.
     */
    @Override
    public void onClose() {
        this.setFocused(false);
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Forwards click events to the list and its child widgets.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.list != null && this.list.mouseClicked(event, doubleClick)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * Forwards key events to the list and focused text boxes.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.list != null && this.list.keyPressed(event)) return true;


        if (this.maxReplacerBox != null && this.maxReplacerBox.keyPressed(event)) return true;
        if (this.maxSnowHeightBox   != null && this.maxSnowHeightBox.keyPressed(event)) return true;
        if (this.nightLengthBox != null && this.nightLengthBox.keyPressed(event)) return true;
        if (this.dayLengthBox   != null && this.dayLengthBox.keyPressed(event)) return true;

        return super.keyPressed(event);
    }

    /**
     * Forwards typed characters to the list and text boxes.
     */
    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.list != null && this.list.charTyped(event)) return true;

        if (this.maxReplacerBox != null && this.maxReplacerBox.charTyped(event)) return true;
        if (this.maxSnowHeightBox   != null && this.maxSnowHeightBox.charTyped(event)) return true;
        if (this.nightLengthBox != null && this.nightLengthBox.charTyped(event)) return true;
        if (this.dayLengthBox   != null && this.dayLengthBox.charTyped(event)) return true;

        return super.charTyped(event);
    }


}

