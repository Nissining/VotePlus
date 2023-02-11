package nissining.voteplus;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.*;
import nissining.voteplus.ecoAPI.EcoPlayer;
import nissining.voteplus.menu.VoteMenu;
import nissining.voteplus.tasks.VoteCdTask;
import nissining.voteplus.tasks.VoteTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VotePlus extends PluginBase implements Listener {

    public VoteMenu menu;
    public Config config;

    public static Executor executor = Executors.newWorkStealingPool();

    public VoteData voteData;
    public VoteCdTask voteCdTask;

    public static String[] modes = new String[]{
            "§a同意", "§c反对"
    };

    @Override
    public void onEnable() {
        if (!getDataFolder().mkdirs()) {
            debug("VotePlus Enabled!");
        }

        config = new Config(getDataFolder() + "/config.yml", 2, new ConfigSection() {{
            put("投票冷却时间", 60); // sec
            put("投票结束时间", 60); // sec
            put("未投票玩家默认结果", 1); // 0=赞成 1=反对
            put("同意该投票触发指令", new ArrayList<String>() {{
                add("kill %target");
            }});
            put("反对该投票触发指令", new ArrayList<String>() {{
                add("kill %target");
                add("give %player 1 1");
            }});
            put("投票状态消息",
                    "正在对玩家 %target 进行投票操作！/vote <y=同意 n=反对>\n\n" +
                            "发起原因： %reason\n" +
                            "发起人： %player\n" +
                            "同意： %win                          反对： %lost"
            );
            put("玩家投票限制次数", 3);
            put("投票发起原因", new ArrayList<String>() {{
                add("恶意游戏行为");
                add("不文明发言");
                add("妨碍其他玩家正常游戏");
            }});
            put("白名单", new ArrayList<String>());
            put("黑名单", new ArrayList<String>());
        }});

        this.menu = new VoteMenu(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    public Config getPlayerConfig(Player player) {
        File file = new File(getDataFolder(), "players/" + player.getName());
        return new Config(file, 2, new ConfigSection() {{
            put("money", 0);
            put("ban", false);
            put("banXboxId", "0");
            put("banTime", 0);
            put("vote", 0);
            put("voteResetTime", "0");
        }});
    }

    public List<String> getVoteReasons() {
        return config.getStringList("投票发起原因");
    }

    public int getMaxVoteCount() {
        return config.getInt("玩家投票限制次数");
    }

    public boolean isIn(String n, boolean wl) {
        // 检查白名单
        if (wl) {
            return config.getStringList("白名单").contains(n);
        } else {
            return config.getStringList("黑名单").contains(n);
        }
    }

    public boolean isInWhiteList(String n) {
        return config.getStringList("白名单").contains(n);
    }

    public boolean isInBlackList(String n) {
        return config.getStringList("黑名单").contains(n);
    }

    public void startVote(Player originator, Player target, String reason) {
        // 服务器小于两人不能进行投票
        if (getServer().getOnlinePlayers().values().size() < 3 && !originator.isOp()) {
            originator.sendMessage("§c无法发起投票！人数不足！");
            return;
        }

        // 发起次数限制
        EcoPlayer ecoPlayer = EcoPlayer.get(originator.getName());
        if (ecoPlayer == null || ecoPlayer.getVote() >= getMaxVoteCount() && !isInWhiteList(originator.getName())) {
            originator.sendMessage("§c发起失败！投票次数过多！");
            return;
        }
        // 黑名单
        if (isInBlackList(originator.getName())) {
            originator.sendMessage("§c发起失败！你在黑名单中！");
            return;
        }

        if (voteData != null) {
            originator.sendMessage("§c当前还有投票正在进行中或未冷却完毕！");
            return;
        }

//        originator.sendMessage("§a开始对玩家 " + target.getName() + " 进行投票操作！");
        getServer().broadcastMessage("§a玩家 §f" + originator.getName() + " §a对玩家§f " + target.getName() + " §a发起强制退场！");
        // 创建投票
        voteData = new VoteData(
                originator,
                target,
                reason,
                config.getInt("投票结束时间"),
                this
        );
        // 发起人加入同意
        tryVote(originator, 0);
        // 发起人投票次数+1
        // 白名单不受影响
        if (!isInWhiteList(originator.getName())) {
            ecoPlayer.addVote();
        }
        // 投票结束倒计时
        executor.execute(() -> new VoteTask(this).start());
    }

    public void checkVote() {
        if (voteData == null) {
            return;
        }
        int mode = config.getInt("未投票玩家默认结果");
        // 没有参与投票默认投票
        getServer().getOnlinePlayers().values().stream()
                .filter(player -> !voteData.checkVote(player.getName()))
                .forEach(player -> {
                    voteData.vote(player.getName(), mode);
                    player.sendMessage("你未进行投票！默认投票为： " + modes[mode]);
                });
        // 更新投票结果
        voteData.updateResult();
    }

    public void overVote() {
        checkVote();
        disCmd();
    }

    public void disCmd() {
        String cmdK;
        if (voteData.result < 2) {
            if (voteData.result == 0) {
                cmdK = "同意";
            } else {
                cmdK = "反对";
            }
            config.getStringList(cmdK + "该投票触发指令").forEach(s -> getServer().dispatchCommand(
                    getServer().getConsoleSender(),
                    s.replaceAll("%target", "\"" + voteData.target.getName() + "\"")
            ));
        }
    }

    public String getVoteResultMsg() {
        String[] ss = {
                "§a已同意该投票操作",
                "§c已反对该投票操作",
                "§b同意和反对一致，投票操作失效"
        };
        StringJoiner sj = new StringJoiner("\n", ss[voteData.getVoteResult()], "");
        sj.add("").add("").add(voteData.getVoteResultSocre());
        return sj.toString();
    }

    public String getVoteStatus() {
        if (voteData == null) {
            return "发生错误！当前没有投票！";
        }

        int win = voteData.getWinners();
        int lost = voteData.getLost();
        return config.getString("投票状态消息")
                .replaceAll("%reason", voteData.reason)
                .replaceAll("%target", voteData.target.getName())
                .replaceAll("%player", voteData.orig.getName())
                .replaceAll("%win", String.valueOf(win))
                .replaceAll("%lost", String.valueOf(lost));
    }

    public void tryVote(Player player, int mode) {
        String t;
        if (voteData == null) {
            t = "§c当前没有发起投票！";
        } else {
            if (!voteData.checkVote(player.getName())) {
                voteData.vote(player.getName(), mode);
                t = "你已" + modes[mode] + "§f该投票！";
            } else {
                t = "§c你已参与这次投票了！";
            }
        }
        player.sendMessage(t);
    }

    public void cancel(Player player) {
        if (voteData != null && voteData.orig.getName().equalsIgnoreCase(player.getName())) {
            VoteData.dummyBossBars.values().forEach(DummyBossBar::destroy);
            VoteData.dummyBossBars.clear();
            voteData = null;
            player.sendMessage("§a你已取消当前投票操作！");
            return;
        }
        player.sendMessage("§c当前没有正在投票操作或你不是该投票的发起人！");
    }

    public DummyBossBar creBossPar(Player player, String text, float health) {
        DummyBossBar.Builder builder = new DummyBossBar.Builder(player);
        builder.text(text);
        builder.length(health);
        builder.color(BossBarColor.PURPLE);

        DummyBossBar build = builder.build();
        build.create();

        return build;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String t = "";
        switch (command.getName()) {
            case "vote":
                // UI
                if (args.length == 0 && sender instanceof Player) {
                    Player player = (Player) sender;
                    menu.openMenu(player);
                    return true;
                }
                // /vote <y/n>
                if (args.length == 1) {
                    Player player = (Player) sender;
                    switch (args[0]) {
                        case "y":
                        case "yes":
                            tryVote(player, 0);
                            break;
                        case "n":
                        case "no":
                            tryVote(player, 1);
                            break;
                        case "cancel":
                            cancel(player);
                            break;
                    }
                }
                // vote <wl/bl> <name>
                if (args.length == 2) {
                    String target = "";
                    if (sender instanceof Player) {
                        Player playerExact = getServer().getPlayerExact(args[1].replaceAll("_", ""));
                        if (playerExact != null) {
                            target = playerExact.getName();
                        }
                    } else {
                        target = args[1];
                    }

                    String key = "";
                    int keyId = -1;

                    switch (args[0]) {
                        case "wl":
                            key = "白名单";
                            keyId = 0;
                            break;
                        case "bl":
                            key = "黑名单";
                            keyId = 1;
                            break;
                    }

                    if (keyId != -1) {
                        if (target.isEmpty()) {
                            t = "找不到目标： " + target;
                        } else {
                            List<String> wl = config.getStringList(key);
                            if (isIn(target, (keyId == 0))) {
                                wl.remove(target);
                                t = key + "移除玩家： " + target;
                            } else {
                                wl.add(target);
                                t = key + "添加玩家： " + target;
                            }
                            config.set(key, wl);
                            config.save();
                        }
                    } else {
                        // vote <p> <r>
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            Player exact = getServer().getPlayerExact(args[0]);
                            if (exact == null) {
                                t = "目标 " + args[0] + " 不存在！";
                            } else {
                                startVote(player, exact, args[1]);
                            }
                        }
                    }

                }
                break;
            case "ban":
                if (args.length == 3 && sender.isOp()) {
                    EcoPlayer ecoPlayer = EcoPlayer.get(args[0]);
                    if (ecoPlayer == null) {
                        sender.sendMessage("玩家不存在！");
                        return true;
                    }
                    if (ecoPlayer.setBan(Integer.parseInt(args[1]), Integer.parseInt(args[2]))) {
                        t = "已封禁该玩家！";
                    } else {
                        t = "该玩家封禁中！";
                    }
                }
                break;
            case "addmoney":
                if (args.length == 2 && sender.isOp()) {
                    EcoPlayer ecoPlayer = EcoPlayer.get(args[0]);
                    if (ecoPlayer == null) {
                        sender.sendMessage("玩家： " + args[0] + " 不存在！");
                        return true;
                    }
                    ecoPlayer.add(Integer.parseInt(args[1]));
                    t = "增加玩家： " + args[0] + " 金币: " + args[1];
                } else {
                    t = "权限不足！";
                }
                break;
            case "reducemoney":
                if (args.length == 2 && sender.isOp()) {
                    EcoPlayer ecoPlayer = EcoPlayer.get(args[0]);
                    if (ecoPlayer == null) {
                        sender.sendMessage("玩家： " + args[0] + " 不存在！");
                        return true;
                    }
                    ecoPlayer.reduce(Integer.parseInt(args[1]));
                    t = "减少玩家： " + args[0] + " 金币: " + args[1];
                }
                break;
        }

        if (!t.isEmpty()) {
            sender.sendMessage(t);
        }

        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (EcoPlayer.get(p.getName()) == null) {
            EcoPlayer.set(p.getName(), getPlayerConfig(p));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        EcoPlayer ecoPlayer = EcoPlayer.get(player.getName());
        if (ecoPlayer != null) {
            ecoPlayer.save();
        }
        EcoPlayer.remove(player.getName());
        if (VoteData.dummyBossBars.containsKey(player.getName())) {
            VoteData.dummyBossBars.get(player.getName()).destroy();
            VoteData.dummyBossBars.remove(player.getName());
        }
    }

    public static void debug(String debug) {
        MainLogger.getLogger().notice(debug);
    }
}
