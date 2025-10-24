package com.Gabou.sereneseasonsplus.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public class SereneExtendedList extends ObjectSelectionList<SereneExtendedList.Row> {

    /**
     * Creates a scrollable two-column list used by the config screen.
     * The left column renders a label and the right hosts one or more widgets.
     *
     * @param mc         minecraft instance
     * @param width      list width
     * @param height     list height
     * @param top        top y
     * @param itemHeight row height in pixels
     */
    public SereneExtendedList(Minecraft mc, int width, int height, int top, int itemHeight) {
        super(mc, width, height, top, itemHeight);
    }

    /**
     * Width reserved for each row (label + widgets area).
     */
    @Override
    public int getRowWidth() {
        return 360;
    }


    /**
     * Scrollbar x position relative to the left edge of the list.
     */
    @Override
    protected int scrollBarX() {
        return this.getRowLeft() + this.getRowWidth() + 8;
    }
    /**
     * Adds a new row with a label and one or more widgets aligned to the right.
     *
     * @param label   left-hand label
     * @param widgets widgets to render on the right
     */
    public void addRow(Component label, AbstractWidget... widgets) {
        this.addEntry(new Row(this, label, widgets));
    }

    public static class Row extends ObjectSelectionList.Entry<Row> {
        private final SereneExtendedList owner;
        private final Component label;
        private final List<AbstractWidget> widgets;

        private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastRowW = Integer.MIN_VALUE, lastRowH = Integer.MIN_VALUE;

        /**
         * Creates a row bound to the given owner with a static label and one or
         * more right-aligned widgets.
         */
        Row(SereneExtendedList owner, Component label, AbstractWidget... widgets) {
            this.owner = owner;
            this.label = label;
            this.widgets = Arrays.asList(widgets);
        }

        /**
         * Lazily computes and applies widget positions when the row geometry changes.
         */
        private void layoutIfNeeded(int x, int y, int rowWidth, int rowHeight) {
            if (x == lastX && y == lastY && rowWidth == lastRowW && rowHeight == lastRowH) return;
            lastX = x; lastY = y; lastRowW = rowWidth; lastRowH = rowHeight;

            final int wx = x + rowWidth - 200;
            final int wy = y + (rowHeight - 20) / 2;

            for (AbstractWidget w : widgets) {
                if (w.getX() != wx) w.setX(wx);
                if (w.getY() != wy) w.setY(wy);
                if (w.getWidth() != 200) w.setWidth(200);
            }
        }

        /**
         * Renders the row label and delegates rendering to child widgets.
         */
        @Override
        public void render(GuiGraphics g, int index, int y, int x, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            layoutIfNeeded(x, y, rowWidth, rowHeight);

            // Label on the left
            g.drawString(owner.minecraft.font, label, x, y + 6, 0xFFFFFF, false);

            // Render widgets on the right
            for (AbstractWidget w : widgets) {
                w.render(g, mouseX, mouseY, delta);
            }
        }

        /**
         * Narratable entries provided by child widgets.
         */
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }

        /**
         * GUI event listeners provided by child widgets.
         */
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        /**
         * Delegates clicks to child widgets and manages row selection/focus.
         */
        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            for (AbstractWidget w : widgets) {
                if (w.mouseClicked(mx, my, button)) {
                    owner.setSelected(this);
                    w.setFocused(true); // Fabric 1.20.4 still supports this
                    return true;
                }
            }
            owner.setSelected(this);
            return false;
        }

        /**
         * Forwards mouse release to child widgets.
         */
        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            for (AbstractWidget w : widgets) w.mouseReleased(mx, my, button);
            return false;
        }

        /**
         * Forwards drag events to child widgets.
         */
        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            for (AbstractWidget w : widgets) if (w.mouseDragged(mx, my, button, dx, dy)) return true;
            return false;
        }

        /**
         * Forwards scroll events to child widgets.
         */
        @Override
        public boolean mouseScrolled(double mx, double my, double dx, double dy) {
            for (AbstractWidget w : widgets) if (w.mouseScrolled(mx, my, dx, dy)) return true;
            return false;
        }

        /**
         * Forwards key events to child widgets.
         */
        @Override
        public boolean keyPressed(int key, int sc, int mods) {
            for (AbstractWidget w : widgets) if (w.keyPressed(key, sc, mods)) return true;
            return false;
        }

        /**
         * Forwards character typed events to child widgets.
         */
        @Override
        public boolean charTyped(char c, int mods) {
            for (AbstractWidget w : widgets) if (w.charTyped(c, mods)) return true;
            return false;
        }

        /**
         * No-op narration aggregation; child widgets provide their own narration.
         */
        @Override
        public void updateNarration(NarrationElementOutput out) { }

        /**
         * Row narration text, derived from the label.
         */
        public Component getNarration() {
            return label;
        }
    }
}
