package nissining.voteplus.tasks;

import nissining.voteplus.VotePlus;

public class VoteTask extends Thread {

    public final VotePlus votePlus;

    public VoteTask(VotePlus votePlus) {
        this.votePlus = votePlus;
    }

    @Override
    public void run() {
        while (votePlus.voteData != null) {
            try {
                votePlus.voteData.task();
                sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
