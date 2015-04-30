BreizhCamp Badge
===

Générateur de badge pour le BreizhCamp.

Pré-requis
----

Java

Build
---

Exécuter la commande suivante à la racine du projet :

```bash
$ ./gradlew build
```

Deux archives au contenu identique, un fichier `.tar` et un fichier `.zip`, sont générées dans le répertoire `build/distributions`.

Installation
---

Décompresser l'archive de votre choix dans le répertoire de votre choix. Vous obtenez l'arborescence suivante :

```
badge-generator-1.0
├── bin
│   ├── badge-generator
│   └── badge-generator.bat
└── lib
    ├── badge-generator-x.x.jar
    ├── groovy-2.4.3.jar
    ├── groovycsv-1.0.jar
    ├── itextpdf-5.4.5.jar
    └── opencsv-2.1.jar
```

Utilisation
---

Exécuter le script `badge-generator` pour avoir toutes les informations nécessaires.