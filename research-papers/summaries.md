## t-Closeness through Microaggregation: Strict Privacy with Enhanced Utility Preservation 
http://ieeexplore.ieee.org/document/7110607/?reload=true

A normál k-anonimizált microdata az eredeti adatok eloszlásának (hozzávetőleges) ismeretével reidentifikálható, ez ellen több védelmi szabály létezik. A t-closeness az egyik legszigorúbb ezek közül, előírja hogy az egyes k-anonim ekvivalencia osztályok meghatározó attribútumainak eloszlása a tejes adathalmaz eloszlásától milyen "messze" lehet. Ez a gyakorlatban valamilyen eloszlás-távolságfüggvény (pl: Earth Mover’s distance) maximális megengedett értékének meghatározásával történik.

Több eljárást is leír a t-closeness előállítására: van amelyik ennek a tulajonságnak a kielégítésével kezdi majd a k-anonimitás teljesítésével folytatja, van amelyik fordítva építkezik.

## Mondrian Multidimensional K-Anonymity 
https://www.utdallas.edu/~muratk/courses/privacy08f_files/MultiDim.pdf

Bevezeti a többdimenziós k-anonimizálás fogalmát. Az egydimenziós partícionálást használva is lehetőség van több attribútum menti "darabolásra", de a többdimenziós partíciónálással nem szükséges egy attribútum minden szeletét a többi attribútum minden más szeletével összepárosítani. Formálisabban: az egydimenziós partícionálás az attribútumok felosztásainak kereszt szorzatait használja patrícióknak, míg a többdimenziós partícionálás az összes attribútum kereszt szorzatának (több dimenziós) felosztását használja partícióknak.

Az eljárásnak az egydimenziós k-anonimizálás speciális esete, így legalább olyan jó eredmények érhetők el vele, általában jobb eredményt is ad, azonban a bonyolultsága is jóval nagyobb.

## Utility-Based Anonymization Using Local Recoding
http://www.cs.cuhk.hk/~adafu/Pub/localrecoding-kdd06.pdf

A különböző Quasi-Identifierek külső adathalmazokban más-más mértékben és adatokhoz társítva jelenhetnek meg, ennek hatására a vissza-identifikációs kísérletekben eltérő szerepet játszhatnak. Az anonimizáció biztosításának érdekében a microdata kiadója figyelembe veheti ezen atribútumok ilyen típusú hasznosságát a lehetséges vissza-identifikációs eljárásokban. Ezeket az attribútum jellemzőket felhasználva javíthatjuk a heurisztikus k-anonimizációs eljárásunkat és csökkenthetjük az információveszteséget.

2 heurisztikus algoritmust is leír ezen adatok felhasználására, a Top-Down és a Bottom-Up módszert.
