# SpatialRelEx
SpatialRelEx: Spatial Relation Extraction System

A multi-class classification system for automatically annotating spatial relations between spatial elements in text. 

The SpatialRelEx tool has been written in Java and is released as free software.

#### Prerequisites:

1) Input data must be in `xml` format.

2) External libraries not included in this download but used by the tool are [Apache Commons IO v2.4](https://commons.apache.org/proper/commons-io/download_io.cgi) and [Stanford CoreNLP](http://nlp.stanford.edu/software/corenlp.shtml#Download). Before running the tool, these libraries must be downloaded and the Java classpath must be set with the paths to the library `jar` files.

3) In addition, the tool also relies on [JAWS](http://lyle.smu.edu/~tspell/jaws/), the Java API for WordNet Searching, which is included in `main\resources\` folder. The Java classpath must be set to its `jar` file.

### Usage:

1) To train and develop a new spatial relation extraction model, and annotate test data with spatial relations using the newly developed model.

    java -Dwordnet.database.dir=main\resources\wordnet-dict\ main.java.spatialrelex.Main -train <YOUR TRAIN DIRECTORY> -dev <YOUR DEVELOPMENT DIRECTORY> -test <YOUR TEST DIRECTORY>

2) To annotate test data using our pre-trained spatial relation extraction models.

    java -Dwordnet.database.dir=main\resources\wordnet-dict\ main.java.spatialrelex.Main -test <YOUR TEST DIRECTORY>

#### Citation:

The relation extraction system is described in:

Sieve-Based Spatial Relation Extraction with Expanding Parse Trees. Jennifer D'Souza and Vincent Ng. In Proceedings of the 2015 Conference on Empirical Methods in Natural Language Processing (EMNLP), pages 758–768.

Please note: While the experimental results described in the paper are from five-fold cross validation, the code released at this site performs only one-fold cross validation. Therefore, the relation extraction results may differ by a margin. However, the system implements the relation extraction methodology exactly as described in the paper, and with slight modification to the source code, should replicate that system in terms of five-fold cross validation results.
