package nissining.voteplus.menu;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import nissining.voteplus.VotePlus;
import nissining.voteplus.utils.MyForm;

/**
 * @author Nissining
 **/
public class VoteMenu {

    private final VotePlus votePlus;

    public VoteMenu(VotePlus votePlus) {
        this.votePlus = votePlus;
    }

    public void openMenu(Player player) {
        FormWindowSimple f = new FormWindowSimple("Vote Menu", "请选择下面玩家进行投票操作");

        Server.getInstance().getOnlinePlayers().values().forEach(
                player1 -> f.addButton(new ElementButton(player1.getName()))
        );

        MyForm myForm = new MyForm(player, f) {
            @Override
            public void call() {
                if (wasClosed()) {
                    return;
                }
                sureMenu(player, Server.getInstance().getPlayerExact(getButtonText()));
            }
        };
        myForm.sendToPlayer(player);
    }

    private void sureMenu(Player player, Player target) {
        FormWindowCustom f = new FormWindowCustom("Vote Menu");
        f.addElement(new ElementDropdown("请选择发起原因", votePlus.getVoteReasons()));

        MyForm myForm = new MyForm(player, f) {
            @Override
            public void call() {
                if (wasClosed()) {
                    openMenu(player);
                    return;
                }
                String reason = getChooseData();
                votePlus.startVote(player, target, reason);
            }
        };
        myForm.sendToPlayer(player);
    }

}
