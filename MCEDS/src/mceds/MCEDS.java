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
    private static boolean DEBUG = false;

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

        if (commandAsList.contains("-debug")) {
            DEBUG = true;
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
        Set<String> allDomains = new TreeSet<>();
        Map<String, TreeMap<Integer, Set<String>>> catalyticSites = new TreeMap<>();
        Map<String, TreeMap<String, Set<String>>> rawMap = new TreeMap<>();
        String thisLine;
        try (BufferedReader bf = new BufferedReader(new FileReader(f))) {
            String header = bf.readLine();
            while ((thisLine = bf.readLine()) != null) {
                String[] split = thisLine.split("\\s+|;");
                String ec = split[0];
                String pdb = split[1];
                List<String> asList = Arrays.asList(split);
                Set<String> data = new TreeSet<>(asList.subList(2, asList.size()));

                if (!rawMap.containsKey(ec)) {
                    rawMap.put(ec, new TreeMap<>());
                }
                rawMap.get(ec).put(pdb, data);
                allDomains.addAll(data);
            }
        } catch (Exception ex) {
            Logger.getLogger(MCEDS.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (DEBUG) {
            System.out.println("Number of PDB entries parsed: " + rawMap.size());
        }

        /*
         Calculating minimum domain combinations in EC
         */
        if (DEBUG) {
            System.out.println("!------------------------------------------------!");
            System.out.println("\tIndex" + "\tEC" + "\tCombinations");
        }
        for (String ec : rawMap.keySet()) {
            TreeMap<String, Set<String>> pdb_cath_combinations = rawMap.get(ec);
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
                        for (String ecAll : rawMap.keySet()) {
                            TreeMap<String, Set<String>> pdb_cath_combinations_all = rawMap.get(ecAll);
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

            if (!catalyticSites.containsKey(ec)) {
                catalyticSites.put(ec, new TreeMap<>());
            }
            /*
             Print Combinations
             */
            if (DEBUG) {
                System.out.println("!------------------------------------------------!");
                combinations.entrySet().stream().forEach((m) -> {
                    System.out.println("\t" + m.getKey() + "\t" + ec + "\t" + m.getValue());
                });
            }
            catalyticSites.get(ec).putAll(combinations);
        }

        /*
         Update and fill map of common domains
         */
        final Map<String, TreeMap<String, Set<String>>> refinedMCEDSMap = new TreeMap<>();
        Set<String> uniqueDomians = new TreeSet<>();
        rawMap.keySet().stream().map((ec) -> {
            if (!refinedMCEDSMap.containsKey(ec)) {
                refinedMCEDSMap.put(ec, new TreeMap<>());
            }
            return ec;
        }).forEach((ec) -> {
            TreeMap<String, Set<String>> pdb2cathCodes = rawMap.get(ec);
            pdb2cathCodes.keySet().stream().forEach((pdbCode) -> {
                Set<String> rawCathcodes = pdb2cathCodes.get(pdbCode);
                TreeMap<Integer, Set<String>> cataticDomainMaps = catalyticSites.get(ec);
                cataticDomainMaps.values().stream().forEach((cathDomains) -> {
                    TreeSet<String> commonDomains = new TreeSet<>(cathDomains);
                    commonDomains.retainAll(rawCathcodes);
                    /*
                     Find the common min domains presence
                     */
                    if (!commonDomains.isEmpty() && commonDomains.size() == cathDomains.size()) {
                        //System.out.println("\t" + ec + "\t" + pdbCode + "\t" + commonDomains);
                        if (!refinedMCEDSMap.get(ec).containsKey(pdbCode)) {
                            refinedMCEDSMap.get(ec).put(pdbCode, new TreeSet<>());
                        }
                        refinedMCEDSMap.get(ec).get(pdbCode).addAll(commonDomains);
                        uniqueDomians.addAll(commonDomains);
                    }
                });
            });
        });

        catalyticSites.clear();

        System.out.println("Total Input Domains Found: " + allDomains.size());
        System.out.println("Total Unique Domains Found: " + uniqueDomians.size());
        System.out.println("Total Confusion Domains Found: " + (allDomains.size() - uniqueDomians.size()));

        System.out.println("!------------------------------------------------!");
        System.out.println("\t\"EC\"" + "\t\"PDB\"" + "\t\"DOMAINS\"" + "\t\"MDC\"");
        System.out.println("!------------------------------------------------!");

        refinedMCEDSMap.keySet().stream().forEach((String ec) -> {
            TreeMap<String, Set<String>> map = refinedMCEDSMap.get(ec);
            map.keySet().stream().forEach((String pdbCode) -> {
                Set<String> commonCATHDomains = map.get(pdbCode);
                Set<String> allDomainsForPDB = rawMap.get(ec).get(pdbCode);
                System.out.println("\t" + ec + "\t" + pdbCode + "\t" + allDomainsForPDB + "\t" + commonCATHDomains);
            });
        });

    }

}
