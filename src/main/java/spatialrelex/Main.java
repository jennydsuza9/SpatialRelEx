
package main.java.spatialrelex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import main.java.parsers.StanfordParser;
import static main.java.spatialrelex.Evaluator.getMovelinkSubpart;
import main.java.spatialrelex.ling.Features;
import main.java.spatialrelex.ling.GeneralInquirer;
import main.java.spatialrelex.ling.VerbNet;
import main.java.spatialrelex.ling.WordNet;
import main.java.spatialrelex.markup.SpatialRelation;
import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

/**
 *
 * @author Jenny D'Souza
 */
public class Main {
    
    public static File trainDir;
    public static File devDir;
    public static File testDir;
    
    public static final String[] XML_EXTENSION = new String[]{"xml"}; 
    public static final String RESOURCES_DIR = "main\\resources";
    public static final String SVM_DIR = "main\\resources\\svm_light_TK";
    public static final String MODEL_DIR = "main\\resources\\models";
    
    public static boolean train = true;
    
    public static FileOutputStream log;    
    
    public Main(String[] args) throws IOException {      
        testDir = new File(args[args.length-1]);
        if (args.length == 6) {
            trainDir = new File(args[1]);
            devDir = new File(args[3]);
        }   
        StanfordParser.initializeStanfordParser();
        VerbNet.initializeVerbNet(new File("main\\resources\\verbnet.txt"));
        GeneralInquirer.initializeGeneralInquirer(new File("main\\resources\\inquirer.txt"));
        WordNet.initializeWordNet();
        log = new FileOutputStream("log.txt");        
    }    
    
    public void markUpRawXMLWithRelations(String relation, Trainer trainer, Trainer tester) throws IOException {
        Main.train = true;
        Features.clearFeatureLists();     
        
        trainer.setFeatures(relation);
        Features.setUniFeaturesSizes();   
        
        String train = "main\\data\\train"+relation+".txt";
        String model = RESOURCES_DIR+"\\models\\model"+relation+".txt"; 
        
        Main.train = false;
        tester.setFeatures(relation);
        String test = "main\\data\\test"+relation+".txt";
        tester.writeLabelledSVMData(new FileOutputStream(test), relation);    
        
        String result = "main\\data\\result"+relation+".txt";        
        Evaluator.classify(test, model, result);
        
        Annotator.storeUnLabelledSVMData(tester.fileDocumentObject, relation);        
    }    
    
    public void markUpRawXMLWithRelations() throws SAXException, ParserConfigurationException, IOException, UnsupportedEncodingException, InterruptedException {
        Collection<File> xmlFiles = FileUtils.listFiles(new File(RESOURCES_DIR+"\\space-eval\\train\\"), XML_EXTENSION, true);
        xmlFiles.addAll(FileUtils.listFiles(new File(RESOURCES_DIR+"\\space-eval\\dev\\"), XML_EXTENSION, true));
        xmlFiles.addAll(FileUtils.listFiles(new File(RESOURCES_DIR+"\\space-eval\\test\\"), XML_EXTENSION, true));
        Trainer trainer = new Trainer(xmlFiles);
        trainer.generateTriplets();
        
        xmlFiles = FileUtils.listFiles(testDir, XML_EXTENSION, true);
        Trainer tester = new Trainer(xmlFiles);
        tester.generateTriplets();
        
        markUpRawXMLWithRelations("LINK", trainer, tester);                
        markUpRawXMLWithRelations("MOVELINK", trainer, tester);                     
        
        //store extracted trigger movers
        String[] testArr = FileUtils.readFileToString(new File("main\\data\\testMOVELINK.txt")).split("\\n");
        String[] resultArr = FileUtils.readFileToString(new File("main\\data\\resultMOVELINK.txt")).split("\\n");
        List<String> extractedTriggerMovers = Evaluator.getExtractedElements(testArr, resultArr);
        for (String extractedTriggerMover : extractedTriggerMovers)
            SpatialRelation.fileTriggerMoverRoleOtherElement.put(extractedTriggerMover, null);        
        
        for (String relation : SpatialRelation.ORDERED_SIEVES) {
            markUpRawXMLWithRelations(relation, trainer, tester);
            
            testArr = FileUtils.readFileToString(new File("main\\data\\test"+relation+".txt")).split("\\n");
            resultArr = FileUtils.readFileToString(new File("main\\data\\result"+relation+".txt")).split("\\n");
            SpatialRelation.fileTriggerMoverRoleOtherElement = getMovelinkSubpart(testArr, resultArr, SpatialRelation.fileTriggerMoverRoleOtherElement, relation);                    
        }
        
        Annotator annotator = new Annotator(tester.fileDocumentObject);
        annotator.markUpSpatialRelations();        
    }
    
