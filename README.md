# SpatialRelEx
SpatialRelEx: Spatial Relation Extraction System

A multi-pass sieve classifier for automatically annotating spatial relations between spatial elements in text. 

The SpatialRelEx tool has been written in Java and is released as free software.

#### Prerequisites:

1) Input data must be in `xml` format (For convenience, [SpaceEval](http://alt.qcri.org/semeval2015/task8/) data used to train, develop, and test **SpatialRelEx** has been included in the `main\resources\space-eval` folder.)

2) External libraries not included in this download but needed by the tool are [Apache Commons IO v2.4](https://commons.apache.org/proper/commons-io/download_io.cgi) and [Stanford CoreNLP](http://nlp.stanford.edu/software/corenlp.shtml#Download). Before running the tool, these libraries must be downloaded and the Java classpath must be set with the paths to the libraries' `jar` files.

3) In addition, the tool also relies on [JAWS](http://lyle.smu.edu/~tspell/jaws/), the Java API for WordNet Searching, which is included in `main\resources\` folder. The Java classpath must be set to its `jar` file as well.

### Usage:

1) To train and develop a new spatial relation extraction model, and annotate test data with spatial relations using the newly developed model.

    java -Dwordnet.database.dir=main\resources\wordnet-dict\ main.java.spatialrelex.Main -train <YOUR TRAIN DIRECTORY> -dev <YOUR DEVELOPMENT DIRECTORY> -test <YOUR TEST DIRECTORY>

2) To annotate test data using our pre-trained spatial relation extraction models.

    java -Dwordnet.database.dir=main\resources\wordnet-dict\ main.java.spatialrelex.Main -test <YOUR TEST DIRECTORY>

*The annotated output in both cases will be writtent to the `src\output\` folder*.

#### Citation:

The relation extraction system is described in:

Sieve-Based Spatial Relation Extraction with Expanding Parse Trees. Jennifer D'Souza and Vincent Ng. In Proceedings of the 2015 Conference on Empirical Methods in Natural Language Processing (EMNLP), pages 758–768.

*Please note: The experimental setup of SpatialRelEx in Usage #1 is 1-fold cross validation, and hence it's output evaluation may differ from the paper in which the experimental setup was 5-fold cross validation. But the methodology for extracting spatial relations is implemented exactly as described in the paper. With slight modification to the source code enabling five-fold cross validation, it should be able to produce the results in the paper.*
