package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.TreeMap;


public class Commit implements Serializable {
    /** Commit Message. */
    private String message;
    /** Commit Parent. */
    private Commit parent;
    /** Date commit was made. */
    private ZonedDateTime date;
    /** Blob Files of the Commit. */
    private TreeMap<String, String> blobs;
    /** If merge Commit, first parent. */
    private Commit parent1;
    /** If merge Commit, second parent. */
    private Commit parent2;
    /** If Commit is Merge Commit. */
    private boolean isMerge;

    public Commit(String inMessage, ZonedDateTime inDate,
                  Commit inParent, TreeMap<String, String> inBlobs) {
        this.message = inMessage;
        this.parent = inParent;
        this.date = inDate;
        this.blobs = inBlobs;
        isMerge = false;
    }

    public Commit(String inMessage, ZonedDateTime inDate, Commit inParent1,
                  Commit inParent2, TreeMap<String, String> inBlobs) {
        this.message = inMessage;
        this.parent1 = inParent1;
        this.parent2 = inParent2;
        this.date = inDate;
        this.blobs = inBlobs;
        isMerge = true;
    }

    public String getMessage() {
        return this.message;
    }

    public String getstringStamp() {
        if (parent == null) {
            return "Wed Dec 31 16:00:00 1969 -0800";
        }
        ZonedDateTime now = date;
        return now.format(DateTimeFormatter.ofPattern("EEE MMM d "
                + "HH:mm:ss yyyy xxxx"));
    }
    public Commit getParent() {
        return this.parent;
    }

    public Commit[] getParent2() {
        Commit[] pList = new Commit[2];
        pList[0] = parent1;
        pList[1] = parent2;
        return pList;
    }

    public TreeMap<String, String> getBlobs() {
        return this.blobs;
    }

    public String getSHA() {
        byte[] combyte = Utils.serialize(this);
        return Utils.sha1(combyte);
    }

    public ArrayList<File> getFiles() {
        String[] blobList = blobs.keySet().toArray(new String[blobs.size()]);
        ArrayList<File> rList = new ArrayList<>();
        for (String s : blobList) {
            File f = Utils.join(Repo.BLOBPATH, s);
            rList.add(f);
        }
        return rList;
    }

    public boolean getMerge() {
        return this.isMerge;
    }

    public void setMerge() {
        isMerge = true;
    }
}
