package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.department.DepartmentTracker;
import com.rzy.dealt_force_skills.network.C2S_DepartmentAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Supplied TaskScreen's grouped tree, palette, tabs and progress layout, bound to the server tracker. */
public final class DepartmentScreen extends Screen {
    private static final List<String> DEPARTMENTS=List.of("战斗部门","后勤部门","医疗部门","战术部门","研发部门","命运契约","收集者");
    private final Screen parent;
    private final RewardIcons icons=new RewardIcons();
    private double lastMouseY;
    private final Map<String,DepartmentTracker.Detail> details=new HashMap<>();
    private final Map<String,DepartmentTracker.Page> pages=new HashMap<>();
    private final Set<String> collapsed=new HashSet<>();
    private DepartmentTracker.Page page=new DepartmentTracker.Page(DEPARTMENTS.get(0),0,1,List.of(),null);
    private String department=DEPARTMENTS.get(0),selected="";
    private int scroll,listScroll,maxScroll,maxListScroll,refresh,drag,pageIndex,popupScroll,maxPopupScroll;
    private boolean prerequisitePopup,abandonPopup;
    private final List<Hit> hits=new ArrayList<>();
    private record Hit(int x,int y,int w,int h,Runnable action) { boolean contains(double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;} }
    public DepartmentScreen(Screen parent){super(Component.translatable("screen.dealt_force_skills.department.title"));this.parent=parent;}
    private boolean embedded(){return parent instanceof DFGUI_MAIN;}
    private int top(){return embedded()?72:36;}
    private int bottom(){return height-48;}
    private int divider(){return Math.max(170,width/4);}
    @Override protected void init(){request(0);}
    private void request(int action){request(action,selected);}
    private void request(int action,String id){NetworkHandler.sendToServer(new C2S_DepartmentAction(action,department,pageIndex,id));}
    public static void accept(DepartmentTracker.Page snapshot) {
        Screen current=Minecraft.getInstance().screen;
        DepartmentScreen screen=current instanceof DepartmentScreen d?d:current instanceof DFGUI_MAIN main?main.embeddedPage(DepartmentScreen.class):null;
        if(screen==null)return;
        screen.pages.put(snapshot.department(),snapshot);
        if(snapshot.detail()!=null)screen.details.put(snapshot.detail().id(),snapshot.detail());
        if(!snapshot.department().equals(screen.department)||screen.pageIndex>=0&&snapshot.page()!=screen.pageIndex)return;
        // A prerequisite jump is awaiting this exact detail; an older page must not change selection.
        if(screen.pageIndex<0&&!screen.selected.isEmpty()
                &&(snapshot.detail()==null||!screen.selected.equals(snapshot.detail().id())))return;
        screen.pageIndex=snapshot.page();
        screen.page=snapshot;
        if(snapshot.detail()!=null && (screen.selected.isEmpty()
                || snapshot.rows().stream().noneMatch(row->row.id().equals(screen.selected)))) {
            screen.selected=snapshot.detail().id();screen.scroll=0;screen.prerequisitePopup=false;screen.abandonPopup=false;
        }
    }
    public void navigate(String group,String task) {
        department=group;selected=task;pageIndex=task.isBlank()?0:-1;scroll=0;listScroll=0;popupScroll=0;prerequisitePopup=false;abandonPopup=false;
        page=pages.getOrDefault(group,new DepartmentTracker.Page(group,0,1,List.of(),null));request(0);
    }
    @Override public void tick(){if(++refresh>=40){refresh=0;request(0);}}
    private void hit(int x,int y,int w,int h,Runnable action){hits.add(new Hit(x,y,w,h,action));}
    private void button(GuiGraphics g,int x,int y,int w,Component label,int mx,int my,Runnable action) {
        DepartmentUiStyle.button(g,font,x,y,w,label,mx>=x&&mx<x+w&&my>=y&&my<y+22);hit(x,y,w,22,action);
    }
    private int text(GuiGraphics g,Component text,int x,int y,int w,int color) {
        for(var line:font.split(text,w)){g.drawString(font,line,x,y,color,false);y+=12;}return y+6;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        hits.clear();icons.clear();lastMouseY=my;int tabY=embedded()?44:8,top=top(),bottom=bottom(),divider=divider();
        g.fill(0,embedded()?40:0,width,height-(embedded()?20:0),DepartmentUiStyle.BG);
        int tabWidth=Math.min(108,(width-16)/7);
        for(int i=0;i<DEPARTMENTS.size();i++) {
            String name=DEPARTMENTS.get(i);int x=8+i*tabWidth;
            DepartmentUiStyle.tab(g,font,x,tabY,tabWidth-3,Component.literal(name),name.equals(department));
            hit(x,tabY,tabWidth-3,20,()->navigate(name,""));
        }
        DepartmentUiStyle.panel(g,0,top,divider-1,44,0xFF2A3036);
        g.drawString(font,department,10,top+8,DepartmentUiStyle.TEXT,false);
        long completed=page.rows().stream().filter(r->r.status().equals("COMPLETED")).count();
        g.drawString(font,completed+" / "+page.rows().size(),10,top+23,DepartmentUiStyle.MUTED,false);
        g.fill(10,top+36,divider-12,top+40,0xFF3A4046);
        if(!page.rows().isEmpty())g.fill(10,top+36,10+(int)((divider-22)*completed/page.rows().size()),top+40,DepartmentUiStyle.YELLOW);
        if(page.pages()>1) {
            button(g,divider-56,top+18,22,Component.literal("<"),mx,my,()->changePage(-1));
            button(g,divider-30,top+18,22,Component.literal(">"),mx,my,()->changePage(1));
        }
        g.fill(divider,top,divider+1,bottom,0x30FFFFFF);
        DepartmentUiStyle.panel(g,divider+2,top,width-divider-6,bottom-top,DepartmentUiStyle.PANEL);
        int listTop=top+48,y=listTop-listScroll;String chapter=null;
        DepartmentUiStyle.scissor(g,0,listTop,divider-7,bottom);
        for(var row:page.rows()) {
            String group=row.chapter().isBlank()?department:row.chapter();
            if(!group.equals(chapter)) {
                chapter=group;DepartmentUiStyle.panel(g,0,y,divider-8,24,DepartmentUiStyle.GROUP);
                g.drawString(font,font.plainSubstrByWidth((collapsed.contains(group)?"▸ ":"▾ ")+group,divider-22),10,y+8,DepartmentUiStyle.TEXT,false);
                int cy=y;if(cy>=listTop&&cy+24<=bottom)hit(0,cy,divider-8,24,()->{if(!collapsed.add(group))collapsed.remove(group);});y+=24;
            }
            if(collapsed.contains(group))continue;
            int rowY=y;g.fill(10,y+9,18,y+10,0x40FFFFFF);g.fill(10,y,11,y+10,0x40FFFFFF);
            DepartmentUiStyle.panel(g,18,y,divider-27,20,row.id().equals(selected)?0xFF334249:DepartmentUiStyle.ROW);
            g.drawString(font,font.plainSubstrByWidth(row.title(),divider-52),24,y+6,DepartmentUiStyle.TEXT,false);
            if(row.status().equals("LOCKED"))g.fill(18,y,divider-9,y+20,0xB5101215);
            if(row.status().equals("PINNED"))g.drawString(font,"◆",divider-23,y+6,DepartmentUiStyle.YELLOW,false);
            if(row.status().equals("COMPLETED"))g.drawString(font,"✓",divider-23,y+6,0xFF80C9A0,false);
            if(rowY>=listTop&&rowY+20<=bottom)hit(18,rowY,divider-27,20,()->{
                selected=row.id();scroll=0;popupScroll=0;prerequisitePopup=row.status().equals("LOCKED");request(0);
            });y+=20;
        }
        g.disableScissor();maxListScroll=Math.max(0,y+listScroll-bottom);listScroll=Math.min(listScroll,maxListScroll);
        DepartmentUiStyle.scrollbar(g,divider-6,listTop,bottom-listTop,listScroll,maxListScroll);
        var d=details.get(selected);int x=divider+12,w=width-x-18;
        if(d==null)g.drawString(font,Component.translatable("screen.dealt_force_skills.department.loading"),x,top+12,DepartmentUiStyle.MUTED,false);
        else if(!prerequisitePopup&&!abandonPopup) {
            DepartmentUiStyle.scissor(g,x,top+4,width-12,bottom);int dy=top+12-scroll;
            dy=text(g,Component.literal(d.title()),x,dy,w,DepartmentUiStyle.TEXT);
            dy=text(g,Component.translatable("screen.dealt_force_skills.department.state."+d.status().toLowerCase(java.util.Locale.ROOT)),x,dy,w,DepartmentUiStyle.YELLOW);
            dy=text(g,Component.literal(d.brief()),x,dy,w,DepartmentUiStyle.TEXT);
            for(String line:d.constraints())dy=text(g,Component.literal(line),x,dy,w,0xFFE2B57B);
            if(d.retryTicks()>0)dy=text(g,Component.translatable("screen.dealt_force_skills.department.retry",(d.retryTicks()+19)/20),x,dy,w,DepartmentUiStyle.MUTED);
            for(var goal:d.goals()) {
                dy=text(g,goal.text(),x,dy,w,DepartmentUiStyle.TEXT);
                dy=icons.items(g,goal.items(),x,dy,w,top+4,bottom);
                g.fill(x,dy,x+w,dy+14,0xFF2A1414);g.fill(x,dy,x+(int)(w*Math.min(1,goal.current()/goal.target())),dy+14,0xFF696223);
                g.drawString(font,number(goal.current())+" / "+number(goal.target()),x+4,dy+3,DepartmentUiStyle.YELLOW,false);dy+=24;
            }
            dy=text(g,Component.translatable("screen.dealt_force_skills.department.reward"),x,dy,w,DepartmentUiStyle.MUTED);
            dy=text(g,d.reward(),x,dy,w,DepartmentUiStyle.TEXT);
            dy=icons.rewards(g,d.preview(),x,dy,w,top+4,bottom);
            if(d.repeatable())dy=text(g,Component.translatable("screen.dealt_force_skills.department.refresh_in",(d.refreshTicks()+19)/20),x,dy,w,DepartmentUiStyle.MUTED);
            g.disableScissor();maxScroll=Math.max(0,dy+scroll-bottom);scroll=Math.min(scroll,maxScroll);
            DepartmentUiStyle.scrollbar(g,width-9,top,bottom-top,scroll,maxScroll);int by=height-44;
            if(d.repeatable()) {
                if(d.status().equals("ACTIVE"))button(g,x,by,110,Component.translatable("screen.dealt_force_skills.department.submit"),mx,my,()->request(2));
                button(g,x+116,by,110,Component.translatable("screen.dealt_force_skills.department."+(d.offerLocked()?"unpin":"pin")),mx,my,()->request(4));
                if(!d.prerequisites().isEmpty())button(g,x+232,by,180,Component.translatable("screen.dealt_force_skills.department.refresh_group"),mx,my,()->request(5,d.prerequisites().get(0).id()));
            } else if(d.status().equals("AVAILABLE"))button(g,x,by,110,Component.translatable("screen.dealt_force_skills.department.accept"),mx,my,()->request(1));
            else if(d.status().equals("ACTIVE"))button(g,x,by,110,Component.translatable("screen.dealt_force_skills.department.abandon"),mx,my,()->abandonPopup=true);
            else if(d.status().equals("ABANDONED"))button(g,x,by,110,Component.translatable("screen.dealt_force_skills.department.resume"),mx,my,()->request(1));
        }
        button(g,8,height-44,88,Component.translatable("gui.back"),mx,my,this::onClose);
        if(prerequisitePopup||abandonPopup) {
            hits.clear();icons.clear();g.fill(0,top,width,height-20,0xB0000000);
            int pw=Math.min(460,width-24),px=(width-pw)/2,ph=Math.min(290,height-top-36),py=top+(height-top-20-ph)/2;
            DepartmentUiStyle.panel(g,px,py,pw,ph,DepartmentUiStyle.GROUP);
            g.drawString(font,Component.translatable("screen.dealt_force_skills.department."+(abandonPopup?"abandon_confirm":"prerequisites")),px+12,py+12,DepartmentUiStyle.TEXT,false);
            if(abandonPopup)button(g,px+12,py+45,140,Component.translatable("screen.dealt_force_skills.department.abandon"),mx,my,()->{abandonPopup=false;request(3);});
            else if(d!=null) {
                int listY=py+38,listBottom=py+ph-42;
                maxPopupScroll=Math.max(0,d.prerequisites().size()*27-(listBottom-listY));popupScroll=Math.min(popupScroll,maxPopupScroll);
                DepartmentUiStyle.scissor(g,px+10,listY,px+pw-10,listBottom);int py2=listY-popupScroll;
                for(var row:d.prerequisites()) {
                    DepartmentUiStyle.button(g,font,px+12,py2,pw-28,Component.literal(row.chapter()+" · "+row.title()+" →"),mx>=px+12&&mx<px+pw-16&&my>=py2&&my<py2+22);
                    int clippedTop=Math.max(listY,py2),clippedBottom=Math.min(listBottom,py2+22);
                    if(clippedBottom>clippedTop)hit(px+12,clippedTop,pw-28,clippedBottom-clippedTop,()->navigate(row.chapter(),row.id()));
                    py2+=27;
                }
                g.disableScissor();DepartmentUiStyle.scrollbar(g,px+pw-10,listY,listBottom-listY,popupScroll,maxPopupScroll);
            }
            button(g,px+pw-100,py+ph-32,88,Component.translatable("gui.back"),mx,my,()->{prerequisitePopup=false;abandonPopup=false;});
        }
        if(!prerequisitePopup&&!abandonPopup)icons.tooltip(g,mx,my);
    }
    private void changePage(int delta){int next=Math.max(0,Math.min(page.pages()-1,pageIndex+delta));if(next!=pageIndex){pageIndex=next;selected="";listScroll=0;scroll=0;request(0);}}
    private static String number(double n){return n==Math.rint(n)?Long.toString((long)n):String.format(java.util.Locale.ROOT,"%.1f",n);}
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(!prerequisitePopup&&!abandonPopup&&icons.click(mx,my,button))return true;
        if(!prerequisitePopup&&!abandonPopup&&com.rzy.dealt_force_skills.client.RecipeViewer.available()&&button==1)return icons.click(mx,my,button);
        if(button!=0)return false;
        for(int i=hits.size()-1;i>=0;i--)if(hits.get(i).contains(mx,my)){hits.get(i).action().run();return true;}
        if(!prerequisitePopup&&!abandonPopup && my>=top()&&my<=bottom()) {
            if(mx>=divider()-8&&mx<divider()){drag=1;return true;}
            if(mx>=width-11){drag=2;return true;}
        }
        return prerequisitePopup||abandonPopup;
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double delta) {
        if(prerequisitePopup){popupScroll=Math.max(0,Math.min(maxPopupScroll,popupScroll-(int)(delta*27)));return true;}
        if(abandonPopup)return true;
        if(mx<divider())listScroll=Math.max(0,Math.min(maxListScroll,listScroll-(int)(delta*30)));
        else scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(delta*30)));return true;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(drag==0)return false;int max=drag==1?maxListScroll:maxScroll;
        int value=(int)(Math.max(0,Math.min(1,(my-top())/(bottom()-top())))*max);if(drag==1)listScroll=value;else scroll=value;return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){drag=0;return true;}
    @Override public void onClose(){if(parent instanceof DFGUI_MAIN main)main.closeEmbeddedPage();else minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
