package edu.umd.cs.example

import edu.umd.cs.psl.application.topicmodel.LatentTopicNetwork
import edu.umd.cs.psl.config.ConfigBundle
import edu.umd.cs.psl.config.ConfigManager
import edu.umd.cs.psl.database.DataStore
import edu.umd.cs.psl.database.Database
import edu.umd.cs.psl.database.Partition
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver
import edu.umd.cs.psl.groovy.PSLModel
import edu.umd.cs.psl.groovy.PredicateConstraint
import edu.umd.cs.psl.model.argument.ArgumentType;

String dataDir = "data/combinedtweets/"
String saveDir = "data/combinedtweets/"

// creating constants and data loading
def numDocuments = 40;
def numWords = 5160; //DataLoader.getVocabularyCount(dataDir + "vocabulary");
System.out.println(numWords);
def numTopics = 20;
int numIterations = 100;
double alpha = 0.01 + 1;
double beta = 0.01 + 1;//0.0001 + 1;

/*
 * The first thing we need to do is initialize a ConfigBundle and a DataStore
 */

/*
 * A ConfigBundle is a set of key-value pairs containing configuration options. One place these
 * can be defined is in psl-example/src/main/resources/psl.properties
 */
ConfigManager cm = ConfigManager.getManager()
ConfigBundle config = cm.getBundle("event-detection")

/* Uses H2 as a DataStore and stores it in a temp. directory by default */
def defaultPath = System.getProperty("java.io.tmpdir")
String dbpath = config.getString("dbpath", defaultPath + File.separator + "event-detection")
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(H2DatabaseDriver.Type.Disk, dbpath, true), config)

/* Create PSL model for theta */
PSLModel mTheta = new PSLModel(this, data)
mTheta.add predicate: "theta", types: [ArgumentType.UniqueID, ArgumentType.UniqueID] //document, topic
mTheta.add predicate: "precedes", types: [ArgumentType.UniqueID, ArgumentType.UniqueID] //document, document
mTheta.add PredicateConstraint.Functional , on : theta //sums to one

// Currently the model has no rules

/* Create PSL model for phi*/
PSLModel mPhi = new PSLModel(this, data)
mPhi.add predicate: "phi", types: [ArgumentType.UniqueID, ArgumentType.UniqueID] //word, topic
mPhi.add PredicateConstraint.InverseFunctional , on : phi

// Load data for the model
int[][] docWords = DataLoader.getMatrix(dataDir+'docWords');
int[][] docCounts = DataLoader.getMatrix(dataDir+'docCount');

// Create partition for data
System.out.println("create partitions");
def readPartitionTheta = new Partition(0);
def writePartitionTheta = new Partition(1);

insert = data.getInserter(theta, writePartitionTheta);
for (int i = 0; i < numDocuments; i++) {
    for (int j = 0; j < numTopics; j++) {
        insert.insert(i, j);
    }
}

def insert2 = data.getInserter(precedes, readPartitionTheta);
for (int j = 1; j < numDocuments; j++)
	insert2.insert(j-1, j); //every document precedes the next document

Database trainDBTheta = data.getDatabase(writePartitionTheta, [precedes] as Set, readPartitionTheta);

def partitionPhi = new Partition(2);
insert = data.getInserter(precedes, partitionPhi);

def writePartitionPhi = new Partition(3);
insert = data.getInserter(phi, writePartitionPhi);
for (int k = 0; k < numTopics; k++) {
    for (int w = 0; w < numWords; w++) {
        insert.insert(k,w);
    }
}

Database dbPhi = data.getDatabase(writePartitionPhi, partitionPhi);
LatentTopicNetwork LTN = new LatentTopicNetwork(mTheta, trainDBTheta, mPhi, dbPhi, docWords, docCounts, numWords, config);
LTN.trainModel();

double[][] inferredTheta = LTN.getTheta();
double[][] inferredPhi = LTN.getPhi();
File thetafile = new File(saveDir+"theta.txt");
println "Inferred theta:"
for (int i = 0; i < numDocuments; i++) {
    for (int j = 0; j < numTopics; j++) {
        thetafile.append( inferredTheta[i][j] + "\t");
    }
    thetafile.append("\n");
}
