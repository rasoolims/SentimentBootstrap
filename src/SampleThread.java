import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Created by Mohammad Sadegh Rasooli.
 * ML-NLP Lab, Department of Computer Science, Columbia University
 * Date Created: 5/23/17
 * Time: 4:56 PM
 * To report any bugs or problems contact rasooli@cs.columbia.edu
 */

public class SampleThread implements Callable<Double> {
    public ArrayList<String> goldLabs;
    public ArrayList<String> p1Labs;
    public ArrayList<String> p2Labs;
    int sampleSize;
    Random rand;

    public SampleThread(ArrayList<String> goldLabs, ArrayList<String> p1Labs, ArrayList<String> p2Labs, int sampleSize, Random rand) {
        this.goldLabs = goldLabs;
        this.p1Labs = p1Labs;
        this.p2Labs = p2Labs;
        this.sampleSize = sampleSize;
        this.rand = rand;
    }

    @Override
    public Double call() throws Exception {
        return Bootstrap.getSampleDiff(rand, goldLabs, p1Labs, p2Labs, goldLabs.size(), sampleSize);
    }
}
