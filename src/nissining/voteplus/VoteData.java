package nissining.voteplus;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.BossBarColor;
import cn.nukkit.utils.DummyBossBar;
import cn.nukkit.utils.TextFormat;
import nissining.voteplus.tasks.VoteCdTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VoteData {

    public int minOver;
    public int maxOver;
    public Player orig; // 发起人
    public Player target; // 被发起
    public String reason; // 发起原因
    public int result = 0; // 0=同意方 1=反对方 2=平局

    public List<String> winners = new ArrayList<>();
    public List<String> lost = new ArrayList<>();

    public static final HashMap<String, DummyBossBar> dummyBossBars = new HashMap<>();
    public static Server server = Server.getInstance();

    public final VotePlus votePlus;

    public VoteData(Player orig, Player target, String reason, int maxOver, VotePlus votePlus) {
        this.orig = orig;
        this.target = target;
        this.reason = reason;
        this.minOver = maxOver;
        this.maxOver = maxOver;
        this.votePlus = votePlus;
    }

    public int getWinners() {
        return winners.size();
    }

    public int getLost() {
        return lost.size();
    }

    public List<String> getAllVoter() {
        return new ArrayList<String>(winners) {{
            addAll(lost);
        }};
    }

    public boolean checkVote(String n) {
        return getAllVoter().contains(n);
    }

    public void vote(String n, int mode) {
        if (mode == 0) {
            winners.add(n);
        } else {
            lost.add(n);
        }
    }

    public int getVoteResult() {
        int win = winners.size();
        int lost = this.lost.size();
        int i;
        if (win > lost) {
            i = 0;
        } else if (win < lost) {
            i = 1;
        } else {
            i = 2;
        }
        return i;
    }

    public void updateResult() {
        result = getVoteResult();
    }

    // 获取投票结果比分
    public String getVoteResultSocre() {
        return "同意: " + winners.size() + "   反对: " + lost.size();
    }

    public void task() {
        if (minOver == -5) {
            // 如果发起人不在白名单，进入冷却
            if (!votePlus.isInWhiteList(orig.getName())) {
                VotePlus.executor.execute(() -> new VoteCdTask(votePlus).start());
            } else {
                votePlus.voteData = null;
            }
            dummyBossBars.values().forEach(DummyBossBar::destroy);
            dummyBossBars.clear();
            return;
        }
        minOver--;

        if (minOver == 0) {
            // 结束投票
            votePlus.overVote();
            // 显示投票结果
            dummyBossBars.values().forEach(dummyBossBar -> {
                dummyBossBar.setLength(100f);
                dummyBossBar.setColor(BossBarColor.GREEN);
                dummyBossBar.setText(votePlus.getVoteResultMsg());
            });
            server.broadcastMessage(votePlus.getVoteResultMsg());
        }

        if (minOver > 0) {
            server.getOnlinePlayers().values().forEach(player -> {
                String color = minOver % 2 == 0 ? TextFormat.RED.toString() : TextFormat.WHITE.toString();
                float time = Math.max(0, minOver * 100f / maxOver);
                color = color + votePlus.getVoteStatus();

                if (dummyBossBars.containsKey(player.getName())) {
                    DummyBossBar dummyBossBar = dummyBossBars.get(player.getName());
                    dummyBossBar.setColor(BossBarColor.PURPLE);
                    dummyBossBar.setLength(time);
                    dummyBossBar.setText(color);
                } else {
                    dummyBossBars.put(
                            player.getName(),
                            votePlus.creBossPar(player, color, time)
                    );
                }
            });
        }

    }

}
