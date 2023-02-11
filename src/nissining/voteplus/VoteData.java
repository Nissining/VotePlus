package nissining.voteplus;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.BossBarColor;
import cn.nukkit.utils.DummyBossBar;
import cn.nukkit.utils.TextFormat;
import nissining.voteplus.tasks.VoteCdTask;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Nissining
 **/
public class VoteData {

    public int minOver;
    /**
     * 投票结束时间
     */
    public int maxOver;
    /**
     * 发起人
     */
    public Player orig;
    /**
     * 目标
     */
    public Player target;
    /**
     * 发起原因
     */
    public String reason;

    public static final HashMap<String, DummyBossBar> BOSS_BAR = new HashMap<>();
    public static final Server SERVER = Server.getInstance();

    public LinkedList<VotePlayer> votePlayers = new LinkedList<>();

    public final VotePlus votePlus;

    public VoteData(Player orig, Player target, String reason, int maxOver, VotePlus votePlus) {
        this.orig = orig;
        this.target = target;
        this.reason = reason;
        this.minOver = maxOver;
        this.maxOver = maxOver;
        this.votePlus = votePlus;
    }

    public VotePlayer getVotePlayer(String name) {
        return votePlayers.stream()
                .filter(votePlayer -> votePlayer.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void addVotePlayer(VotePlayer votePlayer) {
        if (Objects.isNull(getVotePlayer(votePlayer.getName()))) {
            this.votePlayers.add(votePlayer);
            Optional.ofNullable(Server.getInstance().getPlayer(votePlayer.getName()))
                    .ifPresent(p->p.sendMessage("§e请在聊天栏输入数字！0=§c反对 §e1=§a同意"));
        }
    }

    public void fixVote(String name, Integer result) {
        Optional.ofNullable(getVotePlayer(name))
                .ifPresent(votePlayer -> votePlayer.setVoteResult(result));
    }

    public int getWinResultCount() {
        return (int) votePlayers.stream()
                .filter(votePlayer -> votePlayer.getVoteResult() == 1)
                .count();
    }

    public int getLostResultCount() {
        return (int) votePlayers.stream()
                .filter(votePlayer -> votePlayer.getVoteResult() == 0)
                .count();
    }

    public String getVoteResult() {
        String cmdK;
        int result = Integer.compare(getWinResultCount(), getLostResultCount());
        if (result == -1) {
            cmdK = "§c反对";
        } else if (result == 1) {
            cmdK = "§a同意";
        } else {
            cmdK = "§b双方一致";
        }
        return cmdK;
    }

    public String getVoteResultInfo() {
        return "同意: " + getWinResultCount() + "        反对: " + getLostResultCount();
    }

    public String getVoteResultText() {
        StringJoiner sj = new StringJoiner("\n", "本次投票结果为: " + getVoteResult(), "");
        sj.add("");
        sj.add("");
        sj.add(getVoteResultInfo());
        return sj.toString();
    }

    public void task() {
        if (minOver == -5) {
            // 如果发起人不在白名单，进入冷却
            if (!votePlus.isInWhiteList(orig.getName())) {
                int cd = votePlus.config.getInt("投票冷却时间");
                VoteCdTask voteCdTask = new VoteCdTask(votePlus);
                VotePlus.EXECUTOR.schedule(voteCdTask, cd, TimeUnit.SECONDS);
            } else {
                votePlus.voteData = null;
            }
            BOSS_BAR.values().forEach(DummyBossBar::destroy);
            BOSS_BAR.clear();
            return;
        }
        minOver--;

        if (minOver == 0) {
            // 结束投票
            votePlus.overVote();
            // 显示投票结果
            BOSS_BAR.values().forEach(dummyBossBar -> {
                dummyBossBar.setLength(100f);
                dummyBossBar.setColor(BossBarColor.GREEN);
                dummyBossBar.setText(getVoteResultText());
            });
            SERVER.broadcastMessage(getVoteResultText());
        }

        if (minOver > 0) {
            SERVER.getOnlinePlayers().values().forEach(player -> {
                String color = minOver % 2 == 0 ? TextFormat.RED.toString() : TextFormat.WHITE.toString();
                float time = Math.max(0, minOver * 100f / maxOver);
                color = color + votePlus.getVoteStatus();

                if (BOSS_BAR.containsKey(player.getName())) {
                    DummyBossBar dummyBossBar = BOSS_BAR.get(player.getName());
                    dummyBossBar.setColor(BossBarColor.PURPLE);
                    dummyBossBar.setLength(time);
                    dummyBossBar.setText(color);
                } else {
                    BOSS_BAR.put(
                            player.getName(),
                            votePlus.creBossPar(player, color, time)
                    );
                }
            });
        }

    }

}
