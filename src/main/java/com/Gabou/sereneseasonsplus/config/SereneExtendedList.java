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

    public SereneExtendedList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, bottom, itemHeight);
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    @Override
    public int getRowWidth() {
        return 360; // content width (label + 200px control)
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRowLeft() + getRowWidth() + 8;
    }

    public void addRow(Component label, AbstractWidget... widgets) {
        this.addEntry(new Row(this, label, widgets));
    }

    // ──────────────────────────────────────────────────────────────────────────
    public static class Row extends ObjectSelectionList.Entry<Row> {
        private final SereneExtendedList owner;
        private final Component label;
        private final List<AbstractWidget> widgets;

        // cache to avoid relaying out every frame when coords unchanged
        private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastRowW = Integer.MIN_VALUE, lastRowH = Integer.MIN_VALUE;

        Row(SereneExtendedList owner, Component label, AbstractWidget... widgets) {
            this.owner = owner;
            this.label = label;
            this.widgets = Arrays.asList(widgets);
        }

        private void layoutIfNeeded(int x, int y, int rowWidth, int rowHeight) {
            if (x == lastX && y == lastY && rowWidth == lastRowW && rowHeight == lastRowH) return;
            lastX = x; lastY = y; lastRowW = rowWidth; lastRowH = rowHeight;

            final int wx = x + rowWidth - 200;          // right-align 200px control
            final int wy = y + (rowHeight - 20) / 2;    // center vertically (EditBox is 20px)

            for (AbstractWidget w : widgets) {
                if (w.getX() != wx) w.setX(wx);
                if (w.getY() != wy) w.setY(wy);
                if (w.getWidth() != 200) w.setWidth(200);
                // DO NOT change height here; EditBox must remain 20px
            }
        }

        @Override
        public void render(GuiGraphics g, int index, int y, int x, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            layoutIfNeeded(x, y, rowWidth, rowHeight);

            // label
            g.drawString(owner.minecraft.font, label, x, y + 6, 0xFFFFFF, false);

            // controls
            for (AbstractWidget w : widgets) {
                w.render(g, mouseX, mouseY, delta);
            }
        }

        // mapping-safe: don't @Override these; names vary but the list uses them
        public List<? extends NarratableEntry> narratables() { return widgets; }
        public List<? extends GuiEventListener> children()    { return widgets; }

        // Focus + input forwarding so EditBox can be edited
        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            for (AbstractWidget w : widgets) {
                if (w.mouseClicked(mx, my, button)) {
                    // select this row for the list (keeps the list happy)
                    owner.setSelected(this);

                    // give GUI focus to the clicked widget
                    w.setFocused(true);
                    return true;
                }
            }
            // click on empty row area just selects the row
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
        public boolean mouseScrolled(double mx, double my, double delta) {
            for (AbstractWidget w : widgets) if (w.mouseScrolled(mx, my, delta)) return true;
            return false; // let the list handle scrolling otherwise
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

        // minimal narration to satisfy variants of the API
        @Override public void updateNarration(NarrationElementOutput out) { /* no-op */ }
        public Component getNarration() { return label; }
    }
}
