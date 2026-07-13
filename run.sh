#!/bin/bash
echo "=== Validation du parser (autonome) ==="
javac src/XmlNode.java src/XsdToJavaGenerator.java src/ParserValidator.java -d src/
java -cp src ParserValidator bibliotheque.xsd

echo ""
echo "=== Pipeline complet ==="
java -cp src XsdToJavaGenerator bibliotheque.xsd generated
javac -cp src generated/*.java -d generated
java -cp "src;generated" TestMain