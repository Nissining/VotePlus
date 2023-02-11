package nissining.voteplus.tasks;

import nissining.voteplus.VotePlus;

public class VoteCdTask extends VoteTask {

    public VoteCdTask(VotePlus votePlus) {
        super(votePlus);
    }

    @Override
    public void run() {
        try {
            sleep(votePlus.config.getInt("投票冷却时间") * 1000L);
            votePlus.voteData = null;
            votePlus.voteCdTask = null;
        } catch (Exception e) {
        }
    }

}
