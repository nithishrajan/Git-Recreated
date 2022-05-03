package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Nithish Rajan
 */
public class Main {
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        Repo repo = new Repo();
        if (args[0].equals("init")) {
            repo.init();
        } else if (args[0].equals("add")) {
            repo.add(args[1]);
        } else if (args[0].equals("commit")) {
            repo.commit(args[1]);
        } else if (args[0].equals("rm")) {
            repo.remove(args[1]);
        } else if (args[0].equals("log")) {
            repo.log();
        } else if (args[0].equals("global-log")) {
            repo.glog();
        } else if (args[0].equals("find")) {
            repo.find(args[1]);
        } else if (args[0].equals("status")) {
            repo.status();
        } else if (args[0].equals("checkout")) {
            if (args.length == 1) {
                System.out.println("Incorrect operands.");
            }
            if (args.length == 2) {
                repo.branchCheck(args[1]);
                return;
            } else if (args.length == 3) {
                repo.fileCheck(args[2]);
                return;
            } else if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            } else if (args.length == 4) {
                repo.file2comCheck(args[1], args[3]);
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("branch")) {
            repo.branch(args[1]);
        } else if (args[0].equals("rm-branch")) {
            repo.rBranch(args[1]);
        } else if (args[0].equals("reset")) {
            repo.reset(args[1]);
        } else if (args[0].equals("merge")) {
            repo.merge(args[1]);
        } else {
            System.out.println("No command with that name exists.");
        }
    }
}
