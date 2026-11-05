package com.rzy.dealt_force_skills.client.screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
/** Shared presentation adapted from the supplied department/TaskScreen reference. */
public final class DepartmentUiStyle {
    public static final int BG=0xFF0E1819,PANEL=0xFF11161A,GROUP=0xFF222830,ROW=0xFF1A1E24,
            BORDER=0x403A5056,TEXT=0xFFE5E9EC,MUTED=0xFF9EACB3,YELLOW=0xFFFFD700;
    private static final ResourceLocation ON=ResourceLocation.fromNamespaceAndPath("deltaforce","textures/gui/main_tabs_on.png"),
            OFF=ResourceLocation.fromNamespaceAndPath("deltaforce","textures/gui/main_tabs_off.png"),BUTTON=ResourceLocation.fromNamespaceAndPath("deltaforce","textures/gui/button_on.png");
    private DepartmentUiStyle() { }
    public static void panel(GuiGraphics g,int x,int y,int w,int h,int color) {
        g.fill(x,y,x+w,y+h,color);g.renderOutline(x,y,w,h,BORDER);
    }
    public static void tab(GuiGraphics g,Font font,int x,int y,int w,Component label,boolean selected) {
        g.blit(selected?ON:OFF,x,y,0,0,w,20,w,20);g.drawCenteredString(font,label,x+w/2,y+6,selected?TEXT:MUTED);
    }
    public static void button(GuiGraphics g,Font font,int x,int y,int w,Component label,boolean hover) {
        g.blit(BUTTON,x,y,0,0,w,22,w,22);if(hover)g.fill(x,y,x+w,y+22,0x20FFFFFF);
        g.drawCenteredString(font,label,x+w/2,y+7,TEXT);
    }
    public static net.minecraft.client.gui.components.Button widget(Component label,int x,int y,int w,Runnable action) {
        return new net.minecraft.client.gui.components.Button(x,y,w,22,label,b->action.run(),supplier->supplier.get()) {
            @Override public void renderWidget(GuiGraphics graphics,int mx,int my,float tick) {
                var font=net.minecraft.client.Minecraft.getInstance().font;
                Component shown=Component.literal(font.plainSubstrByWidth(getMessage().getString(),Math.max(1,getWidth()-8)));
                button(graphics,font,getX(),getY(),getWidth(),shown,active&&isHoveredOrFocused());
                if(!active)graphics.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0x9011161A);
            }
        };
    }
    /** GuiGraphics scissors use GUI coordinates, while embedded pages draw in the shell's transformed canvas. */
    public static void scissor(GuiGraphics g,int x0,int y0,int x1,int y1) {
        var matrix=g.pose().last().pose();
        var a=matrix.transformPosition(new org.joml.Vector3f(x0,y0,0));
        var b=matrix.transformPosition(new org.joml.Vector3f(x1,y1,0));
        g.enableScissor((int)Math.floor(a.x()),(int)Math.floor(a.y()),(int)Math.ceil(b.x()),(int)Math.ceil(b.y()));
    }
    public static void scrollbar(GuiGraphics g,int x,int top,int height,int offset,int max) {
        if(max<=0)return;int thumb=Math.max(16,height*height/(height+max));
        g.fill(x,top,x+5,top+height,0xFF151A1F);int y=top+(int)((height-thumb)*(offset/(double)max));g.fill(x,y,x+5,y+thumb,0x608FA4AE);
    }
}
