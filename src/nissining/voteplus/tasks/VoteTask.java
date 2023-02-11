package nissining.voteplus.tasks;

import nissining.voteplus.VotePlus;

/**
 * @author Nissining
 **/
public class VoteTask implements Runnable {

    public final VotePlus votePlus;

    public VoteTask(VotePlus votePlus) {
        this.votePlus = votePlus;
    }

    @Override
    public void run() {
        if (votePlus.voteData != null) {
            votePlus.voteData.task();
        }
    }

}
