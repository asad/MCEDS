/*
 The MIT License (MIT)

 Copyright (c) 2015 Syed Asad Rahman <s9asad@gmail.com>

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package mceds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Asad
 */
public class MCEDS {

    private static boolean WITHIN_EC_CLASS = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        List<String> commandAsList = Arrays.asList(args);
        String fileName = null;
        if (commandAsList.contains("-f")) {
            int index = commandAsList.indexOf("-f") + 1;
            fileName = commandAsList.get(index);
        }

        if (commandAsList.contains("-s")) {
            WITHIN_EC_CLASS = true;
        }

        if (fileName != null) {
            MCEDS mceds = new MCEDS(fileName);
        } else {
            System.err.println("java -jar MCEDS.jar -f data/ec5_pdb_cathids.txt -s");
            System.err.println("Note:\t-s for restricting the search within EC classes");
            System.err.println("\tdefault: test against all the ECs present in the input file.");
        }
    }

    private MCEDS(String fileName) {
        File f = new File(fileName);

        Map<String, TreeMap<String, Set<String>>> map = new TreeMap<>();
        String thisLine;
        try (BufferedReader bf = new BufferedReader(new FileReader(f))) {
            String header = bf.readLine();
            while ((thisLine = bf.readLine()) != null) {
                String[] split = thisLine.split("\\s+|;");
                String ec = split[0];
                String pdb = split[1];
                List<String> asList = Arrays.asList(split);
                Set<String> data = new TreeSet<>(asList.subList(2, asList.size()));

                if (!map.containsKey(ec)) {
                    map.put(ec, new TreeMap<>());
                }
                map.get(ec).put(pdb, data);
            }
        } catch (Exception ex) {
            Logger.getLogger(MCEDS.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("map: " + map.size());

        /*
         Calculating minimum domain combinations in EC
         */
        System.out.println("!------------------------------------------------!");
        System.out.println("\tIndex" + "\tEC" + "\tCombinations");

        for (String ec : map.keySet()) {
            TreeMap<String, Set<String>> pdb_cath_combinations = map.get(ec);
            Map<Integer, Set<String>> combinations = new TreeMap<>();

            int counter = 1;
            for (String pdb : pdb_cath_combinations.keySet()) {
                Set<String> common = new TreeSet<>();
                Set<String> domains = pdb_cath_combinations.get(pdb);
                boolean flag = false;

                /*
                 Select Seed Domains
                 */
                for (Set<String> v : combinations.values()) {
                    Set<String> t = new TreeSet<>(v);
                    t.retainAll(domains);
                    if (!t.isEmpty() && t.size() == v.size()) {
                        flag = true;
                        break;
                    }
                }

                /*
                 Selected Successful Seed Domains
                 */
                if (!flag) {
                    common.addAll(domains);
                    if (WITHIN_EC_CLASS) {

                        pdb_cath_combinations.keySet().stream().map((pdbcommon) -> pdb_cath_combinations.get(pdbcommon)).map((domainsComb) -> new TreeSet<>(domainsComb)).map((c) -> {
                            c.retainAll(common);
                            return c;
                        }).filter((c) -> (!c.isEmpty())).forEach((c) -> {
                            common.clear();
                            common.addAll(c);
                        });

                        combinations.put(counter, common);
                        counter++;

                    } else {
                        for (String ecAll : map.keySet()) {
                            TreeMap<String, Set<String>> pdb_cath_combinations_all = map.get(ecAll);
                            pdb_cath_combinations_all.keySet().stream().map((pdbcommon) -> pdb_cath_combinations_all.get(pdbcommon)).map((domainsComb) -> new TreeSet<>(domainsComb)).map((c) -> {
                                c.retainAll(common);
                                return c;
                            }).filter((c) -> (!c.isEmpty())).forEach((c) -> {
                                common.clear();
                                common.addAll(c);
                            });
                        }
                        combinations.put(counter, common);
                        counter++;
                    }
                }

            }

            /*
             Print Combinations
             */
            System.out.println("!------------------------------------------------!");
            combinations.entrySet().stream().forEach((m) -> {
                System.out.println("\t" + m.getKey() + "\t" + ec + "\t" + m.getValue());
            });
        }
    }
}
