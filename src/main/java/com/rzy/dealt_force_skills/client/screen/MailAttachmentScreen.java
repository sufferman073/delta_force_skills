package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.mail.MailboxManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Edits copies only; the server authorizes and validates the submitted attachment list. */
final class MailAttachmentScreen extends Screen {
    private final Screen parent;
    private final List<ItemStack> attachments;
    private final List<ItemStack> catalog=new ArrayList<>();
    private List<ItemStack> filtered=List.of();
    private EditBox search,quantity;
    private boolean inventory=true;
    private int page,selected=-1;
    private record Hit(int x,int y,ItemStack stack,int index) {
        boolean contains(double mx,double my){return mx>=x&&mx<x+24&&my>=y&&my<y+24;}
    }
    private final List<Hit> sourceHits=new ArrayList<>(),attachmentHits=new ArrayList<>();
    MailAttachmentScreen(Screen parent,List<ItemStack> attachments) {
        super(tr("attachments"));this.parent=parent;this.attachments=attachments;
        for(var item:BuiltInRegistries.ITEM){var stack=new ItemStack(item);if(!stack.isEmpty())catalog.add(stack);}
    }
    private static Component tr(String key){return Component.translatable("screen.dealt_force_skills.mail."+key);}
    private void button(Component label,int x,int y,int width,Runnable action){addRenderableWidget(DepartmentUiStyle.widget(label,x,y,width,action));}
    @Override protected void init() {
        String query=search==null?"":search.getValue(),amount=quantity==null?"1":quantity.getValue();
        search=new EditBox(font,12,40,width/2-24,20,tr("search"));search.setMaxLength(128);search.setValue(query);
        search.setResponder(value->{page=0;filter();});addRenderableWidget(search);
        quantity=new EditBox(font,width/2+12,height-86,72,20,tr("quantity"));quantity.setMaxLength(3);quantity.setValue(amount);addRenderableWidget(quantity);
        button(tr(inventory?"inventory_source":"registry_source"),12,12,width/2-24,()->{inventory=!inventory;page=0;clearWidgets();init();});
        button(Component.literal("<"),12,height-86,30,()->page=Math.max(0,page-1));
        button(Component.literal(">"),48,height-86,30,()->page=Math.min(Math.max(0,(filtered.size()-1)/pageSize()),page+1));
        button(tr("apply_quantity"),width/2+90,height-86,Math.max(50,width/4-32),()->{
            if(selected>=0&&selected<attachments.size())attachments.get(selected).setCount(amount(attachments.get(selected)));
        });
        button(tr("remove"),width*3/4+64,height-86,Math.max(40,width/4-76),()->{
            if(selected>=0&&selected<attachments.size()){attachments.remove(selected);selected=-1;}
        });
        button(Component.translatable("gui.done"),12,height-34,90,this::onClose);filter();
    }
    private int columns(){return Math.max(1,(width/2-24)/26);}
    private int pageSize(){return columns()*Math.max(1,(height-170)/26);}
    private int amount(ItemStack stack){try{return Math.max(1,Math.min(stack.getMaxStackSize(),Integer.parseInt(quantity.getValue())));}catch(NumberFormatException e){return 1;}}
    private void filter() {
        String query=search.getValue().toLowerCase(Locale.ROOT).trim();List<ItemStack> source=new ArrayList<>();
        if(inventory&&minecraft.player!=null)for(int i=0;i<minecraft.player.getInventory().getContainerSize();i++) {
            var stack=minecraft.player.getInventory().getItem(i);if(!stack.isEmpty())source.add(stack.copy());
        } else if(!inventory)source.addAll(catalog);
        filtered=source.stream().filter(s->s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                ||BuiltInRegistries.ITEM.getKey(s.getItem()).toString().contains(query)).toList();
    }
    private void slot(GuiGraphics g,Hit hit,boolean active) {
        DepartmentUiStyle.panel(g,hit.x,hit.y,24,24,active?0xFF526437:DepartmentUiStyle.GROUP);
        g.renderItem(hit.stack,hit.x+4,hit.y+4);g.renderItemDecorations(font,hit.stack,hit.x+4,hit.y+4);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        g.fill(0,0,width,height,DepartmentUiStyle.BG);sourceHits.clear();attachmentHits.clear();
        g.drawString(font,tr("attachments").copy().append(" "+attachments.size()+" / "+MailboxManager.MAX_ATTACHMENTS),width/2+12,16,DepartmentUiStyle.TEXT,false);
        int cols=columns(),start=page*pageSize();
        for(int i=start;i<Math.min(filtered.size(),start+pageSize());i++) {
            int j=i-start;var hit=new Hit(12+j%cols*26,70+j/cols*26,filtered.get(i),i);sourceHits.add(hit);slot(g,hit,false);
        }
        for(int i=0;i<attachments.size();i++) {
            var hit=new Hit(width/2+12+i%cols*26,70+i/cols*26,attachments.get(i),i);attachmentHits.add(hit);slot(g,hit,i==selected);
        }
        g.drawString(font,(page+1)+" / "+Math.max(1,(filtered.size()+pageSize()-1)/pageSize()),84,height-80,DepartmentUiStyle.MUTED,false);
        g.drawString(font,tr("quantity"),width/2+12,height-100,DepartmentUiStyle.MUTED,false);
        int y=height-58;for(var line:font.split(tr("attachment_help"),width-24)){g.drawString(font,line,12,y,DepartmentUiStyle.MUTED,false);y+=11;}
        super.render(g,mx,my,tick);
        for(var hit:sourceHits)if(hit.contains(mx,my)){g.renderTooltip(font,hit.stack,mx,my);return;}
        for(var hit:attachmentHits)if(hit.contains(mx,my)){g.renderTooltip(font,hit.stack,mx,my);return;}
    }
    @Override public boolean mouseScrolled(double mx,double my,double delta){page=Math.max(0,Math.min(Math.max(0,(filtered.size()-1)/pageSize()),page-(int)Math.signum(delta)));return true;}
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(super.mouseClicked(mx,my,button))return true;
        if(button!=0)return false;
        for(var hit:sourceHits)if(hit.contains(mx,my)) {
            if(attachments.size()<MailboxManager.MAX_ATTACHMENTS) {
                var copy=hit.stack.copyWithCount(inventory?hit.stack.getCount():amount(hit.stack));
                attachments.add(copy);selected=attachments.size()-1;quantity.setValue(Integer.toString(copy.getCount()));
            }
            return true;
        }
        for(var hit:attachmentHits)if(hit.contains(mx,my)){selected=hit.index;quantity.setValue(Integer.toString(hit.stack.getCount()));return true;}
        return false;
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
