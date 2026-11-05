package com.rzy.dealt_force_skills.client;
import com.rzy.dealt_force_skills.mail.MailboxManager;
/** Updated by mailbox snapshots even when the mailbox is closed. */
public final class ClientMailboxState {
    private static int total,unread;
    private ClientMailboxState() { }
    public static void update(MailboxManager.Page page){total=page.total();unread=page.unread();}
    public static int total(){return total;}
    public static int unread(){return unread;}
    public static void clear(){total=0;unread=0;}
}
