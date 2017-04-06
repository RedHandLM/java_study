package paxos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * 
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉Paxos算法实现
 *
 * @author shichang.liu
 * @date 2017年4月6日上午10:25:34
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public final class PaxosDemo {

    private static final HashFunction HASH_FUNCTION = Hashing.murmur3_32();
    private static final Random RANDOM = new Random();
    private static final String[] PROPOSALS = { "ProjectA", "ProjectB", "ProjectC" };

    public static void main(String[] args) {
        List<Acceptor> acceptors = new ArrayList<Acceptor>();
        Arrays.asList("A", "B", "C", "D", "E").forEach(name -> acceptors.add(new Acceptor(name)));
        Proposer.vote(new Proposal(1L, null), acceptors);
    }

    private static void printInfo(String subject, String operation, String result) {
        System.out.println(subject + ":" + operation + "<" + result + ">");
    }

    /**
     * 对于提案的约束，第三条约束要求： 如果maxVote不存在，那么没有限制，下一次表决可以使用任意提案； 否则，下一次表决要沿用maxVote的提案
     *
     * @param currentVoteNumber
     * @param proposals
     * @return
     */
    private static Proposal nextProposal(long currentVoteNumber, List<Proposal> proposals) {
        long voteNumber = currentVoteNumber + 1;
        if (proposals.isEmpty())
            return new Proposal(voteNumber, PROPOSALS[RANDOM.nextInt(PROPOSALS.length)]);
        Collections.sort(proposals);
        Proposal maxVote = proposals.get(proposals.size() - 1);
        long maxVoteNumber = maxVote.getVoteNumber();
        String content = maxVote.getContent();
        if (maxVoteNumber >= currentVoteNumber)
            throw new IllegalStateException("illegal state maxVoteNumber");
        if (content != null)
            return new Proposal(voteNumber, content);
        else
            return new Proposal(voteNumber, PROPOSALS[RANDOM.nextInt(PROPOSALS.length)]);
    }

    private static class Proposer {

        /**
         * @param proposal
         * @param acceptors
         */
        public static void vote(Proposal proposal, Collection<Acceptor> acceptors) {
            int quorum = Math.floorDiv(acceptors.size(), 2) + 1;
            int count = 0;
            while (true) {
                printInfo("VOTE_ROUND", "START", ++count + "");
                List<Proposal> proposals = new ArrayList<Proposal>();
                for (Acceptor acceptor : acceptors) {
                    Promise promise = acceptor.onPrepare(proposal);
                    if (promise != null && promise.isAck())
                        proposals.add(promise.getProposal());
                }
                if (proposals.size() < quorum) {
                    printInfo("PROPOSER[" + proposal + "]", "VOTE", "NOT PREPARED");
                    proposal = nextProposal(proposal.getVoteNumber(), proposals);
                    continue;
                }
                int acceptCount = 0;
                for (Acceptor acceptor : acceptors) {
                    if (acceptor.onAccept(proposal))
                        acceptCount++;
                }
                if (acceptCount < quorum) {
                    printInfo("PROPOSER[" + proposal + "]", "VOTE", "NOT ACCEPTED");
                    proposal = nextProposal(proposal.getVoteNumber(), proposals);
                    continue;
                }
                break;
            }
            printInfo("PROPOSER[" + proposal + "]", "VOTE", "SUCCESS");
        }

    }

    private static class Acceptor {

        // 上次表决结果
        private Proposal last = new Proposal();
        private String name;

        public Acceptor(String name) {
            this.name = name;
        }

        public Promise onPrepare(Proposal proposal) {
            // 假设这个过程有50%的几率失败
            if (Math.random() - 0.5 > 0) {
                printInfo("ACCEPTER_" + name, "PREPARE", "NO RESPONSE");
                return null;
            }
            if (proposal == null)
                throw new IllegalArgumentException("null proposal");
            if (proposal.getVoteNumber() > last.getVoteNumber()) {
                Promise response = new Promise(true, last);
                last = proposal;
                printInfo("ACCEPTER_" + name, "PREPARE", "OK");
                return response;
            } else {
                printInfo("ACCEPTER_" + name, "PREPARE", "REJECTED");
                return new Promise(false, null);
            }
        }

        public boolean onAccept(Proposal proposal) {
            // 假设这个过程有50%的几率失败
            if (Math.random() - 0.5 > 0) {
                printInfo("ACCEPTER_" + name, "ACCEPT", "NO RESPONSE");
                return false;
            }
            printInfo("ACCEPTER_" + name, "ACCEPT", "OK");
            return last.equals(proposal);
        }
    }

    private static class Promise {

        private final boolean ack;
        private final Proposal proposal;

        public Promise(boolean ack, Proposal proposal) {
            this.ack = ack;
            this.proposal = proposal;
        }

        public boolean isAck() {
            return ack;
        }

        public Proposal getProposal() {
            return proposal;
        }
    }

    private static class Proposal implements Comparable<Proposal> {

        private final long voteNumber;
        private final String content;

        public Proposal(long voteNumber, String content) {
            this.voteNumber = voteNumber;
            this.content = content;
        }

        public Proposal() {
            this(0, null);
        }

        public long getVoteNumber() {
            return voteNumber;
        }

        public String getContent() {
            return content;
        }

        @Override
        public int compareTo(Proposal o) {
            return Long.compare(voteNumber, o.voteNumber);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof Proposal))
                return false;
            Proposal proposal = (Proposal) obj;
            return voteNumber == proposal.voteNumber && StringUtils.equals(content, proposal.content);
        }

        @Override
        public int hashCode() {
            return HASH_FUNCTION.newHasher().putLong(voteNumber).putString(content, Charsets.UTF_8).hash().asInt();
        }

        @Override
        public String toString() {
            return new StringBuilder().append(voteNumber).append(':').append(content).toString();
        }
    }

}