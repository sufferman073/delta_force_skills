package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.network.*;
import com.rzy.dealt_force_skills.workbench.*;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Embedded equipment preview and compact property sliders; all mutations retain server revisions. */
public final class WorkbenchScreen extends Screen {
    private static int sequence;
    private final Screen parent;
    private CompoundTag snapshot=new CompoundTag();
    private int[] draft=new int[WorkbenchStat.values().length];
    private int slot=-1,request,waitingTicks,inventoryScroll,statScroll,maxStatScroll,choiceScroll,refresh,drag,presetScroll;
    private boolean waiting,attachments,presets;
    private String selectedType="",notice="选择一件装备开始改装";
    private Button applyButton;
    private EditBox presetName;
    private Map<String,String> plans=WorkbenchPresets.read();
    private final List<Hit> slots=new ArrayList<>();
    private record Hit(int x,int y,int w,int h,Runnable action){boolean contains(double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}}
    public WorkbenchScreen(Screen parent){super(Component.literal("改枪台 / 装备改装"));this.parent=parent;}
    private int top(){return parent instanceof DFGUI_MAIN?78:44;}
    private int leftWidth(){return Math.max(230,width*42/100);}
    private int right(){return leftWidth()+18;}
    private int bottom(){return height-90;}
    @Override protected void init(){widgets();}
    private void send(int action,int choice) {
        if(waiting||slot<0||action!=0&&!snapshot.hasUUID("Revision"))return;
        request=++sequence;waiting=true;waitingTicks=0;notice="正在处理…";
        var revision=action==0?new UUID(0,0):snapshot.getUUID("Revision");
        NetworkHandler.sendToServer(new C2S_WorkbenchAction(request,action,slot,revision,draft.clone(),choice));widgets();
    }
    public static void accept(int request,CompoundTag snapshot) {
        var current=Minecraft.getInstance().screen;
        WorkbenchScreen screen=current instanceof WorkbenchScreen w?w:current instanceof DFGUI_MAIN main?main.embeddedPage(WorkbenchScreen.class):null;
        if(screen==null||request!=screen.request||!screen.waiting)return;
        screen.waiting=false;screen.snapshot=snapshot.copy();screen.draft=snapshot.getIntArray("Allocation");
        if(screen.draft.length!=WorkbenchStat.values().length)screen.draft=new int[WorkbenchStat.values().length];
        screen.notice=switch(snapshot.getString("Status")) {
            case "ready"->"降低 1 点获得 2 点；单项最多牺牲原属性的 75%";
            case "attachment_installed"->"配件装配成功："+snapshot.getString("AttachmentName")+"（替换的旧配件已退回，背包满时寄往邮箱）";
            case "disabled"->"服务器已禁用改枪台";case "disabled_property"->"部分属性已锁定，请刷新";
            case "stale"->"装备或会话已变化，请刷新";case "invalid_allocation"->"点数不足或属性超限";
            case "missing_material"->"物品栏缺少修复材料";case "no_repair"->"无需修复或无可用材料";
            case "invalid_attachment"->"配件、库存或价格已变化，请刷新";default->"请选择单件装备";
        };
        if(!snapshot.getBoolean("TaczGun")){screen.attachments=false;screen.selectedType="";}
        screen.widgets();
    }
    private List<Integer> equipment() {
        List<Integer> out=new ArrayList<>();if(minecraft==null||minecraft.player==null)return out;
        var inventory=minecraft.player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){var stack=inventory.getItem(i);if(stack.getCount()==1&&!WorkbenchData.types(stack).isEmpty())out.add(i);}return out;
    }
    private Button button(String text,int x,int y,int w,Runnable action) {
        return addRenderableWidget(DepartmentUiStyle.widget(Component.literal(text),x,y,Math.max(20,w),action));
    }
    private void widgets() {
        if(font==null)return;String name=presetName==null?"":presetName.getValue();clearWidgets();applyButton=null;
        int top=top(),x=right(),w=width-x-18;
        button("返回",8,height-44,88,this::onClose);
        if(slot>=0)button("刷新装备",104,height-44,90,()->send(0,0)).active=!waiting;
        if(presets){presetWidgets(name);return;}
        if(snapshot.getBoolean("TaczGun")&&snapshot.getBoolean("AttachmentsEnabled"))
            button(attachments?"属性改装":"TaCZ 配件",x,top-28,110,()->{attachments=!attachments;selectedType="";widgets();}).active=!waiting;
        if(!snapshot.hasUUID("Revision"))return;
        button("清空分配",x+116,top-28,100,()->{for(var tag:snapshot.getList("Stats",10))draft[((CompoundTag)tag).getInt("Id")]=0;widgets();}).active=!waiting;
        button("方案管理",x+222,top-28,100,()->{presets=true;widgets();}).active=!waiting;
        if(attachments)attachmentWidgets();else statWidgets();
        applyButton=button("应用改装",x,height-44,110,()->send(1,0));
        var repair=button("修复至满",x+116,height-44,110,()->send(2,0));
        repair.active=!waiting&&snapshot.getBoolean("RepairEnabled")&&snapshot.getBoolean("Damaged")&&!snapshot.getString("Repair").isEmpty();
        repair.setTooltip(Tooltip.create(Component.literal("消耗 1 个材料："+snapshot.getString("Repair"))));updateApply();
    }
    private void statWidgets() {
        var stats=snapshot.getList("Stats",10);int x=right(),w=width-x-18,top=top();int rowHeight=38;
        int visible=Math.max(1,(bottom()-top)/rowHeight);maxStatScroll=Math.max(0,stats.size()-visible);statScroll=Math.min(statScroll,maxStatScroll);
        int funding=0;for(var tag:stats)funding+=Math.max(0,-((CompoundTag)tag).getInt("Min"))*2;
        for(int i=statScroll;i<Math.min(stats.size(),statScroll+visible);i++) {
            var row=stats.getCompound(i);int id=row.getInt("Id"),min=row.getInt("Min"),max=Math.min(row.getInt("Max"),funding),y=top+(i-statScroll)*rowHeight;
            if(id<0||id>=draft.length)continue;
            addRenderableWidget(new StatSlider(x+24,y+12,w-60,id,min,max,row.getDouble("Base"))).active=!waiting;
            button("−",x,y+12,20,()->step(id,-1,min,max)).active=!waiting;
            button("+",x+w-32,y+12,20,()->step(id,1,min,max)).active=!waiting;
        }
    }
    private void step(int id,int delta,int min,int max){draft[id]=Math.max(min,Math.min(max,draft[id]+delta));widgets();}
    private void attachmentWidgets() {
        if(selectedType.isEmpty())return;int x=right(),w=width-x-18;
        var choices=snapshot.getList("Attachments",10);List<Integer> matching=new ArrayList<>();
        for(int i=0;i<choices.size();i++)if(choices.getCompound(i).getString("Type").equals(selectedType))matching.add(i);
        int count=Math.max(1,(bottom()-top()-30)/34);choiceScroll=Math.min(choiceScroll,Math.max(0,matching.size()-count));
        button("← 配件槽位",x,top(),140,()->{selectedType="";widgets();});
        for(int i=choiceScroll;i<Math.min(matching.size(),choiceScroll+count);i++) {
            int index=matching.get(i);var row=choices.getCompound(index);String source=switch(row.getInt("Source")){case 0->"物品栏";case 1->"仓库";default->"赎回 "+row.getLong("Price")+" 哈夫币";};
            String text=row.getString("Name")+" · "+source;
            var b=button(font.plainSubstrByWidth(text,w-14),x,top()+30+(i-choiceScroll)*34,w-8,()->send(3,index));b.active=!waiting;
            b.setTooltip(Tooltip.create(Component.literal(text+"\n装配后旧配件退回，溢出部分寄邮件")));
        }
    }
    private void presetWidgets(String name) {
        int x=right(),w=width-x-18,y=top(),buttonWidth=(w-12)/3;
        presetName=new EditBox(font,x,y+24,w,22,Component.literal("方案名称"));presetName.setMaxLength(64);presetName.setValue(name);addRenderableWidget(presetName);
        button("保存当前方案",x,y+54,buttonWidth,()->{
            try{if(presetName.getValue().isBlank()||!inRange(draft))throw new IllegalArgumentException();
                plans.put(presetName.getValue().trim(),WorkbenchPlan.export(snapshot.getString("Item"),draft));WorkbenchPresets.save(plans);notice="方案已保存";widgets();}
            catch(Exception ex){notice="请输入名称并确认点数有效；保存失败时检查配置目录";}
        });
        button("复制到剪贴板",x+buttonWidth+6,y+54,buttonWidth,()->{if(inRange(draft))minecraft.keyboardHandler.setClipboard(WorkbenchPlan.export(snapshot.getString("Item"),draft));});
        button("从剪贴板导入",x+2*(buttonWidth+6),y+54,buttonWidth,()->loadPlan(minecraft.keyboardHandler.getClipboard()));
        int row=0,visible=Math.max(1,(bottom()-y-120)/28);
        presetScroll=Math.min(presetScroll,Math.max(0,plans.size()-visible));
        for(var entry:plans.entrySet().stream().skip(presetScroll).limit(visible).toList()) {
            int ry=y+90+row++*28;
            button(font.plainSubstrByWidth(entry.getKey(),w-95),x,ry,w-92,()->{presetName.setValue(entry.getKey());loadPlan(entry.getValue());});
            button("删除",x+w-85,ry,76,()->{plans.remove(entry.getKey());try{WorkbenchPresets.save(plans);widgets();}catch(java.io.IOException ex){notice="删除保存失败";}});
        }
        button("返回改装预览",x,height-44,160,()->{presets=false;widgets();});
    }
    private void loadPlan(String text) {
        try{var values=WorkbenchPlan.read(text,snapshot.getString("Item"));if(!inRange(values))throw new IllegalArgumentException();
            draft=values;notice="方案已载入预览，点击应用改装提交";presets=false;widgets();}
        catch(RuntimeException ex){notice="方案无效、装备不符、点数不足或超出属性范围";}
    }
    private boolean inRange(int[] values) {
        if(WorkbenchData.balance(values)<0)return false;boolean[] available=new boolean[values.length];
        for(var tag:snapshot.getList("Stats",10)){var row=(CompoundTag)tag;int id=row.getInt("Id");if(id<0||id>=values.length)return false;available[id]=true;
            if(values[id]<row.getInt("Min")||values[id]>row.getInt("Max"))return false;}
        int[] committed=snapshot.getIntArray("Allocation");for(int i=0;i<values.length;i++)if(!available[i]&&values[i]!=(i<committed.length?committed[i]:0))return false;return true;
    }
    private void updateApply(){if(applyButton!=null)applyButton.active=!waiting&&inRange(draft);}
    @Override public void tick(){if(waiting&&++waitingTicks>=60){waiting=false;notice="请求未响应，请刷新重试";widgets();}updateApply();}
    private void model(GuiGraphics g,ItemStack stack,int cx,int cy,int size) {
        if(stack.isEmpty())return;g.pose().pushPose();g.pose().translate(cx,cy,160);g.pose().scale(size,-size,size);
        g.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(12));g.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(25));
        minecraft.getItemRenderer().renderStatic(stack,net.minecraft.world.item.ItemDisplayContext.FIXED,15728880,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,g.pose(),g.bufferSource(),minecraft.level,0);g.flush();g.pose().popPose();
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        slots.clear();int top=top(),left=leftWidth(),x=right(),w=width-x-18;
        g.fill(0,parent instanceof DFGUI_MAIN?40:0,width,height-(parent instanceof DFGUI_MAIN?20:0),DepartmentUiStyle.BG);
        DepartmentUiStyle.panel(g,8,top-4,left-12,height-top-48,DepartmentUiStyle.PANEL);
        DepartmentUiStyle.panel(g,x-8,top-4,w+12,height-top-48,DepartmentUiStyle.PANEL);
        if(slot>=0&&minecraft.player!=null)model(g,minecraft.player.getInventory().getItem(slot),left/2,top+(bottom()-top)/2-15,Math.min(120,left/3));
        g.drawString(font,font.plainSubstrByWidth(snapshot.getString("Name"),left-28),20,top+10,DepartmentUiStyle.TEXT,false);
        if(!presets) {
            var equipment=equipment();int columns=Math.max(1,(left-28)/38),visible=columns*2;
            inventoryScroll=Math.min(inventoryScroll,Math.max(0,equipment.size()-visible));
            for(int i=inventoryScroll;i<Math.min(equipment.size(),inventoryScroll+visible);i++) {
                int index=equipment.get(i),ix=20+((i-inventoryScroll)%columns)*38,iy=bottom()-66+((i-inventoryScroll)/columns)*34;
                DepartmentUiStyle.panel(g,ix,iy,32,30,index==slot?0xFF465B61:DepartmentUiStyle.ROW);
                var stack=minecraft.player.getInventory().getItem(index);g.renderItem(stack,ix+8,iy+6);
                slots.add(new Hit(ix,iy,32,30,()->{if(waiting)return;slot=index;statScroll=0;snapshot=new CompoundTag();selectedType="";send(0,0);}));
                if(mx>=ix&&mx<ix+32&&my>=iy&&my<iy+30)g.renderTooltip(font,stack,mx,my);
            }
            if(attachments&&selectedType.isEmpty()) {
                var types=snapshot.getList("AttachmentSlots",10);int cx=x+w/2,cy=top+(bottom()-top)/2;
                model(g,minecraft.player.getInventory().getItem(slot),cx,cy,Math.min(54,w/7));
                for(int i=0;i<types.size();i++) {
                    var row=types.getCompound(i);double angle=Math.PI/3*i-Math.PI/2;
                    int ix=cx+(int)(Math.cos(angle)*Math.min(140,w/3))-24,iy=cy+(int)(Math.sin(angle)*Math.min(140,(bottom()-top)/2-28))-24;
                    DepartmentUiStyle.panel(g,ix,iy,48,48,DepartmentUiStyle.GROUP);
                    var part=readItem(row.getCompound("Item"));if(part.isEmpty())g.drawCenteredString(font,"+",ix+24,iy+18,DepartmentUiStyle.MUTED);else g.renderItem(part,ix+16,iy+16);
                    g.drawCenteredString(font,slotName(row.getString("Type")),ix+24,iy+50,DepartmentUiStyle.MUTED);
                    slots.add(new Hit(ix,iy,48,48,()->{selectedType=row.getString("Type");choiceScroll=0;widgets();}));
                }
            } else if(!attachments) {
                var stats=snapshot.getList("Stats",10);int visibleRows=Math.max(1,(bottom()-top)/38);
                for(int i=statScroll;i<Math.min(stats.size(),statScroll+visibleRows);i++) {
                    var row=stats.getCompound(i);int id=row.getInt("Id"),y=top+(i-statScroll)*38;var stat=WorkbenchStat.values()[id];
                    g.drawString(font,stat.title,x,y,DepartmentUiStyle.TEXT,false);int barX=x+w/2;
                    g.fill(barX,y+2,x+w-12,y+7,0xFF263239);
                    double ratio=(draft[id]-row.getInt("Min"))/(double)Math.max(1,row.getInt("Max")-row.getInt("Min"));
                    g.fill(barX,y+2,barX+(int)((w/2-12)*ratio),y+7,DepartmentUiStyle.YELLOW);
                }
                DepartmentUiStyle.scrollbar(g,width-10,top,bottom()-top,statScroll,maxStatScroll);
            }
            g.drawString(font,"剩余改装点："+WorkbenchData.balance(draft),x,height-72,WorkbenchData.balance(draft)>=0?0xFF80C9A0:0xFFD86C6C,false);
        } else g.drawString(font,"方案管理 · 输入名称保存，点击已有方案载入预览",x,top+6,DepartmentUiStyle.TEXT,false);
        g.drawString(font,font.plainSubstrByWidth(notice,width-24),12,height-58,snapshot.getString("Status").equals("attachment_installed")?0xFF80C9A0:DepartmentUiStyle.MUTED,false);
        super.render(g,mx,my,tick);
    }
    private ItemStack readItem(CompoundTag tag){return ItemStack.parseOptional(minecraft.level.registryAccess(),tag);}
    private static String slotName(String type){return switch(type){case "SCOPE"->"瞄具";case "MUZZLE"->"枪口";case "STOCK"->"枪托";case "GRIP"->"握把";case "LASER"->"镭射";case "EXTENDED_MAG"->"弹匣";default->type;};}
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(button==0)for(var hit:slots)if(hit.contains(mx,my)){hit.action().run();return true;}
        if(button==0&&!attachments&&mx>=width-12&&my>=top()&&my<bottom()){drag=1;return true;}return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double delta) {
        if(presets)presetScroll=Math.max(0,presetScroll-(int)delta);
        else if(mx<leftWidth())inventoryScroll=Math.max(0,inventoryScroll-(int)(delta*2));
        else if(attachments)choiceScroll=Math.max(0,choiceScroll-(int)delta);
        else statScroll=Math.max(0,Math.min(maxStatScroll,statScroll-(int)delta));widgets();return true;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(drag==1){statScroll=Math.max(0,Math.min(maxStatScroll,(int)((my-top())/(bottom()-top())*maxStatScroll)));widgets();return true;}return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button){drag=0;return super.mouseReleased(mx,my,button);}
    @Override public void onClose(){if(parent instanceof DFGUI_MAIN main)main.closeEmbeddedPage();else minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    private final class StatSlider extends AbstractSliderButton {
        private final int id,min,max;private final double base;
        StatSlider(int x,int y,int width,int id,int min,int max,double base){super(x,y,width,20,Component.empty(),max==min?0:(draft[id]-min)/(double)(max-min));this.id=id;this.min=min;this.max=max;this.base=base;updateMessage();}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float tick) {
            DepartmentUiStyle.panel(g,getX(),getY(),getWidth(),getHeight(),active?DepartmentUiStyle.ROW:DepartmentUiStyle.BG);
            int filled=(int)((getWidth()-4)*value);g.fill(getX()+2,getY()+getHeight()-4,getX()+2+filled,getY()+getHeight()-2,DepartmentUiStyle.YELLOW);
            int knob=getX()+(int)((getWidth()-4)*value);g.fill(knob,getY()+2,knob+3,getY()+getHeight()-5,0x607E9AA0);
            g.drawCenteredString(font,getMessage(),getX()+getWidth()/2,getY()+5,active?DepartmentUiStyle.TEXT:DepartmentUiStyle.MUTED);
        }
        @Override protected void updateMessage(){var stat=WorkbenchStat.values()[id];setMessage(Component.literal(number(base)+" → "+number(base+stat.change(base,draft[id]))+" ["+(draft[id]>0?"+":"")+draft[id]+"]"));}
        @Override protected void applyValue(){draft[id]=(int)Math.round(min+value*(max-min));updateMessage();updateApply();}
    }
    private static String number(double v){return String.format(Locale.ROOT,"%.2f",v).replaceAll("0+$","").replaceAll("\\.$","");}
}
