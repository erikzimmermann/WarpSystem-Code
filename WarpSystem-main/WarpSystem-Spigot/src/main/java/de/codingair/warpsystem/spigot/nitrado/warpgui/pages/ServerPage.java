package de.codingair.warpsystem.spigot.nitrado.warpgui.pages;

import de.codingair.codingapi.player.gui.inventory.v2.GUI;
import de.codingair.codingapi.player.gui.inventory.v2.Page;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Button;
import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.warpsystem.base.transfer.packets.spigot.utils.ServerPing;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ServerPage extends Page {
    private static final String[] SKULLS = {
            "105b356be68ac56cfe0611b95027adf1ee67050e4cd66a0a99678fc2e25b6b0f",
            "2d82af7f62ff1c900a563cb0bee80a534a7f39aad19d0cc1ac9b4f7e6ccd025b",
            "538a304555a9c1e70676fd09e2c072a68918c78f070e53ded0cd59d3c34c8d7",
            "ca4e7cdd3f87c3f44e1a6df78f1ffb7542a2b0e336a8b7e27e11a9e68a7f8999",
            "56585d7841d05f89375631564fca774b3edefe86af2b6ae39f3baecebb549",
            "c0400111ae026101077b59d8482c84a832c74f4f2105fc13b7c94f5b8000ea3",
            "a6c4710595ec3c70362d9bf20583c88f80b9daccb2182304d0f2fd59b7fdb435",
            "19cab55f57a39cccad5e90f31603c0696bc8ce341b520bd01e00a8723a6c4381",
            "bc45c8a5048808bf397e29af1dc7285a23e86c3b1987ba3a442bf23f1af10215",
            "ef8a5cc77f9880eddb546bfa1fb9de2ac7d4ac7ce64a7fa0477865b7443de4",
            "4e1db50d492250359bad5f4af41bd5c48ab80f649d1103d22c0457a8c18f1",
            "7dc1330b302e800c31c8dfefbd1b996016c34649f3a396749d3e048637364aad",
            "b58f62684c8fd2e1b2dcbc25614df01bd53f5450c48b0853ac242fe94e964466",
            "3499d90dac0cf695f3b1dea4fdd988c992d7a894488a115a268f380fb97d0299",
            "976cc31029c010e56791a9991cfbac8684d268103a5a93ba732408fd0590adbe",
            "462320f312fa993080238f460698603b998464e6ec444fbb285f488ff7b5e455",
            "645731d7605b895372d41f7791455a93350ab6f74d59e6d8582668ebcba7",
            "9216b42018004a86081b5c1ee87dc8f12770f1859266e2cd359b75b44b5e7680",
            "9ab1313246323c989db030b90313d054cce09f67548c985936308abc2a0ff2a2",
            "d5831efc9574b5dbf27e61a8c340939a335f53944f72750ce3c0ebff2a6733b1",
            "d4b11b1d2fdd7dd8b89f6c9f732d5b7aa456a49bd156687bc8fe892b6dbfb20a"
    };

    private static final String QUESTION = "b4d7cc4dca986a53f1d6b52aaf376dc6acc73b8b287f42dc8fef5808bb5d76";

    public ServerPage(GUI gui, Page basic) {
        super(gui, basic);
        setTitle("Teleporter - §5§lEvent Server");
    }

    @Override
    public void buildItems() {
        int id = 0;
        for(int i = 0; i < 3; i++) {
            addButton(3 + i, 0, new PanelButton("Event", SKULLS[id++], id, "event" + (id < 10 ? "0" : "") + id, gui.getPlayer()));
        }

        for(int j = 0; j < 3; j++) {
            for(int i = 0; i < 5; i++) {
                addButton(2 + i, 1 + j, new PanelButton("Event", SKULLS[id++], id, "event" + (id < 10 ? "0" : "") + id, gui.getPlayer()));
            }
        }

        for(int i = 0; i < 3; i++) {
            addButton(3 + i, 4, new PanelButton("Event", SKULLS[id++], id, "event" + (id < 10 ? "0" : "") + id, gui.getPlayer()));
        }

        String server = WarpSystem.getInstance().getCurrentServer();
        String name = server.split("[0-9]", -1)[0];
        server = name + " " + server.replace(name, "");
        server = server.substring(0, 1).toUpperCase() + server.substring(1).toLowerCase();
        String finalServer = server;

        ServerPing ping = WarpSystem.getInstance().getServerManager().getProperties(WarpSystem.getInstance().getCurrentServer());
        addButton(8, 2, new Button() {
            @Override
            public ItemStack buildItem() {
                return new ItemBuilder(QUESTION).setName("§7Du bist hier: §e" + finalServer).addLore("§7Status: §aOnline").addLore("§7Spieler: " + (ping == null ? "§7?" : ((PanelButton.isFull(ping) ? "§c" : "§a") + ping.getPlayers())) + "§8/§770").getItem();
            }

            @Override
            public boolean canClick(ClickType type) {
                return false;
            }

            @Override
            public void onClick(GUI gui, InventoryClickEvent e) {
            }
        });
    }
}
