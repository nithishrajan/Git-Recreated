package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

public class Repo implements Serializable {
    /** Current Staging Area. */
    private StagingArea _stage;
    /** Commit List. */
    private List<String> _commitList;
    /** String of Current Branch. */
    private String _currBranch;
    /** List of Branches. */
    private List<String> _branches;
    /** Current Commit List used for Merge splitFinder. */
    private HashMap<Commit, Integer> _cList = new HashMap<>();
    /** Given Commit List used for Merge splitFinder. */
    private HashMap<Commit, Integer> _oList = new HashMap<>();
    /** Current Commit. */
    private Commit _currCommit;

    /** Working Directory. */
    static final File WORKINGDIR = new File(System.getProperty("user.dir"));
    /** Gitlet Directory. */
    static final File GITLETPATH = Utils.join(WORKINGDIR, ".gitlet");
    /** Stage Directory. */
    static final File STAGEPATH = Utils.join(GITLETPATH, "stage");
    /** Commit Directory. */
    static final File COMMITPATH = Utils.join(GITLETPATH, "commits");
    /** Blob Directory. */
    static final File BLOBPATH = Utils.join(GITLETPATH, "blobs");
    /** Branch Directory. */
    static final File BRANCHPATH = Utils.join(GITLETPATH, "branches");


    public Repo() {
        if (COMMITPATH.exists()) {
            _commitList = Utils.plainFilenamesIn(COMMITPATH);
        }
        if (Utils.join(BRANCHPATH, "currBranch").exists()) {
            File bFile = Utils.join(BRANCHPATH, "currBranch");
            _currBranch = Utils.readContentsAsString(bFile);
        }
        _branches = Utils.plainFilenamesIn(BRANCHPATH);
        if (STAGEPATH.exists()) {
            File sFile = Utils.join(STAGEPATH, "curStage");
            _stage = Utils.readObject(sFile, StagingArea.class);
        }
        if (BRANCHPATH.exists()) {
            File cbFile = Utils.join(BRANCHPATH, _currBranch);
            _currCommit = Utils.readObject(cbFile, Commit.class);
        }
    }

    public void init() {
        if (GITLETPATH.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            return;
        }
        GITLETPATH.mkdir();
        COMMITPATH.mkdir();
        BLOBPATH.mkdir();
        STAGEPATH.mkdir();
        BRANCHPATH.mkdir();
        _commitList = new ArrayList<>();
        TreeMap<String, String> tMap = new TreeMap<>();
        String s = "initial commit";
        ZonedDateTime d = ZonedDateTime.now();
        Commit initial = new Commit(s, d, null, tMap);
        String initialIdentification = Utils.sha1(Utils.serialize(initial));
        File iCom = Utils.join(COMMITPATH, initialIdentification);
        Utils.writeObject(iCom, initial);
        File master = Utils.join(BRANCHPATH, "master");
        Utils.writeObject(master, initial);
        _stage = new StagingArea();
        File stage = Utils.join(STAGEPATH, "curStage");
        Utils.writeObject(stage, _stage);
        File currBranch = Utils.join(BRANCHPATH, "currBranch");
        Utils.writeContents(currBranch, "master");
    }

