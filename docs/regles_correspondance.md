# Règles de correspondance XSD -> Java

Ce document liste, indépendamment du code, les règles utilisées par
`XsdToJavaGenerator` pour transformer un schéma XSD en classes Java.

## 1. Concepts XSD couverts

| Concept XSD | Concept Java | Détail |
|---|---|---|
| `xs:element` + `xs:complexType` (avec `xs:sequence`) | une classe Java, héritant de `XmlNode` | ex. `<livre>` -> `class Livre extends XmlNode` |
| `xs:element type="xs:string"` | champ `String` + getter/setter | ex. `<titre>` -> `String titre` |
| `xs:element type="xs:int"` ou `xs:integer` | champ `Integer` + getter/setter | mapping ajouté en v2 |
| `xs:element type="xs:boolean"` | champ `Boolean` + getter/setter | mapping ajouté en v2 |
| `xs:sequence` | ordre de déclaration des champs dans la classe | respecté tel quel |
| `xs:element ref="X"` (sans `maxOccurs`) | champ de type `X` (classe ou type simple) | résolu comme un lien direct objet -> objet dès le parsing |
| `xs:element ref="X" maxOccurs="unbounded"` | `List<X>` + accesseur pluralisé (`getLivres()`) | le nom du champ est pluralisé automatiquement (ajout d'un `s`) |
| élément racine (jamais référencé par un `ref` ailleurs dans le schéma) | classe "point d'entrée" sur laquelle on appelle `save()` | détecté automatiquement, pas besoin de le préciser en argument |

## 2. Mécanisme de sérialisation

Contrairement à une génération "à plat" où chaque classe produite contiendrait
son propre code de sérialisation, ce générateur sépare les deux
responsabilités :

- **Le générateur** ne produit que la structure de données (champs +
  accesseurs) et fait hériter chaque classe de `XmlNode`.
- **`XmlNode`** (une classe écrite une seule fois, pas générée) fournit
  `save(fichier)` et `toXmlString()`, qui parcourent les champs de l'objet
  par réflexion (`java.lang.reflect.Field`) pour reconstruire le XML :
  - un champ simple (`String`, `Integer`, `Boolean`) devient une balise texte
  - un champ objet imbriqué (`XmlNode`) devient une balise fille
  - un champ `List<XmlNode>` devient une répétition de balises, sans balise
    englobante (conforme à l'exemple du sujet)

Ce découpage a un avantage direct : si demain on change la façon de
sérialiser (indentation, encodage, ajout d'attributs XML...), il suffit de
modifier `XmlNode`, sans reproduire un générateur de code pour la partie
sérialisation.

## 3. Détection de la racine

L'élément racine est le seul élément déclaré au premier niveau du schéma qui
n'apparaît dans aucun `ref="..."` d'un autre élément. Aucune configuration
manuelle n'est nécessaire : le générateur calcule l'ensemble des noms
référencés et en déduit l'élément qui n'y figure pas.

## 4. Limites volontaires

Conformément à la consigne ("n'allez pas dans les concepts trop avancés
d'XSD"), les éléments suivants ne sont **pas** gérés :

- `xs:attribute` (attributs XML, par opposition aux éléments)
- `xs:choice`, `xs:all` (uniquement `xs:sequence` est supporté)
- `xs:restriction`, `xs:pattern`, `xs:enumeration` et autres contraintes
- espaces de noms multiples / imports de schémas (`xs:import`, `xs:include`)
- types simples autres que `xs:string`, `xs:int`/`xs:integer`, `xs:boolean`
  (tout autre type est ramené à `String` par défaut)

Ces limites couvrent largement l'exemple demandé dans l'énoncé
(bibliothèque / livre / titre / auteur / éditeur) et la majorité des cas
d'usage simples.
