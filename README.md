# descriptoid

A program that extracts visual appearance of persons in text. Contains two parts: One in Java for annotating the text using the [Langforia API](http://vilde.cs.lth.se:9000/api), extracting appearances using dependency trees and rules, as well as a webserver that can be interacted with using the gui in [descriptoid-frontend](https://github.com/dhellstrom/descriptoid-frontend). The other part is in Python and uses Scikit-Learn to train a classifier that can predict whether a sentence contains a description or not.

## Usage

### Java
Sine there is no main program to run the easiest thing is probably to download the project and open it as an IntelliJ (or possibly Eclipse) project and run the files from there. Requires Java 8 or later.

#### RunLocal
Runs the core program locally. Expects two arguments: **filename** and **mode**. **Mode** can be either *existing* or *new*. If it is *existing* it looks for the file name in the **annotations** folder where there already are som documents annotated by Langforia. If **mode** is *new* it looks for the filename in the **corpus** folder which contains plain text documents. It will then send the text to Langforia for annotation before running the description extraction. Depending on the size of the text the annotation process may take a lot of time. 

When done the program will produce an output file containing the descriptions of the characters in the **descriptions** folder.

#### WebServer
Starts a web server. Simply run it and it will set up the server, allowing you to use the descriptoid-frontend gui to process your own text without having to save them in files.

#### AnnotateAndSave
Sends the contents of a plain text file to Langforia for annotation and saves the response. Useful so you don't have to annotate long texts over and over again. Expects a filename in the **corpus** folder.

#### MLInputGenerator
Generates input for the Python program that determies if sentences contain descriptions or not. Expects a filename in the **test_annotations** folder. Note that you have to run **AnnotateAndSave** first if you are using a new text. 

### Python
Contains two files. One for training the classifier and one for using it. There is already a trained classifier so unless you want to train it on new data you can leave it as it is. Uses Python 3.

#### MLPredictor
Reads data from *MLInput.txt*, the file created by **MLInputGenerator**, so make sure you have that. Other than that you just need to run the file.
