/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mceds;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

/**
 *
 * @author Asad
 */
public class ECPDBDomainIntegrator {

    boolean DEBUG = false;

    final Map<String, TreeMap<String, Set<String>>> rawMap;

    public ECPDBDomainIntegrator() {
        this.rawMap = new TreeMap<>();
    }

    String pdb_interpro = "ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_interpro.tsv.gz";
    String pdb_ec = "ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_enzyme.tsv.gz";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        ECPDBDomainIntegrator ecpdbDomainIntegrator = new ECPDBDomainIntegrator();
        ecpdbDomainIntegrator.PDB2ECAndInterPro();
        Map<String, TreeMap<String, Set<String>>> map = ecpdbDomainIntegrator.getMap();
        System.out.println("Number of EC Parsed: " + map.size());
    }

    /**
     *
     */
    protected void PDB2ECAndInterPro() {

        String server = "ftp.ebi.ac.uk";
        int port = 21;
        String user = "anonymous";
        String pass = "";

        File downloadFile1 = null;
        try {
            downloadFile1 = File.createTempFile("pdb_chain_interpro", "tsv.gz");
            downloadFile1.deleteOnExit();
        } catch (IOException ex) {
            Logger.getLogger(ECPDBDomainIntegrator.class.getName()).log(Level.SEVERE, null, ex);
        }
        File downloadFile2 = null;
        try {
            downloadFile2 = File.createTempFile("pdb_chain_enzyme", "tsv.gz");
            downloadFile2.deleteOnExit();
        } catch (IOException ex) {
            Logger.getLogger(ECPDBDomainIntegrator.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (downloadFile1 == null || downloadFile2 == null) {
            return;
        }

        FTPClient ftpClient = new FTPClient();
        try {

            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // APPROACH #1: using retrieveFile(String, OutputStream)
            String remoteFile1 = "pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_interpro.tsv.gz";
            boolean success;
            try (OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1))) {
                success = ftpClient.retrieveFile(remoteFile1, outputStream1);
            }

            if (success) {
                System.err.println("File " + downloadFile1.getName() + " has been downloaded successfully.");
            }

            // APPROACH #2: using InputStream retrieveFileStream(String)
            String remoteFile2 = "pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_enzyme.tsv.gz";
            InputStream inputStream;
            try (OutputStream outputStream2 = new BufferedOutputStream(new FileOutputStream(downloadFile2))) {
                inputStream = ftpClient.retrieveFileStream(remoteFile2);
                byte[] bytesArray = new byte[4096];
                int bytesRead = -1;
                while ((bytesRead = inputStream.read(bytesArray)) != -1) {
                    outputStream2.write(bytesArray, 0, bytesRead);
                }
                success = ftpClient.completePendingCommand();
                if (success) {
                    System.err.println("File " + downloadFile2.getName() + " has been downloaded successfully.");
                }
            }
            inputStream.close();

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        /*
         Read PDB Enzyme File
         */
        if (downloadFile2.exists()) {
            try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(downloadFile2))) {
                Reader decoder = new InputStreamReader(in);

                String thisLine;
                try (BufferedReader bf = new BufferedReader(decoder)) {
                    String info = bf.readLine();
                    String header = bf.readLine();

                    while ((thisLine = bf.readLine()) != null) {
                        //System.err.println("line " + line);
                        String[] split = thisLine.split("\\s+");
                        String pdb = split[0];
                        String ec = split[split.length - 1];

                        if (!rawMap.containsKey(ec)) {
                            rawMap.put(ec, new TreeMap<>());
                        }

                        if (!rawMap.get(ec).containsKey(pdb)) {
                            rawMap.get(ec).put(pdb, new TreeSet<>());
                        }
                        //System.err.println("pdb " + pdb + " ec " + ec);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ECPDBDomainIntegrator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (downloadFile1.exists()) {
            try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(downloadFile1))) {
                Reader decoder = new InputStreamReader(in);

                String thisLine;
                int line = 0;
                try (BufferedReader bf = new BufferedReader(decoder)) {
                    String info = bf.readLine();
                    String header = bf.readLine();
                    while ((thisLine = bf.readLine()) != null) {
                        line++;
                        //System.err.println("line " + line);
                        String[] split = thisLine.split("\\s+");
                        String pdb = split[0];
                        String interpro = split[split.length - 1];

                        rawMap.keySet().stream().filter((ec) -> (rawMap.get(ec).containsKey(pdb))).forEach((ec) -> {
                            rawMap.get(ec).get(pdb).add(interpro);
                        });

                        //System.err.println("pdb " + pdb + " interpro " + interpro);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ECPDBDomainIntegrator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    protected void parseFile(String fileName) {
        File f = new File(fileName);
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
            }
        } catch (Exception ex) {
            Logger.getLogger(MCEDS.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (DEBUG) {
            System.out.println("Number of PDB entries parsed: " + rawMap.size());
        }
    }

    Map<String, TreeMap<String, Set<String>>> getMap() {
        return rawMap;
    }
}
