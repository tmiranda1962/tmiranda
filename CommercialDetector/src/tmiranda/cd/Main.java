/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

/**
 *
 * @author Tom Miranda
 */
public class Main {

    static boolean GetNumberRunning = false;
    static boolean GetQueueSize = false;

    public Main() {}

    public static void main(String[] args) {

        System.out.println("CommercialDetector command line invoked.");

        // Parse the arguments.
        for (int i=0; i<args.length; i++) {

            String ThisArg = args[i];
            System.out.println("Arg = " + ThisArg);

            if (!ThisArg.startsWith("-")) {
                System.out.println("Invalid argument, skipping. Use --help for help.");
                continue;
            }

            if (ThisArg.equals("--help")) {
                System.out.println("Valid arguments:");
                System.out.println("-NR Show the Number of Running jobs.");
                System.out.println("-QS Show the Queue Size.");
                System.out.println("--help Print this help message.");
            } else if (ThisArg.equals("-NR")) {
                GetNumberRunning = true;
            } else if (ThisArg.equals("-QS")) {
                GetQueueSize = true;
            }

        }

        if (GetNumberRunning) {
            System.out.println("Number running = " + ComskipManager.getInstance().getNumberRunning());
        }

        if (GetQueueSize) {
            System.out.println("Number running = " + ComskipManager.getInstance().getQueueSize(false));
        }

    }

}
