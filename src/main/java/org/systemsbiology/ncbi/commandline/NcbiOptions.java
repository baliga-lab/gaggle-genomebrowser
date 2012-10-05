package org.systemsbiology.ncbi.commandline;

import java.util.HashMap;

public class NcbiOptions {
    public enum Command {PROJECTS, SEQUENCES, GENES};
    public Command command;
    public String organism;
    public String genomeProjectId;
    private HashMap<String, Command> commands = new HashMap<String, Command>();

    // initialize command map
    {
        commands.put("projects",  Command.PROJECTS);
        commands.put("sequences", Command.SEQUENCES);
        commands.put("genes",     Command.GENES);
    }

    private boolean needsHelp(String arg) {
        return arg.startsWith("?") ||
            "-?".equals(arg) || 
            "-h".equals(arg) || 
            "-help".equals(arg) || 
            "--help".equals(arg); 
    }

    public void usage() {
        System.out.println("\n");
        System.out.println("NcbiFetch projects ORGANISM");
        System.out.println();
        System.out.println("NcbiFetch sequences GENOME_PROJECT_ID");
        //		System.out.println("");
        //		System.out.println();
        //		System.out.println("");
        //		System.out.println();
        //		System.out.println("");
        //		System.out.println("");
        //		System.out.println("");
        System.out.println();
        System.out.println("The projects command is used to search NCBI from genome projects for an");
        System.out.println("organism. Often, more than one result is returned. Sequences can be");
        System.out.println("retrieved by GENOME_PROJECT_ID, which is returned by the projects");
        System.out.println("command.");
        System.out.println("\n");
    }

    public boolean parse(String[] args) {
        if (args.length < 1 || needsHelp(args[0])) {
            usage();
            return false;
        }

        if (commands.containsKey(args[0])) {
            command = commands.get(args[0]);
			
            switch (command) {

            case PROJECTS:
                if (args.length < 2) {
                    usage();
                    return false;
                }
                organism = args[1];
                break;

            case SEQUENCES:
                if (args.length < 2) {
                    usage();
                    return false;
                }
                genomeProjectId = args[1];
                break;
            }
            return true;
        }	else {
            usage();
            return false;
        }
    }
}
