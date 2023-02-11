package nissining.voteplus.ecoAPI;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.LoginChainData;
import nissining.voteplus.VotePlus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class EcoPlayer {

    public static HashMap<String, EcoPlayer> players = new HashMap<>();

    public static void set(String n, Config config) {
        Player exact = Server.getInstance().getPlayerExact(
                n.replaceAll("_", " ")
        );
        if (exact == null)
            return;
        players.put(n, new EcoPlayer(exact, config));
    }

    public static EcoPlayer get(String n) {
        Player playerExact = Server.getInstance().getPlayerExact(
                n.replaceAll("_", " ")
        );
        if (playerExact == null) {
            return null;
        }
        return players.getOrDefault(playerExact.getName(), null);
    }

    public static boolean remove(String n) {
        EcoPlayer ecoPlayer = get(n);
        if (ecoPlayer != null) {
            players.remove(n);
            return true;
        }
        return false;
    }

    protected final Player player;
    private final Config config;
    private final ConfigSection configSection;
    private final LoginChainData loginChainData;

    public EcoPlayer(Player player, Config config) {
        this.player = player;
        this.config = config;
        this.configSection = config.getRootSection();
        this.loginChainData = player.getLoginChainData();
        checkBan();
        checkResetVote();
    }

    public void set(int money) {
        configSection.set("money", money);
    }

    public int get() {
        return configSection.getInt("money", 0);
    }

    public void reduce(int money) {
        set(Math.max(0, get() - money));
    }

    public void add(int money) {
        set(get() + money);
    }

    public int getVote() {
        return configSection.getInt("vote", 0);
    }

    public void addVote() {
        configSection.set("vote", getVote() + 1);
        if (configSection.getString("voteResetTime").equalsIgnoreCase("0"))
            configSection.set("voteResetTime", getDataFormatByDay(new Date().getTime()));
    }

    public void resetVote() {
        configSection.set("vote", 0);
    }

    public void checkResetVote() {
        String now = getDataFormatByDay(new Date().getTime());
        String before = configSection.getString("voteResetTime");
        if (!before.equalsIgnoreCase("0") && !now.equalsIgnoreCase(before)) {
            resetVote();
            configSection.set("voteResetTime", "0");
            player.sendMessage("已重置投票次数！");
        }
    }

    public void checkBan() {
        boolean ban = configSection.getBoolean("ban");

        if (ban) {
            // 如果没有xboxid
            if (loginChainData.isXboxAuthed() &&
                    loginChainData.getXUID().equals(configSection.getString("banXboxId"))) {
                VotePlus.debug(player.getName() + "通过XboxId登录！检查封禁中...");
            } else {
                VotePlus.debug(player.getName() + "未通过XboxId登录！跳过XboxId检查");
            }

            long now = new Date().getTime();
            long after = config.getLong("banTime");

            ban = now < after;

            if (ban) {
                player.kick("你被封禁了！解除时间为：" + getDataFormat(after));
            } else {
                configSection.set("ban", false);
                configSection.set("banTime", 0);
                save();
                VotePlus.debug(player.getName() + "解除封禁！原因： 已到解除时间");
            }
        }

    }

    public boolean setBan(int day, int sec) {
        if (configSection.getBoolean("ban")) {
            return false;
        }
        configSection.set("ban", true);
        configSection.set("banXboxId", loginChainData.getXUID());
        configSection.set("banTime", getDateAfter(day, sec));
        save();
        checkBan();
        return true;
    }

    public Long getDateAfter(int day, int sec) {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.set(Calendar.DATE, now.get(Calendar.DATE) + day);
        now.set(Calendar.SECOND, now.get(Calendar.SECOND) + sec);
        return now.getTime().getTime();
    }

    public String getDataFormat(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        return format.format(new Date(time));
    }

    public String getDataFormatByDay(long time) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy:MM:dd");
        return sf.format(time);
    }

    public void save() {
        config.setAll(configSection);
        config.save();
    }

}
