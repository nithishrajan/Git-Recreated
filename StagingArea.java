package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


public class StagingArea implements Serializable {

    /** List of Added Files. */
    private ArrayList<String> _addedFiles;

    /** List of Files in Working Directory. */
    static final File WORKINGDIR = new File(System.getProperty("user.dir"));

    /** File path for .gitlet folder. */
    private static File _gitletPath = Utils.join(WORKINGDIR, ".gitlet");

    /** File path for Stage. */
    private static File _stagePath = Utils.join(_gitletPath, "stage");

    /** File path for list of added files. */
    private File addedStage = Utils.join(_stagePath, "added");

    /** File path for list of removed files. */
    private File removePath = Utils.join(_stagePath, "remove");


    public ArrayList<String> getAddedFiles() {
        if (Utils.plainFilenamesIn(addedStage) == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Utils.plainFilenamesIn(addedStage));
    }
    public void clear() {
        _addedFiles = new ArrayList<>();
        List<String> filesinAdd = Utils.plainFilenamesIn(addedStage);
        List<String> filesinRemove = Utils.plainFilenamesIn(removePath);
        if (filesinAdd != null) {
            for (String s : filesinAdd) {
                File f = Utils.join(addedStage, s);
                f.delete();
            }
        }
        if (filesinRemove != null) {
            for (String s : filesinRemove) {
                File f = Utils.join(removePath, s);
                f.delete();
            }
        }

    }

    public void add(String realFile, Commit recentCommit) {
        if (!addedStage.exists()) {
            addedStage.mkdir();
            _addedFiles = new ArrayList<>();
        } else {
            _addedFiles = getAddedFiles();
        }
        File realAddFile = new File(realFile);
        byte[] contents = Utils.readContents(realAddFile);
        String afi = Utils.sha1(contents);
        TreeMap<String, String> blobs = recentCommit.getBlobs();
        if (_addedFiles.contains(realFile)) {
            File realFilePath = Utils.join(addedStage, realFile);
            realFilePath.delete();
            Utils.writeContents(realFilePath, contents);
            File removeFile = Utils.join(removePath, realFile);
            removeFile.delete();
            return;
        }
        String blobAfi = blobs.get(realFile);
        if (blobAfi != null && blobAfi.equals(afi)) {
            _addedFiles.remove(realFile);
            File removeFile = Utils.join(removePath, realFile);
            removeFile.delete();
            return;
        }
        _addedFiles.add(realFile);
        File f = Utils.join(addedStage, realFile);
        Utils.writeContents(f, contents);
        File removeFile = Utils.join(removePath, realFile);
        removeFile.delete();
    }

    public void remove(String rFile, Commit rCom) {
        _addedFiles = getAddedFiles();
        _addedFiles.remove(rFile);
        if (!removePath.exists()) {
            removePath.mkdir();
        }
        File f = new File(rFile);
        if (rCom.getBlobs().get(rFile) == null || !f.exists()) {
            File re = Utils.join(addedStage, rFile);
            re.delete();
            if (!f.exists()) {
                File s = Utils.join(removePath, rFile);
                Utils.writeContents(s, rFile);
            }
            return;
        }
        byte[] contents = Utils.readContents(f);
        if (rCom.getBlobs().get(rFile).equals(Utils.sha1(contents))) {
            Utils.restrictedDelete(f);
            File removedFile = Utils.join(removePath, rFile);
            Utils.writeContents(removedFile, contents);
        }
        File re = Utils.join(addedStage, rFile);
        re.delete();
    }

    public Commit commit(String message, Commit recentCommit) {
        _addedFiles = getAddedFiles();
        List<String> removedFiles = Utils.plainFilenamesIn(removePath);
        if (_addedFiles.size() == 0 && removedFiles == null) {
            return null;
        }
        TreeMap<String, String> blobs = recentCommit.getBlobs();
        TreeMap<String, String> comBlobs = new TreeMap<>();
        for (String key : blobs.keySet()) {
            if (!_addedFiles.contains(key)) {
                comBlobs.put(key, blobs.get(key));
            }
        }
        for (String key : _addedFiles) {
            File f = Utils.join(addedStage, key);
            String sha = Utils.sha1(Utils.readContents(f));
            comBlobs.put(key, sha);
        }
        if (removedFiles != null) {
            for (String s : removedFiles) {
                if (comBlobs.containsKey(s)) {
                    comBlobs.remove(s);
                }
            }
        }
        String s = message;
        ZonedDateTime d = ZonedDateTime.now();
        return new Commit(s, d, recentCommit, comBlobs);
    }
}
