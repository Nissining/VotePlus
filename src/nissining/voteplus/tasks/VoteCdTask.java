package nissining.voteplus.tasks;

import nissining.voteplus.VotePlus;

/**
 * @author Nissining
 **/
public class VoteCdTask extends VoteTask {

    public VoteCdTask(VotePlus votePlus) {
        super(votePlus);
    }

    @Override
    public void run() {
        votePlus.voteData = null;
        votePlus.voteCdTask = null;
    }

}