    public void trainRawXMLWithRelations(String relation, Trainer trainer) throws IOException {
        Main.train = true;
        Features.clearFeatureLists();     
        
        trainer.setFeatures(relation);
        Features.setUniFeaturesSizes();   
        
        String train = "main\\data\\train"+relation+".txt";
        trainer.writeLabelledSVMData(new FileOutputStream(train), relation);        
        String model = "main\\data\\model"+relation+".txt";
        Evaluator.train(Evaluator.relationBestC.get(relation), Evaluator.relationBestCost.get(relation), Evaluator.relationBestT.get(relation), train, model);        
    }    
    
    /**
     * 
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws InterruptedException 
     */
    public void trainModel() throws SAXException, ParserConfigurationException, IOException, UnsupportedEncodingException, InterruptedException {
        Collection<File> xmlFiles = FileUtils.listFiles(trainDir, XML_EXTENSION, true);
        xmlFiles.addAll(FileUtils.listFiles(devDir, XML_EXTENSION, true));
        xmlFiles.addAll(FileUtils.listFiles(testDir, XML_EXTENSION, true));
        
        Trainer trainer = new Trainer(xmlFiles);
        trainer.generateTriplets();
        
        trainRawXMLWithRelations("LINK", trainer);                
        trainRawXMLWithRelations("MOVELINK", trainer);                     
        
        for (String relation : SpatialRelation.ORDERED_SIEVES) {
            trainRawXMLWithRelations(relation, trainer);
        }
    }
    
    public void trainAndMarkUpRawXMLWithRelations(String relation, Trainer trainer, Trainer tester) throws IOException {
        Main.train = true;
        Features.clearFeatureLists();     
        
        trainer.setFeatures(relation);
        Features.setUniFeaturesSizes();   
        
        String train = "main\\data\\train"+relation+".txt";
        trainer.writeLabelledSVMData(new FileOutputStream(train), relation);
        
        Main.train = false;
        tester.setFeatures(relation);
        String test = "main\\data\\test"+relation+".txt";
        tester.writeLabelledSVMData(new FileOutputStream(test), relation);    
        
        String model = "main\\data\\model"+relation+".txt";
        String result = "main\\data\\result"+relation+".txt";        
        Evaluator.applyBestModel(relation, train, model, test, result);        
        
        Annotator.storeUnLabelledSVMData(tester.fileDocumentObject, relation);        
    }
    
    /**
     * 
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws InterruptedException 
     */
    public void trainAndMarkUpRawXMLWithRelations() throws SAXException, ParserConfigurationException, IOException, UnsupportedEncodingException, InterruptedException {
        Collection<File> xmlFiles = FileUtils.listFiles(trainDir, XML_EXTENSION, true);
        xmlFiles.addAll(FileUtils.listFiles(devDir, XML_EXTENSION, true));
        Trainer trainer = new Trainer(xmlFiles);    
        trainer.generateTriplets();
        
        xmlFiles = FileUtils.listFiles(testDir, XML_EXTENSION, true);
        Trainer tester = new Trainer(xmlFiles);
        tester.generateTriplets();      
        
        trainAndMarkUpRawXMLWithRelations("LINK", trainer, tester);                
        trainAndMarkUpRawXMLWithRelations("MOVELINK", trainer, tester);             
        
        //store extracted trigger movers
        String[] testArr = FileUtils.readFileToString(new File("main\\data\\testMOVELINK.txt")).split("\\n");
        String[] resultArr = FileUtils.readFileToString(new File("main\\data\\resultMOVELINK.txt")).split("\\n");
        List<String> extractedTriggerMovers = Evaluator.getExtractedElements(testArr, resultArr);
        for (String extractedTriggerMover : extractedTriggerMovers)
            SpatialRelation.fileTriggerMoverRoleOtherElement.put(extractedTriggerMover, null);
        
        for (String relation : SpatialRelation.ORDERED_SIEVES) {
            trainAndMarkUpRawXMLWithRelations(relation, trainer, tester);
            
            testArr = FileUtils.readFileToString(new File("main\\data\\test"+relation+".txt")).split("\\n");
            resultArr = FileUtils.readFileToString(new File("main\\data\\result"+relation+".txt")).split("\\n");
            SpatialRelation.fileTriggerMoverRoleOtherElement = getMovelinkSubpart(testArr, resultArr, SpatialRelation.fileTriggerMoverRoleOtherElement, relation);                    
        }

        Annotator annotator = new Annotator(tester.fileDocumentObject);
        annotator.markUpSpatialRelations();
    }
       
