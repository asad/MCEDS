# Minimal Common Enzymatic Domain Selector (MCEDS)
Minimum Common Domain for Enzymes - by eliminate confusion domains within multi-domain architecture.
	
###Biological relevance:

For example, EC 5.99.1.3 is resented by PDB code '1ab4' which consists of 3 CATH domains (multi-domain architecture). These CATH domains are 3.90.199.10, 3.30.1360.40 and 1.10.268.10. 
The tool identifies 3.30.1360.40 as a 'confusion' domain (as it is not specific to this EC number) and 3.90.199.10 & 1.10.268.10 as 'Minimal Common Enzymatic Domain' (acts as a signature for this protein). 



####Command to Run:

	java -jar MCEDS.jar -f data/ec5_pdb_cathids.txt -s

Note:	

	-s for restricting the search within EC classes
	
	default: test against all the ECs present in the input file.

####EC-PDB-InterPro MCEDS knowledge-base via SIFTS

#####Command to build MCEDS based on the InterPro domains:

    java -jar MCEDS.jar -cache -s
