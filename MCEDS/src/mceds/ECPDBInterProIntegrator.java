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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
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
public class ECPDBInterProIntegrator {

    String pdb_interpro = "ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_interpro.tsv.gz";
    String pdb_ec = "ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_enzyme.tsv.gz";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        Map<String, TreeMap<String, Set<String>>> PDB2ECAndInterPro = new ECPDBInterProIntegrator().PDB2ECAndInterPro();
        System.out.println("Number of EC Parsed: " + PDB2ECAndInterPro.size());
    }

    /**
     *
     * @return Map of EC and InterPro Domains
     */
    protected Map<String, TreeMap<String, Set<String>>> PDB2ECAndInterPro() {

        Map<String, TreeMap<String, Set<String>>> rawData = new TreeMap<>();

        String server = "ftp.ebi.ac.uk";
        int port = 21;
        String user = "anonymous";
        String pass = "";

        File downloadFile1 = null;
        try {
            downloadFile1 = File.createTempFile("pdb_chain_interpro", "tsv.gz");
            downloadFile1.deleteOnExit();
        } catch (IOException ex) {
            Logger.getLogger(ECPDBInterProIntegrator.class.getName()).log(Level.SEVERE, null, ex);
        }
        File downloadFile2 = null;
        try {
            downloadFile2 = File.createTempFile("pdb_chain_enzyme", "tsv.gz");
            downloadFile2.deleteOnExit();
        } catch (IOException ex) {
            Logger.getLogger(ECPDBInterProIntegrator.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (downloadFile1 == null || downloadFile2 == null) {
            return rawData;
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

                        if (!rawData.containsKey(ec)) {
                            rawData.put(ec, new TreeMap<>());
                        }

                        if (!rawData.get(ec).containsKey(pdb)) {
                            rawData.get(ec).put(pdb, new TreeSet<>());
                        }
                        //System.err.println("pdb " + pdb + " ec " + ec);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ECPDBInterProIntegrator.class.getName()).log(Level.SEVERE, null, ex);
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

                        rawData.keySet().stream().filter((ec) -> (rawData.get(ec).containsKey(pdb))).forEach((ec) -> {
                            rawData.get(ec).get(pdb).add(interpro);
                        });

                        //System.err.println("pdb " + pdb + " interpro " + interpro);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ECPDBInterProIntegrator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //System.out.println("Number of EC Parsed: " + rawData.size());
        return rawData;
    }
}
