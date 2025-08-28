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
  * TODO: describe method.
  *
  * @param px description
  * @return description
  */
 * - Does NOT resize EditBox height (keep 20px)
 * - Forwards mouse/keyboard so EditBox gets focus and can type
 /**
  * TODO: describe method.
  *
  * @param narratables description
  * @return description
  */
 * - Avoids fragile @Override on mapping-variant methods (children/narratables)
 */
public class SereneExtendedList extends ObjectSelectionList<SereneExtendedList.Row> {

    /**
     * Constructs a new instance.
     *
     * @param mc description
     * @param width description
     * @param height description
     * @param top description
     * @param bottom description
     * @param itemHeight description
     */
    public SereneExtendedList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, bottom, itemHeight);
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    /**
     * TODO: describe method.
     * @return description
     */
    @Override
    public int getRowWidth() {
        return 360; 
    }

    /**
     * TODO: describe method.
     * @return description
     */
    @Override
    protected int getScrollbarPosition() {
        return this.getRowLeft() + getRowWidth() + 8;
    }

    /**
     * TODO: describe method.
     *
     * @param label description
     * @param widgets description
     */
    public void addRow(Component label, AbstractWidget... widgets) {
        this.addEntry(new Row(this, label, widgets));
    }

    
    public static class Row extends ObjectSelectionList.Entry<Row> {
        private final SereneExtendedList owner;
        private final Component label;
        private final List<AbstractWidget> widgets;

        
        private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastRowW = Integer.MIN_VALUE, lastRowH = Integer.MIN_VALUE;

        Row(SereneExtendedList owner, Component label, AbstractWidget... widgets) {
            this.owner = owner;
            this.label = label;
            this.widgets = Arrays.asList(widgets);
        }

        /**
         * TODO: describe method.
         *
         * @param x description
         * @param y description
         * @param rowWidth description
         * @param rowHeight description
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
         * TODO: describe method.
         *
         * @param g description
         * @param index description
         * @param y description
         * @param x description
         * @param rowWidth description
         * @param rowHeight description
         * @param mouseX description
         * @param mouseY description
         * @param hovered description
         * @param delta description
         */
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
         * TODO: describe method.
         * @return description
         */
        public List<? extends NarratableEntry> narratables() { return widgets; }
        /**
         * TODO: describe method.
         * @return description
         */
        public List<? extends GuiEventListener> children()    { return widgets; }

        
        /**
         * TODO: describe method.
         *
         * @param mx description
         * @param my description
         * @param button description
         * @return description
         */
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


        /**
         * TODO: describe method.
         *
         * @param mx description
         * @param my description
         * @param button description
         * @return description
         */
        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            for (AbstractWidget w : widgets) w.mouseReleased(mx, my, button);
            return false;
        }

        /**
         * TODO: describe method.
         *
         * @param mx description
         * @param my description
         * @param button description
         * @param dx description
         * @param dy description
         * @return description
         */
        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            for (AbstractWidget w : widgets) if (w.mouseDragged(mx, my, button, dx, dy)) return true;
            return false;
        }

        /**
         * TODO: describe method.
         *
         * @param mx description
         * @param my description
         * @param delta description
         * @return description
         */
        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            for (AbstractWidget w : widgets) if (w.mouseScrolled(mx, my, delta)) return true;
            return false; 
        }

        /**
         * TODO: describe method.
         *
         * @param key description
         * @param sc description
         * @param mods description
         * @return description
         */
        @Override
        public boolean keyPressed(int key, int sc, int mods) {
            for (AbstractWidget w : widgets) if (w.keyPressed(key, sc, mods)) return true;
            return false;
        }

        /**
         * TODO: describe method.
         *
         * @param c description
         * @param mods description
         * @return description
         */
        @Override
        public boolean charTyped(char c, int mods) {
            for (AbstractWidget w : widgets) if (w.charTyped(c, mods)) return true;
            return false;
        }

        
        /**
         * TODO: describe method.
         *
         * @param out description
         */
        @Override public void updateNarration(NarrationElementOutput out) {  }
        public Component getNarration() { return label; }
    }
}
