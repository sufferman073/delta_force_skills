package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.client.RecipeViewer;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.department.TaskDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Shared item/currency/character presentation for inbox, composer and department details. */
final class RewardIcons {
    private record Cell(int x,int y,int top,int bottom,ItemStack stack,long count,Component label,ResourceLocation texture,boolean portrait) {
        boolean contains(double mx,double my){return mx>=x&&mx<x+46&&my>=Math.max(y,top)&&my<Math.min(y+40,bottom);}
    }
    private final List<Cell> cells=new ArrayList<>();
    private int contentTop,contentBottom,lastMouseX,lastMouseY;
    void clear(){cells.clear();}
    boolean scroll(double delta){return false;}
    int items(GuiGraphics g,List<TaskDisplay.Item> items,int x,int y,int width,int top,int bottom) {
        return rewards(g,new TaskDisplay.Rewards(items,0,0,List.of()),x,y,width,top,bottom);
    }
    int rewards(GuiGraphics g,TaskDisplay.Rewards rewards,int x,int y,int width,int top,int bottom) {
        contentTop=bottom;contentBottom=top;
        List<Cell> entries=new ArrayList<>();
        for(var item:rewards.items())entries.add(new Cell(0,0,top,bottom,item.stack(),item.count(),item.stack().getHoverName(),null,false));
        if(rewards.coins()>0)entries.add(new Cell(0,0,top,bottom,ItemStack.EMPTY,rewards.coins(),Component.translatable("currency.deltaforce.havoc_coin"),
                new ResourceLocation("deltaforce","textures/gui/coin/havoc_coin.png"),false));
        if(rewards.experience()>0)entries.add(new Cell(0,0,top,bottom,ItemStack.EMPTY,rewards.experience(),
                Component.translatable("screen.dealt_force_skills.mail.experience"),new ResourceLocation("minecraft","textures/item/experience_bottle.png"),false));
        for(String id:rewards.unlocks()) {
            var name=ClientCharacterSelectionState.catalogCharacter(id).map(c->c.displayName()).orElse(Component.literal(id));
            String path=id.contains(":")?id.substring(id.indexOf(':')+1):id;
            entries.add(new Cell(0,0,top,bottom,ItemStack.EMPTY,1,name,new ResourceLocation("deltaforce","textures/gui/operator/"+path+".png"),true));
        }
        int columns=Math.max(1,width/50),i=0;
        for(var entry:entries) {
            int cx=x+(i%columns)*50,cy=y+(i/columns)*44;i++;
            Cell cell=new Cell(cx,cy,top,bottom,entry.stack,entry.count,entry.label,entry.texture,entry.portrait);
            if(cy+40<=top||cy>=bottom)continue;
            cells.add(cell);DepartmentUiStyle.panel(g,cx,cy,46,40,DepartmentUiStyle.GROUP);
            if(cell.texture!=null) {
                if(Minecraft.getInstance().getResourceManager().getResource(cell.texture).isPresent())
                    g.blit(cell.texture,cx+(cell.portrait?13:15),cy+3,0,0,cell.portrait?20:16,cell.portrait?26:16,cell.portrait?20:16,cell.portrait?26:16);
                else g.drawCenteredString(Minecraft.getInstance().font,"?",cx+23,cy+8,DepartmentUiStyle.TEXT);
            } else g.renderItem(cell.stack,cx+15,cy+4);
            String amount=cell.portrait?"+":Long.toString(cell.count);
            var font=Minecraft.getInstance().font;
            // Keep large exact counts readable without covering neighbouring icons.
            float scale=Math.min(1,40F/Math.max(1,font.width(amount)));
            g.pose().pushPose();g.pose().translate(cx+23,cy+30,0);g.pose().scale(scale,scale,1);
            g.drawCenteredString(font,amount,0,0,DepartmentUiStyle.TEXT);g.pose().popPose();
        }
        return y+((entries.size()+columns-1)/columns)*44;
    }
    void tooltip(GuiGraphics g,int mx,int my) {
        lastMouseX=mx;lastMouseY=my;
        var mc=Minecraft.getInstance();
        for(var cell:cells)if(cell.contains(mx,my)) {
            List<Component> lines=new ArrayList<>();
            if(!cell.stack.isEmpty())lines.addAll(Screen.getTooltipFromItem(mc,cell.stack));else lines.add(cell.label);
            lines.add(cell.label.copy().append(" × "+cell.count));
            if(!cell.stack.isEmpty())lines.add(Component.translatable("screen.dealt_force_skills.mail."+(RecipeViewer.available()?"jei_hint":"jei_missing")));
            g.renderComponentTooltip(mc.font,lines,mx,my);return;
        }
    }
    boolean click(double mx,double my,int button) {
        if(!RecipeViewer.available()||(button!=0&&button!=1))return false;
        for(var cell:cells)if(cell.contains(mx,my)&&!cell.stack.isEmpty())return RecipeViewer.open(cell.stack,button==1);
        return false;
    }
}