    /**
     * Develops model for extracting links by optimizing model parameters.
     * 
     * @param relation
     * @param trainer
     * @param developer
     * @throws IOException 
     */
    public void developRelationExtractionModel(String relation, Trainer trainer, Trainer developer) throws IOException {
        Main.train = true;
        Features.clearFeatureLists();

        //set features for instances of this relation.
        trainer.setFeatures(relation);
        Features.setUniFeaturesSizes();
        //write svm training data for instances of this relation.
        String train = "main\\data\\train"+relation+".txt";
        trainer.writeLabelledSVMData(new FileOutputStream(train), relation);

        Main.train = false;
        developer.setFeatures(relation);
        String devel = "main\\data\\test"+relation+".txt";
        developer.writeLabelledSVMData(new FileOutputStream(devel), relation);

        //develops RELATION model
        //in other words, optimizes C and Cost parameters for relation extraction
        String model = "main\\data\\model"+relation+".txt";
        String result = "main\\data\\result"+relation+".txt";
        Evaluator.develop(relation, train, model, devel, result);
    }
    
    public void developRelationExtractionModel() throws SAXException, ParserConfigurationException, IOException, UnsupportedEncodingException, InterruptedException {
        Collection<File> xmlFiles = FileUtils.listFiles(trainDir, XML_EXTENSION, true);
        Trainer trainer = new Trainer(xmlFiles);
        trainer.generateTriplets();
        
        xmlFiles = FileUtils.listFiles(devDir, XML_EXTENSION, true);
        Trainer developer = new Trainer(xmlFiles);
        developer.generateTriplets();
        
        developRelationExtractionModel("LINK", trainer, developer);                
        developRelationExtractionModel("MOVELINK", trainer, developer);     
        
        //store extracted trigger movers
        String[] testArr = FileUtils.readFileToString(new File("main\\data\\testMOVELINK.txt")).split("\\n");
        String[] resultArr = FileUtils.readFileToString(new File("main\\data\\resultMOVELINK.txt")).split("\\n");
        List<String> extractedTriggerMovers = Evaluator.getExtractedElements(testArr, resultArr);
        for (String extractedTriggerMover : extractedTriggerMovers)
            SpatialRelation.fileTriggerMoverRoleOtherElement.put(extractedTriggerMover, null);                
        
        //iterate over the optional roles
        for (int i = 0; i < 6; i++) {
            //iterates through optional roles and finds the role that produces results with highest precision
            for (String relation : SpatialRelation.RELATION_ROLENAME.keySet()) {
                if (SpatialRelation.ORDERED_SIEVES.contains(relation))
                    continue;
                developRelationExtractionModel(relation, trainer, developer);
            }
            //assigns the role with highest precision
            Evaluator.setSievesOrder(developer.fileDocumentObject);                             
        }        
    }
    
    /**
     * 
     * @param args which are the data directories for training, developing and testing models.
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     * @throws java.io.UnsupportedEncodingException 
     * @throws java.lang.InterruptedException 
     */
    public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException, UnsupportedEncodingException, InterruptedException {
        Main main;
        if (args.length == 6 && args[0].equals("-train") && (new File(args[1])).exists() &&
                args[2].equals("-dev") && (new File(args[3])).exists() && 
                args[4].equals("-test") && (new File(args[5])).exists()) {
            main = new Main(args);
            main.developRelationExtractionModel();            
            main.trainAndMarkUpRawXMLWithRelations();
            //main.trainModel();
        }     
        else if (args.length == 2 && args[0].equals("-test") && (new File(args[1])).exists()) {
            SpatialRelation.setOrderedSievesList();
            main = new Main(args);
            main.markUpRawXMLWithRelations();
        }
        else {
            System.out.println("=======================================");
            System.out.println("To train and develop a new spatial relation extraction model, "
                    + "and annotate test data with spatial relations using the newly developed model.");
            System.out.println("Usage: java -Dwordnet.database.dir=main\\resources\\wordnet-dict\\ main.java.spatialrelex.Main "
                    + "-train <YOUR TRAIN DIRECTORY> "
                    + "-dev <YOUR DEVELOPMENT DIRECTORY> "
                    + "-test <YOUR TEST DIRECTORY>");
            System.out.println(" ");
            System.out.println("To annotate test data using our pre-trained spatial relation extraction models.");
            System.out.println("Usage: java -Dwordnet.database.dir=main\\resources\\wordnet-dict\\ main.java.spatialrelex.Main "
                    + "-test <YOUR TEST DIRECTORY>");
            System.out.println("*The output in both cases will be writtent to the `src\\output\\` folder*.");
            System.out.println("=======================================");
            
            System.exit(1);
        }
        
    }
    
}