    public void add(String addArg) throws IOException {
        if (!COMMITPATH.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        if (!Utils.join(WORKINGDIR, addArg).exists()) {
            System.out.println("File does not exist.");
            return;
        }
        File f = Utils.join(STAGEPATH, "added");
        File cwdFile = new File(addArg);
        byte[] contents = Utils.readContents(cwdFile);
        File blobFile = Utils.join(BLOBPATH, Utils.sha1(contents));
        Utils.writeContents(blobFile, contents);
        Commit rCom = _currCommit;
        _stage.add(addArg, rCom);
    }

    public void commit(String comArg) {
        if (comArg.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        File f = Utils.join(STAGEPATH, "added");
        File r = Utils.join(STAGEPATH, "remove");
        if (_stage.getAddedFiles().size() == 0
                && (Utils.plainFilenamesIn(r) == null)) {
            System.out.println("No changes added to the commit.");
            return;
        }
        Commit rCom = recentCommit();
        Commit added = _stage.commit(comArg, rCom);
        _stage.clear();
        byte[] in = Utils.serialize(added);
        String newIdentification = Utils.sha1(in);
        File iCom = Utils.join(COMMITPATH, newIdentification);
        Utils.writeObject(iCom, added);
        File cBranch = Utils.join(BRANCHPATH, _currBranch);
        cBranch.delete();
        Utils.writeObject(cBranch, added);
    }

    public void remove(String rArg) {
        ArrayList<String> addedFiles = new ArrayList<>();
        File f = Utils.join(STAGEPATH, "added");
        if (f.exists()) {
            addedFiles = new ArrayList<>(Utils.plainFilenamesIn(f));
        } else {
            addedFiles = new ArrayList<>();
        }
        Commit rCom = recentCommit();
        String sha = rCom.getBlobs().get(rArg);
        if (sha == null && !addedFiles.contains(rArg)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        _stage.remove(rArg, rCom);
    }

    public void log() {
        Commit rCom = recentCommit();
        while (rCom != null) {
            System.out.println("===");
            System.out.println("commit " + rCom.getSHA());
            if (rCom.getMerge()) {
                String first = rCom.getParent2()[0].getSHA();
                System.out.println("Merge: " + first.substring(0, 7)
                        + " " + rCom.getParent2()[1].getSHA().substring(0, 7));
            }
            System.out.println("Date: " + rCom.getstringStamp());
            System.out.println(rCom.getMessage());
            System.out.println();
            if (rCom.getMerge()) {
                rCom = rCom.getParent2()[0];
            } else {
                rCom = rCom.getParent();
            }
        }
    }

    public void glog() {
        List<String> commitList = Utils.plainFilenamesIn(COMMITPATH);
        for (String s : commitList) {
            File f = Utils.join(COMMITPATH, s);
            Commit com = Utils.readObject(f, Commit.class);
            System.out.println("===");
            System.out.println("commit " + com.getSHA());
            System.out.println("Date: " + com.getstringStamp());
            System.out.println(com.getMessage());
            System.out.println();
        }
    }

    public void find(String arg) {
        ArrayList<Commit> commitList = new ArrayList<>();
        for (String s : _commitList) {
            File f = Utils.join(COMMITPATH, s);
            Commit c = Utils.readObject(f, Commit.class);
            commitList.add(c);
        }
        ArrayList<String> goodMessages = new ArrayList<>();
        for (Commit c : commitList) {
            if (c.getMessage().equals(arg)) {
                goodMessages.add(c.getSHA());
            }
        }
        if (goodMessages.size() == 0) {
            System.out.println("Found no commit with that message.");
            return;
        }
        for (String t : goodMessages) {
            System.out.println(t);
        }
    }

    public void status() {
        if (!COMMITPATH.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        List<String> branches = Utils.plainFilenamesIn(BRANCHPATH);
        File added = Utils.join(STAGEPATH, "added");
        File removed = Utils.join(STAGEPATH, "remove");
        List<String> stagedFiles = Utils.plainFilenamesIn(added);
        List<String> removedFiles = Utils.plainFilenamesIn(removed);
        System.out.println("=== Branches ===");
        System.out.println("*" + _currBranch);
        for (String a: branches) {
            if (!a.equals("currBranch") && !a.equals(_currBranch)) {
                System.out.println(a);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        if (stagedFiles != null) {
            for (String b : stagedFiles) {
                System.out.println(b);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        if (removedFiles != null) {
            for (String c: removedFiles) {
                System.out.println(c);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");

    }

    public void fileCheck(String file) {
        Commit rCom = recentCommit();
        if (rCom.getBlobs().get(file) == null) {
            throw new GitletException("File does not exist in that commit.");
        }
        file2comCheck(_currCommit.getSHA(), file);
    }

    public void file2comCheck(String comName, String fileName) {
        String com = comName;
        if (comName.length() == 8) {
            for (String s : _commitList) {
                if (s.startsWith(comName)) {
                    com = s;
                }
            }
        }
        File commitPath = Utils.join(COMMITPATH, com);
        if (!commitPath.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit specCommit = Utils.readObject(commitPath, Commit.class);
        String afi = specCommit.getBlobs().get(fileName);
        if (afi == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        byte[] content = Utils.readContents(Utils.join(BLOBPATH, afi));
        File f = new File(fileName);
        f.delete();
        Utils.writeContents(f, content);
    }

    public void branchCheck(String branch) {
        File branchFile = Utils.join(BRANCHPATH, branch);
        File added = Utils.join(STAGEPATH, "added");
        List<String> addFiles = Utils.plainFilenamesIn(added);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            return;
        }
        if (branch.equals(_currBranch)) {
            System.out.println("No need to checkout the current branch.");
        }
        Commit specCom = Utils.readObject(branchFile, Commit.class);
        Set<String> specNames = specCom.getBlobs().keySet();
        String[] spckeySet = specNames.toArray(new String[specNames.size()]);
        Set<String> pmn = _currCommit.getBlobs().keySet();
        String[] rcomkeySet = pmn.toArray(new String[pmn.size()]);
        List<String> workingFiles = Utils.plainFilenamesIn(WORKINGDIR);
        for (int x = 0; x < workingFiles.size(); x++) {
            String fName = workingFiles.get(x);
            if (!pmn.contains(fName)) {
                if (addFiles.size() == 0) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
            }
        }
        for (int x = 0; x < _currCommit.getBlobs().keySet().size(); x++) {
            if (!specNames.contains(rcomkeySet[x])) {
                File cray = new File(rcomkeySet[x]);
                Utils.restrictedDelete(cray);
            }
        }
        for (int x = 0; x < spckeySet.length; x++) {
            String s = specCom.getBlobs().get(spckeySet[x]);
            File blob = Utils.join(BLOBPATH, s);
            byte[] contents = Utils.readContents(blob);
            File newFile = new File(spckeySet[x]);
            Utils.restrictedDelete(newFile);
            Utils.writeContents(newFile, contents);
        }
        _stage.clear();
        File currBranch = Utils.join(BRANCHPATH, "currBranch");
        currBranch.delete();
        Utils.writeContents(currBranch, branch);
    }

    public void branch(String arg) {
        List<String> branchList = Utils.plainFilenamesIn(BRANCHPATH);
        if (branchList.contains(arg)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        File newBranch = Utils.join(BRANCHPATH, arg);
        Utils.writeObject(newBranch, _currCommit);
    }

    public void rBranch(String arg) {
        File f = Utils.join(BRANCHPATH, arg);
        if (!f.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (arg.equals(_currBranch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        f.delete();
    }

    public void reset(String arg) {
        if (!_commitList.contains(arg)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File added = Utils.join(STAGEPATH, "added");
        List<String> addFiles = Utils.plainFilenamesIn(added);
        File cFile = Utils.join(COMMITPATH, arg);
        Commit c = Utils.readObject(cFile, Commit.class);
        List<String> workingFiles = Utils.plainFilenamesIn(WORKINGDIR);
        for (String s : workingFiles) {
            if (!recentCommit().getBlobs().containsKey(s)) {
                if (!addFiles.contains(s)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return;
                }
            }
        }
        for (String s : recentCommit().getBlobs().keySet()) {
            if (!c.getBlobs().containsKey(s)) {
                File f = new File(s);
                remove(s);
                Utils.restrictedDelete(f);
            }
        }
        if (addFiles != null) {
            for (String t : addFiles) {
                if (!c.getBlobs().containsKey(t)) {
                    File f = new File(t);
                    Utils.restrictedDelete(t);
                    File addedFile = Utils.join(added, t);
                    addedFile.delete();
                }
            }
        }
        Set<String> keySet = c.getBlobs().keySet();
        String[] blobList = keySet.toArray(new String[keySet.size()]);
        for (String s : blobList) {
            file2comCheck(c.getSHA(), s);
        }
        _stage.clear();
        File cBranch = Utils.join(BRANCHPATH, _currBranch);
        cBranch.delete();
        Utils.writeObject(cBranch, c);
    }

    public void merge(String arg) {
        File bArg = Utils.join(BRANCHPATH, arg);
        if (mergeError(arg)) {
            return;
        }
        Commit otherCommit = Utils.readObject(bArg, Commit.class);
        Commit splitPoint = splitFinder(arg);
        if (otherCommit.getParent().getSHA().equals(_currCommit.getSHA())) {
            splitPoint = _currCommit;
        }
        if (otherCommit.getSHA().equals(splitPoint.getSHA())) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            return;
        }
        if (splitPoint.equals(_currCommit)) {
            branchCheck(arg);
            System.out.println("Current branch fast-forwarded.");
        }
        Commit r = mergeHelper(_currCommit, otherCommit, splitPoint, arg);
        byte[] b = Utils.serialize(r);
        File f = Utils.join(COMMITPATH, Utils.sha1(b));
        Utils.writeObject(f, r);
        File branch = Utils.join(BRANCHPATH, _currBranch);
        branch.delete();
        Utils.writeObject(branch, r);
        r.setMerge();
    }

    private Commit mergeHelper(Commit currCommit, Commit otherCommit,
                               Commit splitPoint, String arg) {
        TreeMap<String, String> cBlobs = currCommit.getBlobs();
        TreeMap<String, String> oBlobs = otherCommit.getBlobs();
        TreeMap<String, String> sBlobs = splitPoint.getBlobs();
        boolean conflict = false;
        TreeMap<String, String> newBlobs = new TreeMap<>();
        for (String s : splitPoint.getBlobs().keySet()) {
            File f = new File(s);
            if (!cBlobs.containsKey(s) && !oBlobs.containsKey(s)) {
                continue;
            }
            if (sBlobs.get(s).equals(cBlobs.get(s))
                    && !oBlobs.containsKey(s)) {
                Utils.restrictedDelete(f);
                continue;
            }
            if (sBlobs.get(s).equals(oBlobs.get(s))
                    && !cBlobs.containsKey(s)) {
                continue;
            }
            if (!sBlobs.get(s).equals(oBlobs.get(s))
                    && sBlobs.get(s).equals(cBlobs.get(s))) {
                newBlobs.put(s, oBlobs.get(s));
                Utils.restrictedDelete(f);
                File blob = Utils.join(BLOBPATH, oBlobs.get(s));
                Utils.writeContents(f, Utils.readContents(blob));
                continue;
            }
            if (!sBlobs.get(s).equals(oBlobs.get(s))
                    && !sBlobs.get(s).equals(cBlobs.get(s))) {
                if (!cBlobs.get(s).equals(oBlobs.get(s))) {
                    conflict = true;
                    String cBlobCon = "";
                    String oBlobCon = "";
                    if (cBlobs.get(s) == null) {
                        File sup = Utils.join(BLOBPATH, oBlobs.get(s));
                        oBlobCon = Utils.readContentsAsString(sup);
                    } else if (oBlobs.get(s) == null) {
                        File sup = Utils.join(BLOBPATH, cBlobs.get(s));
                        cBlobCon = Utils.readContentsAsString(sup);
                    } else {
                        File sup = Utils.join(BLOBPATH, cBlobs.get(s));
                        File sups = Utils.join(BLOBPATH, oBlobs.get(s));
                        cBlobCon = Utils.readContentsAsString(sup);
                        oBlobCon = Utils.readContentsAsString(sups);
                    }
                    Utils.restrictedDelete(f);
                    String fileContent = "<<<<<<< HEAD\n" + cBlobCon
                            + "=======\n" + oBlobCon + ">>>>>>>\n";
                    Utils.writeContents(f, fileContent);
                }
            }
            if (cBlobs.get(s).equals(sBlobs.get(s))
                    && sBlobs.get(s).equals(oBlobs.get(s))) {
                newBlobs.put(s, cBlobs.get(s));
            }
        }
        return mergeHelper2(currCommit, otherCommit,
                splitPoint, arg, newBlobs, conflict);
    }

    public Commit mergeHelper2(Commit currCommit, Commit otherCommit,
                               Commit splitPoint, String arg,
                               TreeMap<String, String> blobs,
                               boolean isConflict) {
        TreeMap<String, String> cBlobs = currCommit.getBlobs();
        TreeMap<String, String> oBlobs = otherCommit.getBlobs();
        TreeMap<String, String> sBlobs = splitPoint.getBlobs();
        Set<String> cFileNames = currCommit.getBlobs().keySet();
        Set<String> oFileNames = otherCommit.getBlobs().keySet();
        Set<String> sfn = splitPoint.getBlobs().keySet();
        for (String c : cFileNames) {
            if (!sBlobs.containsKey(c) && !oBlobs.containsKey(c)) {
                blobs.put(c, cBlobs.get(c));
            } else if (!sBlobs.containsKey(c)) {
                if (!oBlobs.get(c).equals(cBlobs.get(c))) {
                    isConflict = true;
                    File f = new File(c);
                    File sup = Utils.join(BLOBPATH, cBlobs.get(c));
                    File sups = Utils.join(BLOBPATH, oBlobs.get(c));
                    String cBlobCon = Utils.readContentsAsString(sup);
                    String oBlobCon = Utils.readContentsAsString(sups);
                    Utils.restrictedDelete(f);
                    String fileContent = "<<<<<<< HEAD\n" + cBlobCon
                            + "=======\n" + oBlobCon + ">>>>>>>\n";
                    Utils.writeContents(f, fileContent);
                    blobs.put(c, Utils.sha1(Utils.readContents(f)));
                }
            }
            if (sBlobs.containsKey(c)) {
                if (!sBlobs.get(c).equals(cBlobs.get(c))
                        && !oBlobs.containsKey(c)) {
                    isConflict = true;
                    File f = new File(c);
                    File sup = Utils.join(BLOBPATH, cBlobs.get(c));
                    String cBlobCon = Utils.readContentsAsString(sup);
                    Utils.restrictedDelete(f);
                    String fileContent = "<<<<<<< HEAD\n" + cBlobCon
                            + "=======\n" + ">>>>>>>\n";
                    Utils.writeContents(f, fileContent);
                    blobs.put(c, Utils.sha1(Utils.readContents(f)));
                }
            }
        }
        for (String o : oFileNames) {
            if (!sfn.contains(o) && !cFileNames.contains(o)) {
                blobs.put(o, oBlobs.get(o));
                File newFile = new File(o);
                File blobFile = Utils.join(BLOBPATH, oBlobs.get(o));
                Utils.restrictedDelete(newFile);
                byte[] contents = Utils.readContents(blobFile);
                Utils.writeContents(newFile, contents);
            }
        }
        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        String s = "Merged " + arg + " " + "into " + _currBranch + ".";
        ZonedDateTime d = ZonedDateTime.now();
        return new Commit(s, d, _currCommit, otherCommit, blobs);
    }

    public Commit splitFinder(String givenBranch) {
        File f = Utils.join(BRANCHPATH, givenBranch);
        Commit c = _currCommit;
        Commit otherCommit = Utils.readObject(f, Commit.class);
        _cList = new HashMap<>();
        _oList = new HashMap<>();
        listMaker(c, true, 0);
        listMaker(otherCommit, false, 0);
        HashMap<Commit, Integer> split = new HashMap<>();
        Commit firstCom = null;
        for (Commit cCom : _cList.keySet()) {
            for (Commit oCom : _oList.keySet()) {
                if (cCom.getSHA().equals(oCom.getSHA())) {
                    if (split.size() == 0) {
                        split.put(oCom, _oList.get(oCom));
                        firstCom = oCom;
                    } else if (split.get(firstCom) > _oList.get(oCom)) {
                        split = new HashMap<>();
                        split.put(oCom, _oList.get(oCom));
                        firstCom = oCom;
                    }
                }
            }
        }
        Set<Commit> s = split.keySet();
        Commit[] sup = s.toArray(new Commit[s.size()]);
        return sup[0];
    }

    public void listMaker(Commit intended, boolean commitBool, int length) {
        if (intended == null) {
            return;
        }
        if (commitBool) {
            _cList.put(intended, length);
            if (!intended.getMerge()) {
                listMaker(intended.getParent(), true, length + 1);
            } else {
                Commit[] parents = intended.getParent2();
                listMaker(parents[0], true, length + 1);
                listMaker(parents[1], true, length + 1);
            }
        } else {
            _oList.put(intended, length);
            if (!intended.getMerge()) {
                listMaker(intended.getParent(), false, length + 1);
            } else {
                Commit[] parents = intended.getParent2();
                listMaker(parents[0], false, length + 1);
                listMaker(parents[1], false, length + 1);
            }
        }
    }

    public boolean mergeError(String givenBranch) {
        File f = Utils.join(STAGEPATH, "added");
        File r = Utils.join(STAGEPATH, "remove");
        ArrayList<String> addedFiles = new ArrayList<String>();
        if (f.exists()) {
            addedFiles = _stage.getAddedFiles();
        }
        List<String> s = (Utils.plainFilenamesIn(r));
        if (s != null) {
            if (addedFiles.size() != 0 || s.size() != 0) {
                System.out.println("You have uncommitted changes.");
                return true;
            }
        }
        if (!_branches.contains(givenBranch)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (_currBranch.equals(givenBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        List<String> workingFiles = Utils.plainFilenamesIn(WORKINGDIR);
        for (int x = 0; x < workingFiles.size(); x++) {
            String fName = workingFiles.get(x);
            if (!_currCommit.getBlobs().keySet().contains(fName)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        return false;
    }

    public Commit recentCommit() {
        File f = Utils.join(BRANCHPATH, _currBranch);
        return Utils.readObject(f, Commit.class);
    }

    public ArrayList<String> branchCommits() {
        ArrayList<String> rList = new ArrayList<>();
        for (String s : _branches) {
            File f = Utils.join(BRANCHPATH, s);
            if (!s.equals("currBranch")) {
                rList.add(Utils.readContentsAsString(f));
            }
        }
        return rList;
    }

    public ArrayList<File> fileMaker(Commit given) {
        ArrayList<File> rFile = new ArrayList<>();
        Set<String> gfileNames = given.getBlobs().keySet();
        String[] fileNames = gfileNames.toArray(new String[0]);
        for (String s : fileNames) {
            String shaID = given.getBlobs().get(s);
            rFile.add(Utils.join(BLOBPATH, shaID));
        }
        return rFile;
    }
}
