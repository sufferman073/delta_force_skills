package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.mail.MailboxManager;
import com.rzy.dealt_force_skills.client.ClientMailboxState;
import com.rzy.dealt_force_skills.network.C2S_MailboxAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import com.rzy.dealt_force_skills.mail.MailText;
import com.rzy.dealt_force_skills.department.TaskDisplay;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.UUID;

public final class MailboxScreen extends Screen {
    private final Screen parent;
    private MailboxManager.Page snapshot=new MailboxManager.Page(0,1,List.of());
    private UUID selected;
    private boolean composing;
    private int scroll,maxScroll,refresh;
    private EditBox recipient,subject,coins,experience;
    private MultiLineEditBox body;
    private final List<ItemStack> attachments=new ArrayList<>();
    private final RewardIcons icons=new RewardIcons();
    private boolean formatSubject;
    public MailboxScreen(Screen parent){super(Component.translatable("screen.dealt_force_skills.mail.title"));this.parent=parent;}
    private boolean embedded(){return parent instanceof DFGUI_MAIN;}
    private int top(){return embedded()?78:44;}
    private int divider(){return Math.max(180,width/3);}
    @Override protected void init(){widgets();NetworkHandler.sendToServer(C2S_MailboxAction.open(snapshot.page()));}
    public static void accept(MailboxManager.Page value) {
        ClientMailboxState.update(value);Screen current=Minecraft.getInstance().screen;
        MailboxScreen screen=current instanceof MailboxScreen mail?mail:current instanceof DFGUI_MAIN main?main.embeddedPage(MailboxScreen.class):null;
        if(screen==null)return;screen.snapshot=value;
        if(screen.selected==null||value.entries().stream().noneMatch(e->e.id().equals(screen.selected)))screen.selected=value.entries().isEmpty()?null:value.entries().get(0).id();
        if(!screen.composing)screen.widgets();
    }
    private Component tr(String key){return Component.translatable("screen.dealt_force_skills.mail."+key);}
    private Button button(Component label,int x,int y,int w,Runnable action){return addRenderableWidget(DepartmentUiStyle.widget(label,x,y,w,action));}
    private Button mailRow(Component label,int x,int y,int w,boolean selected,Runnable action) {
        return addRenderableWidget(new Button(x,y,w,22,label,b->action.run(),supplier->supplier.get()) {
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float tick) {
                int color=selected?0xFF405369:isHoveredOrFocused()?0xFF344355:0xFF263140;
                DepartmentUiStyle.panel(g,getX(),getY(),getWidth(),getHeight(),color);
                g.drawString(font,getMessage(),getX()+7,getY()+7,DepartmentUiStyle.TEXT,false);
            }
        });
    }
    private EditBox edit(String label,int x,int y,int w,int max,String initial) {
        var box=new EditBox(font,x,y,w,20,tr(label));box.setMaxLength(max);box.setValue(initial);addRenderableWidget(box);return box;
    }
    private void widgets() {
        String target=recipient==null?"*":recipient.getValue(),title=subject==null?"":subject.getValue(),content=body==null?"":body.getValue();
        String money=coins==null?"0":coins.getValue(),xp=experience==null?"0":experience.getValue();
        clearWidgets();int top=top(),divider=divider(),x=divider+20,w=width-x-24;
        button(tr("inbox"),8,top-28,120,()->{composing=false;widgets();});
        if(snapshot.administrator())button(tr("compose"),134,top-28,140,()->{composing=true;widgets();});
        button(Component.translatable("gui.back"),8,height-45,88,this::onClose);
        if(composing) {
            recipient=edit("recipient",x,top+22,w,32,target);subject=edit("subject",x,top+68,w,128,title);
            body=new MultiLineEditBox(font,x,top+114,w,Math.max(24,height-top-260),tr("body"),tr("body")) {
                @Override public void renderWidget(GuiGraphics g,int mx,int my,float tick) {
                    if(!visible)return;
                    renderBackground(g);DepartmentUiStyle.scissor(g,getX()+1,getY()+1,getX()+getWidth()-1,getY()+getHeight()-1);
                    g.pose().pushPose();g.pose().translate(0,-scrollAmount(),0);
                    renderContents(g,mx,my,tick);g.pose().popPose();g.disableScissor();renderDecorations(g);
                }
            };
            body.setCharacterLimit(4096);body.setValue(content);addRenderableWidget(body);
            int footer=height-120;
            coins=edit("coins",x,footer,w/2-8,10,money);experience=edit("experience",x+w/2,footer,w/2,7,xp);
            button(tr("attachments").copy().append(" ("+attachments.size()+")"),x,footer+26,w,()->minecraft.setScreen(new MailAttachmentScreen(minecraft.screen,attachments)));
            button(tr(formatSubject?"format_subject":"format_body"),14,top+12,divider-28,()->{formatSubject=!formatSubject;widgets();})
                    .setTooltip(net.minecraft.client.gui.components.Tooltip.create(tr("format_help")));
            // Optional mods may extend ChatFormatting; the editor supports the 16 standard color codes.
            var colors="0123456789abcdef".chars().mapToObj(code->net.minecraft.ChatFormatting.getByCode((char)code)).toList();
            int colorWidth=Math.max(28,(divider-28)/4);
            for(int i=0;i<colors.size();i++) {
                var color=colors.get(i);int cx=14+i%4*colorWidth,cy=top+42+i/4*22;
                var colorButton=net.minecraft.client.gui.components.Button.builder(Component.literal("\u25A0 "+color.getChar()).withStyle(color),b->insertFormat(color.getChar()))
                        .bounds(cx,cy,colorWidth-3,22).build();
                colorButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.dealt_force_skills.mail.color."+color.getName())));addRenderableWidget(colorButton);
            }
            String[] styles={"bold","italic","underline","strikethrough","obfuscated","reset"};char[] codes={'l','o','n','m','k','r'};
            int stylesY=top+42+((colors.size()+3)/4)*22+6;
            for(int i=0;i<styles.length;i++){char code=codes[i];button(tr(styles[i]),14+i%2*(divider-28)/2,stylesY+i/2*24,(divider-32)/2,()->insertFormat(code));}
            button(tr("preview"),14,height-75,divider-28,()->{
                try{minecraft.setScreen(new MailPreviewScreen(minecraft.screen,subject.getValue(),body.getValue(),draftRewards()));}
                catch(NumberFormatException invalid){minecraft.player.displayClientMessage(tr("invalid_amount"),true);}
            });
            button(tr("send"),x,height-45,150,()->{
                try {
                    NetworkHandler.sendToServer(new C2S_MailboxAction(3,new UUID(0,0),snapshot.page(),recipient.getValue().trim(),subject.getValue(),body.getValue(),
                            Long.parseLong(coins.getValue()),Integer.parseInt(experience.getValue()),false,List.copyOf(attachments)));
                } catch(NumberFormatException invalid){minecraft.player.displayClientMessage(tr("invalid_amount"),true);}
            });return;
        }
        for(int i=0;i<snapshot.entries().size();i++) {
            var entry=snapshot.entries().get(i);int rowY=top+i*24;
            mailRow(Component.literal((entry.unread()?"● ":"")+font.plainSubstrByWidth(MailText.parse(entry.title()).getString(),divider-38)),8,rowY,divider-20,entry.id().equals(selected),()->{
                selected=entry.id();scroll=0;widgets();NetworkHandler.sendToServer(C2S_MailboxAction.read(entry.id(),snapshot.page()));
            });
        }
        button(Component.literal("<"),8,height-75,30,()->NetworkHandler.sendToServer(C2S_MailboxAction.open(snapshot.page()-1)));
        button(Component.literal(">"),44,height-75,30,()->NetworkHandler.sendToServer(C2S_MailboxAction.open(snapshot.page()+1)));
        var entry=snapshot.entries().stream().filter(e->e.id().equals(selected)).findFirst().orElse(null);
        if(entry!=null)button(tr("claim"),x,height-45,150,()->NetworkHandler.sendToServer(new C2S_MailboxAction(true,entry.id(),snapshot.page())));
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean charTyped(char code,int modifiers) {
        return super.charTyped(code,modifiers);
    }
    private void insertFormat(char code) {
        if(formatSubject){setFocused(subject);subject.insertText("&"+code);}
        else{setFocused(body);body.charTyped('&',0);body.charTyped(code,0);}
    }
    private TaskDisplay.Rewards draftRewards() {
        long money=Long.parseLong(coins.getValue());int xp=Integer.parseInt(experience.getValue());
        if(money<0||money>1000000000L||xp<0||xp>1000000)throw new NumberFormatException();
        return new TaskDisplay.Rewards(attachments.stream().map(stack->new TaskDisplay.Item(stack,stack.getCount())).toList(),money,xp,List.of());
    }
    @Override public boolean mouseClicked(double x,double y,int button){return !composing&&icons.click(x,y,button)||super.mouseClicked(x,y,button);}
    @Override public void tick(){if(!composing&&++refresh>=80){refresh=0;NetworkHandler.sendToServer(C2S_MailboxAction.open(snapshot.page()));}}
    private int text(GuiGraphics g,Component value,int x,int y,int w,int color){for(var line:font.split(value,w)){g.drawString(font,line,x,y,color,false);y+=13;}return y+8;}
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        icons.clear();int top=top(),divider=divider(),x=divider+20,w=width-x-24;
        g.fill(0,embedded()?40:0,width,height-(embedded()?20:0),DepartmentUiStyle.BG);
        DepartmentUiStyle.panel(g,0,top-4,divider,height-top-48,DepartmentUiStyle.ROW);
        DepartmentUiStyle.panel(g,divider+4,top-4,width-divider-8,height-top-48,DepartmentUiStyle.PANEL);
        if(composing) {
            g.drawString(font,tr("recipient_hint"),x,top+6,DepartmentUiStyle.MUTED,false);
            g.drawString(font,tr("subject"),x,top+52,DepartmentUiStyle.MUTED,false);
            g.drawString(font,tr("body"),x,top+98,DepartmentUiStyle.MUTED,false);
            g.drawString(font,tr("coins"),x,height-134,DepartmentUiStyle.MUTED,false);
            g.drawString(font,tr("experience"),x+w/2,height-134,DepartmentUiStyle.MUTED,false);
        } else {
            g.drawString(font,(snapshot.page()+1)+" / "+snapshot.pages()+"  ·  "+snapshot.total(),84,height-68,DepartmentUiStyle.MUTED,false);
            var entry=snapshot.entries().stream().filter(e->e.id().equals(selected)).findFirst().orElse(null);
            if(entry==null)g.drawString(font,tr("empty"),x,top+16,DepartmentUiStyle.MUTED,false);
            else {
                DepartmentUiStyle.scissor(g,x,top,width-12,height-56);int y=top+10-scroll;
                y=text(g,MailText.parse(entry.title()),x,y,w,DepartmentUiStyle.TEXT);
                y=text(g,Component.translatable("screen.dealt_force_skills.mail.from",entry.sender().isBlank()?"特勤处":entry.sender()),x,y,w,DepartmentUiStyle.MUTED);
                y=text(g,MailText.parse(entry.body()),x,y,w,DepartmentUiStyle.TEXT);
                y=icons.rewards(g,new TaskDisplay.Rewards(entry.items().stream().filter(stack->!stack.isEmpty()).map(stack->new TaskDisplay.Item(stack,stack.getCount())).toList(),
                        entry.coins(),entry.experience(),entry.unlocks()),x,y,w,top,height-56);
                g.disableScissor();maxScroll=Math.max(0,y+scroll-height+56);scroll=Math.min(scroll,maxScroll);
                DepartmentUiStyle.scrollbar(g,width-9,top,height-top-56,scroll,maxScroll);
            }
        }
        super.render(g,mx,my,tick);icons.tooltip(g,mx,my);
    }
    @Override public boolean mouseScrolled(double mx,double my,double delta){if(!composing){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(delta*26)));return true;}return super.mouseScrolled(mx,my,delta);}
    @Override public void onClose(){if(parent instanceof DFGUI_MAIN main)main.closeEmbeddedPage();else minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
