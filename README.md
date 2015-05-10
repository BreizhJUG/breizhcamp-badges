BreizhCamp Badge
===

Générateur de badge pour le BreizhCamp.

Pré-requis
----

Java 8

Build
---

Exécuter la commande suivante à la racine du projet :

```bash
$ ./gradlew build
```

Deux archives au contenu identique, un fichier `.tar` et un fichier `.zip`, sont générées dans le répertoire `build/distributions`.

Installation
---

Décompresser l'archive dans le répertoire de votre choix. Vous obtenez l'arborescence suivante :

```
breizhcamp-badge-1.0
├── bin
│   ├── breizhcamp-badge
│   └── breizhcamp-badge.bat
└── lib
    ├── breizhcamp-badge-1.0.jar
    ├── commons-cli-1.2.jar
    ├── groovy-2.4.3.jar
    ├── groovycsv-1.0.jar
    ├── itextpdf-5.5.5.jar
    └── opencsv-2.1.jar
```

Utilisation
---

Exécuter le script `breizhcamp-badge -h` pour obtenir de l'aide.

En mode CSV, la première ligne du fichier doit obligatoirement contenir les libellés de colonnes suivants :
* `lastname`,
* `firstname`,
* `company`,
* `email`,
* `ticketType`.