package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.department.TaskDisplay;
import com.rzy.dealt_force_skills.mail.MailText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MailPreviewScreen extends Screen {
    private final Screen parent;
    private final String subject,body;
    private final TaskDisplay.Rewards rewards;
    private final RewardIcons icons=new RewardIcons();
    private int scroll,maxScroll;
    MailPreviewScreen(Screen parent,String subject,String body,TaskDisplay.Rewards rewards) {
        super(Component.translatable("screen.dealt_force_skills.mail.preview"));this.parent=parent;this.subject=subject;this.body=body;this.rewards=rewards;
    }
    @Override protected void init(){addRenderableWidget(DepartmentUiStyle.widget(Component.translatable("gui.back"),12,height-34,90,this::onClose));}
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        g.fill(0,0,width,height,DepartmentUiStyle.BG);icons.clear();int y=18-scroll;
        DepartmentUiStyle.scissor(g,12,12,width-12,height-42);
        for(var line:font.split(MailText.parse(subject),width-32)){g.drawString(font,line,16,y,DepartmentUiStyle.TEXT,false);y+=14;}y+=12;
        for(var line:font.split(MailText.parse(body),width-32)){g.drawString(font,line,16,y,DepartmentUiStyle.TEXT,false);y+=14;}y+=12;
        y=icons.rewards(g,rewards,16,y,width-32,12,height-42);
        g.disableScissor();maxScroll=Math.max(0,y+scroll-height+48);scroll=Math.min(scroll,maxScroll);
        super.render(g,mx,my,tick);icons.tooltip(g,mx,my);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(delta*26)));return true;}
    @Override public boolean mouseClicked(double x,double y,int button){return icons.click(x,y,button)||super.mouseClicked(x,y,button);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
