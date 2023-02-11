package nissining.voteplus;

import cn.nukkit.Server;

import java.util.Optional;

/**
 * @author Nissining
 */
public class VotePlayer {

    private final String name;
    /**
     * 投票结果
     * 0=反对 1=同意
     */
    private Integer voteResult;
    /**
     * 投票结果次数
     */
    private Integer voteCount = 0;

    public VotePlayer(String name, Integer voteResult) {
        this.name = name;
        this.voteResult = voteResult;
    }

    public String getName() {
        return name;
    }

    public Integer getVoteResult() {
        return voteResult;
    }

    public void setVoteResult(Integer voteResult) {
        boolean b = this.voteCount < 1;
        if (b) {
            this.voteResult = voteResult;
            this.voteCount++;
            Optional.ofNullable(Server.getInstance().getPlayer(name))
                    .ifPresent(player -> player.sendMessage(getVoteResultString()));
        }
    }

    public String getVoteResultString() {
        return "§e你 " + (getVoteResult() == 0 ? "§c反对" : "§a同意") + " §e了该投票";
    }

    @Override
    public String toString() {
        return "VotePlayer{" +
                "name='" + name + '\'' +
                ", voteResult=" + voteResult +
                '}';
    }
}
