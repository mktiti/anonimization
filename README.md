# KAnon
## Data Anonimization Benchmark Framework and Library

### Command line arguments
* Data file location, following `-f` or `--datafile`, default is `data.csv`
* Output file, following `-o` or `--output`, default is `output.csv`
* Data descriptor file location, following `-d` or `--descriptor`, default is `descriptor.conf`

### Types
The library is going to be type-aware and be able to use different anonymization techniques base on the types of each data attribute.
There is built in support for 
* Integers (with custom value ranges)
* Dates (with custom value ranges)
* Strings (with custom length ranges)
* Custom hierarchic enums

#### Custom hierarchic enums:
Data can have attributes of custom enums, these attributes can only take on a predefined set of (string) values, which may be hierarchically organized.

### Data descriptor file
In case of a standalone run, a data descriptor file is used to define the input's attribute layout.
The descriptor file can be supplied by the `-d` or `--descriptor` flags and is `desctor.conf` by default.
The structure of the file follows an easy to read DSL syntax, consisting of two main parts, the enum declarations (optional) and attribute eclaration.

#### Enum declaration
The possible enum categories are defined in the `Enums { ... }` tag.
Each tag have a name and an optional block which contains its children. The top level children of `Enums` are the custom types and must have children of their own.
Example - A gender type with possible values of male, female and other/unknown and a multi-level location type:
```
Enums {
    Gender { Male, Female, Other/Unknown }
    Location {
        Asia,
        Africa,
        Europe {
            West-Europe {
                UK,
                Ireland,
                France,
                ...
            },
            North-Europe {
                Norway,
                Sweden,
                Finland,
                ...
            },
            Middle-Europe {
                Hungary,
                Slovakia,
                Poland,
                ...
            }
        },
        Australia,
        America { North-America, South-America }
    }
}
```

#### Attribute declaration
The other big part of the data descriptor is the attribute section, which actually describes the structural layout of the input.
In the `Attributes { ... }` tag the column definitions can be written line by line. 
Definitions follow the syntax: 

`{attribute name} [quasi] [secret] {data type} [data type constraint]`


A name is mandatory, the quasi and secret flags are used to mark quasi identifiers and secrets.
The data type is required, and its allowed values are `Int`, `String`, `Date` and the previously defined enum types (top level declarations of the `Enums` tag). For built in types, an optional constraint may be appliable - possible values include:
* `Int [10;]` -> Integer, >= 10
* `Int [;10]` -> Integer, <= 10
* `Int [10;20]` -> Integer, 10 <= and <= 20
* `String [10;]` -> Similar to Int, but the range limit is on length
* `Date [2010-01-15;2011-02-02]` -> Similar to Int, range limit is on value
* `Date yyyy.dd.MM [;2000.21.01]` -> Date, with range limit in custom format

Attributes examples (references the previous enum declarations):
```
Attributes {
    id secret Int [0;]
    name quasi secret String [2;50]
    birthdate quasi Date [1900-01-01;]
    birthplace quasi Location
    gender quasi Gender
    disease secret String
}
```
