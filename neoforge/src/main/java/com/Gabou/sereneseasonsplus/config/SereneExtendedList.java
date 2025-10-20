package com.Gabou.sereneseasonsplus.config;

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

/**
 * Scrollable settings list: label on the left + one or more widgets on the right.
 * - Does NOT resize EditBox height (keep 20px)
 * - Forwards mouse/keyboard so EditBox gets focus and can type
 * - Avoids fragile @Override on mapping-variant methods (children/narratables)
 */
public class SereneExtendedList extends ObjectSelectionList<SereneExtendedList.Row> {
    /**
     * Creates a scrollable list for config rows.
     *
     * @param mc         minecraft instance
     * @param width      list width
     * @param height     list height
     * @param top        top y position
     * @param itemHeight height of each row
     */
    public SereneExtendedList(Minecraft mc, int width, int height, int top, int itemHeight) {
        super(mc, width, height, top, itemHeight);
    }

    @Override
    /**
     * Fixed content width for each row.
     */
    public int getRowWidth() {
        return 360;
    }

    @Override
    /**
     * Scrollbar position aligned to the right of the content.
     */
    protected int getScrollbarPosition() {
        return this.getRowLeft() + getRowWidth() + 8;
    }

    /**
     * Adds a new row containing the label and provided widgets.
     *
     * @param label   row label
     * @param widgets widgets to render and interact with
     */
    public void addRow(Component label, AbstractWidget... widgets) {
        this.addEntry(new Row(this, label, widgets));
    }

    // ──────────────────────────────────────────────────────────────────────────
    public static class Row extends Entry<Row> {
        private final SereneExtendedList owner;
        private final Component label;
        private final List<AbstractWidget> widgets;

        private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastRowW = Integer.MIN_VALUE, lastRowH = Integer.MIN_VALUE;

        /**
         * Creates a row.
         *
         * @param owner   parent list
         * @param label   row label
         * @param widgets widgets in the row
         */
        Row(SereneExtendedList owner, Component label, AbstractWidget... widgets) {
            this.owner = owner;
            this.label = label;
            this.widgets = Arrays.asList(widgets);
        }

        /**
         * Lays out widgets only when coordinates or row dimensions change.
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

        @Override
        public void render(GuiGraphics g, int index, int y, int x, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            layoutIfNeeded(x, y, rowWidth, rowHeight);

            g.drawString(owner.minecraft.font, label, x, y + 6, 0xFFFFFF, false);

            for (AbstractWidget w : widgets) {
                w.render(g, mouseX, mouseY, delta);
            }
        }

        
        /**
         * Returns the narratable entries for accessibility.
         */
        public List<? extends NarratableEntry> narratables() { return widgets; }
        /**
         * Returns the children listeners of this row.
         */
        public List<? extends GuiEventListener> children()    { return widgets; }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            for (AbstractWidget w : widgets) {
                if (w.mouseClicked(mx, my, button)) {
                    owner.setSelected(this);
                    w.setFocused(true);
                    return true;
                }
            }
            owner.setSelected(this);
            return false;
        }


        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            for (AbstractWidget w : widgets) w.mouseReleased(mx, my, button);
            return false;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            for (AbstractWidget w : widgets) if (w.mouseDragged(mx, my, button, dx, dy)) return true;
            return false;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double deltaX, double deltaY) {
            for (AbstractWidget w : widgets) if (w.mouseScrolled(mx, my, deltaX, deltaY)) return true;
            return false;
        }

        @Override
        public boolean keyPressed(int key, int sc, int mods) {
            for (AbstractWidget w : widgets) if (w.keyPressed(key, sc, mods)) return true;
            return false;
        }

        @Override
        public boolean charTyped(char c, int mods) {
            for (AbstractWidget w : widgets) if (w.charTyped(c, mods)) return true;
            return false;
        }

        @Override public void updateNarration(NarrationElementOutput out) { /* no-op */ }
        public Component getNarration() { return label; }
    }
}
