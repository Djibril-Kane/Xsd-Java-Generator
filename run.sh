#!/bin/bash
javac src/XmlNode.java src/XsdToJavaGenerator.java -d src/
java -cp src XsdToJavaGenerator bibliotheque.xsd generated
javac -cp src generated/*.java -d generated
java -cp "src;generated" TestMain