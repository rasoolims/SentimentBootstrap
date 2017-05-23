import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
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

    public static double fScore(ArrayList<Integer> g_lab, ArrayList<Integer> p_lab) {
        HashMap<Integer, Integer> tp = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> fp = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> fn = new HashMap<Integer, Integer>();
        HashSet<Integer> labels = new HashSet<Integer>();
        for (int i = 0; i < g_lab.size(); i++) {
            int p = p_lab.get(i);
            int g = g_lab.get(i);
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

            if (p==g) {
                tp.put(g, tp.get(g) + 1);
            } else {
                fn.put(g, fn.get(g) + 1);
                fp.put(p, fp.get(p) + 1);
            }
        }

        double mf = 0;
        for (int lab : labels) {
            double p = (tp.get(lab) + fp.get(lab)) > 0 ? 100.0 * tp.get(lab) / (tp.get(lab) + fp.get(lab)) : 0;
            double r = (tp.get(lab) + fn.get(lab)) > 0 ? 100.0 * tp.get(lab) / (tp.get(lab) + fn.get(lab)) : 0;
            double f = (p + r) > 0 ? (2. * p * r) / (p + r) : 0;
            mf += f;
        }
        return mf / labels.size();

    }

    public static ArrayList<Integer> ReadLabels(String filePath,  HashMap<String, Integer> sentimentMap) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        ArrayList<Integer> labels = new ArrayList<Integer>();
        while ((line = reader.readLine()) != null) {
            labels.add(sentimentMap.get(line.trim().split("\t")[1]));
        }
        return labels;
    }


    public static void main(String[] args) throws Exception {
        DecimalFormat dfrm = new DecimalFormat("##0.00");
        Random rand = new Random();
        if(args.length<4){
            System.out.println("gold_file model1_output model2_output numOfThreads");
            System.exit(0);
        }
        HashMap<String, Integer> sentimentMap = new HashMap<String, Integer>();
        sentimentMap.put("neutral",0);
        sentimentMap.put("negative",1);
        sentimentMap.put("positive",2);

        ArrayList<Integer> g_lab = ReadLabels(args[0],sentimentMap);
        ArrayList<Integer> p1_lab = ReadLabels(args[1],sentimentMap);
        ArrayList<Integer> p2_lab = ReadLabels(args[2],sentimentMap);
        int numOfThreads = Integer.parseInt(args[3]);

        int b = 1000000;
        int l = g_lab.size();
        int sample_size = Math.max(l / 3, 1);

        double orig_f_1 = fScore(g_lab, p1_lab);
        double orig_f_2 = fScore(g_lab, p2_lab);

        double df = orig_f_1 - orig_f_2;
        System.out.println("original difference " + dfrm.format(df));
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
            if ((i+1) % 100000 == 0) {
                double progress = 100.0 *(i+1)/b;
                System.out.print(dfrm.format(progress) + "% ("+s+")...last diff: "+dfrm.format(diff) +" ");
            }
        }
        System.out.print("\n");
        System.out.println(s / b);
        boolean isTerminated = executor.isTerminated();
        while (!isTerminated) {
            executor.shutdownNow();
            isTerminated = executor.isTerminated();
        }
    }

    public static double getSampleDiff(Random rand, ArrayList<Integer> g_lab, ArrayList<Integer> p1_lab, ArrayList<Integer> p2_lab, int l, int sample_size) {
        ArrayList<Integer> g_sample = new ArrayList<Integer>();
        ArrayList<Integer> p1_sample = new ArrayList<Integer>();
        ArrayList<Integer> p2_sample = new ArrayList<Integer>();
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
