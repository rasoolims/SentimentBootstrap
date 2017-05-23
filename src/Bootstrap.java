import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Mohammad Sadegh Rasooli.
 * ML-NLP Lab, Department of Computer Science, Columbia University
 * Date Created: 5/23/17
 * Time: 4:35 PM
 * To report any bugs or problems contact rasooli@cs.columbia.edu
 */

public class Bootstrap {

    public static double fScore(ArrayList<String> g_lab, ArrayList<String> p_lab) {
        HashMap<String, Integer> tp = new HashMap<String, Integer>();
        HashMap<String, Integer> fp = new HashMap<String, Integer>();
        HashMap<String, Integer> fn = new HashMap<String, Integer>();
        HashSet<String> labels = new HashSet<String>();
        for (int i = 0; i < g_lab.size(); i++) {
            String p = p_lab.get(i);
            String g = g_lab.get(i);
            labels.add(g);
            labels.add(p);
            if (!tp.containsKey(p)) {
                tp.put(p, 0);
                fp.put(p, 0);
                fn.put(p, 0);
            }
            if (!tp.containsKey(g)) {
                tp.put(g, 0);
                fp.put(g, 0);
                fn.put(g, 0);
            }

            if (p.equals(g)) {
                tp.put(g, tp.get(g) + 1);
            } else {
                fn.put(g, fn.get(g) + 1);
                fp.put(p, fp.get(p) + 1);
            }
        }

        double mf = 0;
        for (String lab : labels) {
            double p = (tp.get(lab) + fp.get(lab)) > 0 ? 100.0 * tp.get(lab) / (tp.get(lab) + fp.get(lab)) : 0;
            double r = (tp.get(lab) + fn.get(lab)) > 0 ? 100.0 * tp.get(lab) / (tp.get(lab) + fn.get(lab)) : 0;
            double f = (p + r) > 0 ? (2. * p * r) / (p + r) : 0;
            mf += f;
        }
        return mf / labels.size();

    }

    public static ArrayList<String> ReadLabels(String filePath) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        ArrayList<String> labels = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            labels.add(line.trim().split("\t")[1]);
        }
        return labels;
    }


    public static void main(String[] args) throws Exception {
        Random rand = new Random();
        if(args.length<4){
            System.out.println("gold_file model1_output model2_output numOfThreads");
            System.exit(0);
        }

        ArrayList<String> g_lab = ReadLabels(args[0]);
        ArrayList<String> p1_lab = ReadLabels(args[1]);
        ArrayList<String> p2_lab = ReadLabels(args[2]);
        int numOfThreads = Integer.parseInt(args[3]);

        int b = 1000000;
        int l = g_lab.size();
        int sample_size = Math.max(l / 3, 1);

        double orig_f_1 = fScore(g_lab, p1_lab);
        double orig_f_2 = fScore(g_lab, p2_lab);

        double df = orig_f_1 - orig_f_2;
        System.out.println("original difference " + df);
        double two_delta = 2 * df;

        ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);
        CompletionService<Double> pool = new ExecutorCompletionService<Double>(executor);

        double s = 0;
        for (int i = 0; i < b; i++) {
            pool.submit(new SampleThread(g_lab,p1_lab, p2_lab,sample_size, rand));


        }
        for(int i=0;i<b;i++){
            double diff = pool.take().get();
            if (diff > two_delta) s += 1;
            if (i % 1000 == 0)
                System.out.print(i + "...");
        }
        System.out.print("\n");

        System.out.println(s / b);
    }

    public static double getSampleDiff(Random rand, ArrayList<String> g_lab, ArrayList<String> p1_lab, ArrayList<String> p2_lab, int l, int sample_size) {
        ArrayList<String> g_sample = new ArrayList<String>();
        ArrayList<String> p1_sample = new ArrayList<String>();
        ArrayList<String> p2_sample = new ArrayList<String>();
        for (int j = 0; j < sample_size; j++) {
            int r = rand.nextInt(l);
            g_sample.add(g_lab.get(r));
            p1_sample.add(p1_lab.get(r));
            p2_sample.add(p2_lab.get(r));
        }

        double f_1 = fScore(g_sample, p1_sample);
        double f_2 = fScore(g_sample, p2_sample);
        return f_1 - f_2;
    }
}
