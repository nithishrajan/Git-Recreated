# Gitlet Design Document
author: Nithish Rajan

## 1. Classes and Data Structures

### Main.java
This class is the entry point of the program. It takes in arguments from the command prompt and interprets the argument to perform in the repository.

#### Fields

1. String ARGS: Command inputted into terminal to be interpreted.

### Repo.java
This class is where we perform our commands that are given to us from the Main class.

#### Fields
1. String head: A pointer to the head of the commit linked list.
2. stagingArea Stage: The staging area of the repo, where we add or remove our files for commiting.

### stagingArea.java
This class is where we put our added or removed files that are given to us from the Repo class.

#### Fields
1. TreeMap<String, String> added: The files we add to the staging area to be commited.
2. TreeMap<String, String> removed: The files we remove from commited files.

### Commit.java
This class is where we create our commit, along with its various details and reference to a Blob File.

#### Fields
1. String logMessage: User-inputted changes to files in the commit
2. String sha: The SHA-1 ID of the commit, which contains the file (blob) references, parent reference, log message, and commit time.
3. String dt: The date and time the commit was made.
4. TreeMap<String, String> blobs: The Treemap of Blobs that this commit contains.



## 2. Algorithms

### Main.java
1. main(String... args): This is the entry point of the program, it takes in a string of the format <COMMAND> <OPERAND>, and uses the command to determine which function to run, and passes in the operand to that function.

### Repo.java
1. init(): Intializes the repository in the current directory, if it already exists in the directory, it will erorr, otherwise it will create a repository with one `Commit`, containing no Files, essentially a null commit.
2. add(String fileName): Check if FILENAME file is identical to current commit, and do nothing, and remove from staging area if it is there, otherwise call Stage.add(FILENAME) for File. 
3. commit(String message): Takes in MESSAGE and creates a commit with MESSAGE, staging area is cleared, commit will have references to blob and SHA ID
4. rm(String fileName): removes file FILENAME from staging area, remove file from working directory if not already removed and tracked in the current commit.
5. log(): Returns commits starting at head, and until initial commit, ignoring any second parents in commits
6. global-log(): Returns all commits, uses method from Gitlet.utils
7. find(String cMessage): Prints out ids of all commits that have given commit message,
8. status(): Displays what branches exist in order of Branches, Staged Files, Removed Files
9. checkout(String fileName, String commitID): Changes head to commit, and puts file in working directory
10. branch(string branchName): Creates a new branch with the given name, and points it at the current head node. A branch is a name for a reference (a SHA-1 identifier) to a commit node
11. reset(string commitid): Checksout the files in the commit.
12. merge(string branchName): Checks if Given branch is the same as the split point, then we do nothing and print message, "Given branch is an ancestor of the current branch", if split point is current branch, then we checkout given branch and print "Current branch fast-forwarded"

### stagingArea.java
1. addFile(): Adds file to Treemap variable added.
2. rmFile(): Adds file to Treemap variable removed.

### Commit.java
1. getHash(): Returns ID of the commit by returning sha.

## 3. Persistence

Describe your strategy for ensuring that you don’t lose the state of your program
across multiple runs. Here are some tips for writing this section:

* This section should be structured as a list of all the times you
  will need to record the state of the program or files. For each
  case, you must prove that your design ensures correct behavior. For
  example, explain how you intend to make sure that after we call
       `java gitlet.Main add wug.txt`,
  on the next execution of
       `java gitlet.Main commit -m “modify wug.txt”`, 
  the correct commit will be made.
  
* A good strategy for reasoning about persistence is to identify which
  pieces of data are needed across multiple calls to Gitlet. Then,
  prove that the data remains consistent for all future calls.
  
* This section should also include a description of your .gitlet
  directory and any files or subdirectories you intend on including
  there.

## 4. Design Diagram

Attach a picture of your design diagram illustrating the structure of your
classes and data structures. The design diagram should make it easy to 
visualize the structure and workflow of your program.

