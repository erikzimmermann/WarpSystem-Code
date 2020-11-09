package net.nitrado.warpsystem.utils.warpgui.pages;

import de.codingair.codingapi.player.gui.inventory.v2.GUI;

public class ServerPage extends PanelPage {
    private static final String[] SKULLS = {
            "cb7e589138bb8e7cababc23b62d312a369cc34b7f4ca4154d87b2de101a8c4d",
            "2d82af7f62ff1c900a563cb0bee80a534a7f39aad19d0cc1ac9b4f7e6ccd025b",
            "347fda5947418f791475a1a4de4a47d539fa6bd5f428a3e3d48b8624ad1557",
            "6d39df5aa66b122ec60344b37a6cba8f7c69b938cc2bfe73242898e5c9ab3ebe",
            "855fad012c8844fc313510436b4919e002d93d6a0761f7bdaf78069683eee5e6",
            "9e55b4c3653193b2ba68392e280ad72147d8eab1b1a48cf97fe9acf4242638",
            "de5c4e52de3a38cd87bcff48668a55cbbae506c9a97e5d8df6f5975e74427",
            "19cab55f57a39cccad5e90f31603c0696bc8ce341b520bd01e00a8723a6c4381",
            "bc45c8a5048808bf397e29af1dc7285a23e86c3b1987ba3a442bf23f1af10215",
            "6b84208e4be2233fcd96e7f02d9613d5437f1d006236277c57d40652c5be2f23",
            "92b4444749677f07d2cf216428745db78d2e2ce6f362a0e3961f99f17a1884ef",
            "289f6f778fd870a4be0dfdd88b5c278ff47c195231323cecc9b15c06357359a2",
            "c2dd3e8beb78c2a35e6a96a4c677ccfae42697b16afa8f52be68b7a4324c66c",
            "3499d90dac0cf695f3b1dea4fdd988c992d7a894488a115a268f380fb97d0299",
            "976cc31029c010e56791a9991cfbac8684d268103a5a93ba732408fd0590adbe",
            "1546959556eff4b8827ca50d8df686e8f20d5c843da689dddc48bff0a217efbe",
            "4e32e95f9d6e0ed3fb929eaa897ff81d9f5adb8f5ba9b5c2aac98e5446ffbfc",
            "272e4673a53268afc36d7a327dc257b927a82218ae15596f13c62b7037dcf",
            "272cad3786fa4c83ffab91929dfdfcdc568e43d221751665a7e309489295055",
            "576779c1a3d8e0c817dab1c371ff739d7894c018ca404abc7154ace5faed23df",
            "81fa895abc74002d5309540561ae8326a001d876c1c8fc84ea95797796efb6a7",
    };

    public ServerPage(GUI gui, SwitchPage switchPage) {
        super(gui, switchPage);
    }

    @Override
    public void buildItems() {
        for(int i = 0; i < 3; i++) {
            for(int j = 1; j <= 7; j++) {
                int id = i * 7 + j;

                addButton(j, i + 2, new PanelPage.PanelButton("Event", SKULLS[id - 1], id, "event" + id, gui.getPlayer()));
            }
        }
    }
}
