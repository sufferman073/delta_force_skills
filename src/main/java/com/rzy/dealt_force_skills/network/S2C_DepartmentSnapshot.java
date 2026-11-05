package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.department.DepartmentTracker;
import com.rzy.dealt_force_skills.department.DepartmentPlatform;
import com.rzy.dealt_force_skills.department.TaskDisplay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public record S2C_DepartmentSnapshot(DepartmentTracker.Page page) implements CustomPacketPayload {
    public static final Type<S2C_DepartmentSnapshot> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills","s2c_departmentsnapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,S2C_DepartmentSnapshot> STREAM_CODEC=StreamCodec.of((buf,msg)->encode(msg,buf),S2C_DepartmentSnapshot::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static void string(RegistryFriendlyByteBuf buf,String value,int max) { buf.writeUtf(value.substring(0,Math.min(max,value.length())),max); }
    public static void encode(S2C_DepartmentSnapshot msg,RegistryFriendlyByteBuf buf) {
        var page=msg.page;string(buf,page.department(),32);buf.writeVarInt(page.page());buf.writeVarInt(page.pages());buf.writeVarInt(page.rows().size());
        for(var row:page.rows()) { string(buf,row.id(),64);string(buf,row.chapter(),256);string(buf,row.title(),128);string(buf,row.status(),16); }
        var detail=page.detail();buf.writeBoolean(detail!=null);if(detail==null)return;
        string(buf,detail.id(),64);string(buf,detail.title(),128);string(buf,detail.brief(),2048);buf.writeUtf(DepartmentPlatform.encodeText(detail.reward()),32767);
        buf.writeVarInt(detail.constraints().size());for(String line:detail.constraints())string(buf,line,1024);
        string(buf,detail.status(),16);buf.writeLong(detail.retryTicks());buf.writeBoolean(detail.repeatable());buf.writeVarInt(detail.goals().size());
        for(var goal:detail.goals()){buf.writeUtf(DepartmentPlatform.encodeText(goal.text()),8192);buf.writeDouble(goal.current());buf.writeDouble(goal.target());TaskDisplay.writeItems(buf,goal.items());}
        buf.writeVarInt(detail.prerequisites().size());
        for(var row:detail.prerequisites()){string(buf,row.id(),64);string(buf,row.chapter(),256);string(buf,row.title(),128);string(buf,row.status(),16);}
        buf.writeBoolean(detail.offerLocked());buf.writeLong(detail.refreshTicks());TaskDisplay.writeRewards(buf,detail.preview());
    }
    private static int count(RegistryFriendlyByteBuf buf,int maximum) {
        int n=buf.readVarInt();if(n<0||n>maximum)throw new io.netty.handler.codec.DecoderException("Invalid department snapshot size");return n;
    }
    public static S2C_DepartmentSnapshot decode(RegistryFriendlyByteBuf buf) {
        String department=buf.readUtf(32);int page=count(buf,10000),pages=count(buf,10000),size=count(buf,DepartmentTracker.PAGE_SIZE);
        List<DepartmentTracker.Row> rows=new ArrayList<>();
        for(int i=0;i<size;i++)rows.add(new DepartmentTracker.Row(buf.readUtf(64),buf.readUtf(256),buf.readUtf(128),buf.readUtf(16)));
        DepartmentTracker.Detail detail=null;
        if(buf.readBoolean()) {
            String id=buf.readUtf(64),title=buf.readUtf(128),brief=buf.readUtf(2048);
            var reward=DepartmentPlatform.decodeText(buf.readUtf(32767));
            List<String> constraints=new ArrayList<>();int n=count(buf,16);for(int i=0;i<n;i++)constraints.add(buf.readUtf(1024));
            String status=buf.readUtf(16);long retry=Math.max(0,buf.readLong());boolean repeatable=buf.readBoolean();
            List<DepartmentTracker.GoalView> goals=new ArrayList<>();n=count(buf,128);
            for(int i=0;i<n;i++) {
                var text=DepartmentPlatform.decodeText(buf.readUtf(8192));double current=buf.readDouble(),target=buf.readDouble();
                if(!Double.isFinite(current)||!Double.isFinite(target)||current<0||target<=0)throw new io.netty.handler.codec.DecoderException("Invalid department progress");
                goals.add(new DepartmentTracker.GoalView(text,Math.min(current,target),target,TaskDisplay.readItems(buf)));
            }
            List<DepartmentTracker.Row> prerequisites=new ArrayList<>();n=count(buf,512);
            for(int i=0;i<n;i++)prerequisites.add(new DepartmentTracker.Row(buf.readUtf(64),buf.readUtf(256),buf.readUtf(128),buf.readUtf(16)));
            detail=new DepartmentTracker.Detail(id,title,brief,reward,List.copyOf(constraints),status,retry,repeatable,List.copyOf(goals),List.copyOf(prerequisites),buf.readBoolean(),Math.max(0,buf.readLong()),TaskDisplay.readRewards(buf));
        }
        return new S2C_DepartmentSnapshot(new DepartmentTracker.Page(department,page,Math.max(1,pages),List.copyOf(rows),detail));
    }
    public static void handle(S2C_DepartmentSnapshot msg,IPayloadContext ctx) {
        ctx.enqueueWork(()->com.rzy.dealt_force_skills.client.screen.DepartmentScreen.accept(msg.page));
    }
}
