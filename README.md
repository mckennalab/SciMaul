# SciMaul, a high-dimensional barcode splitter for single-cell sequencing experiments

<div style="text-align:center"><img src ="https://github.com/aaronmck/SciMaul/raw/master/images/sci_maul.png" /></div>

SciMaul splits transforms input reads into cells, splitting on any number of barcodes located anywhere within the reads. SciMaul handles complex configurations that include indices, UMI sequences, and static barcodes, producing a heirarchical directory structure based on these 'dimensions'. The goal is to have one tool that can split SCI experiments, 10X single-cell RNA-Seq, and droplet experiments within a single framework, all the while providing some simple statistics and data cleanup. SCIMaul's name comes from adding SCI to it's predecessor [Maul](https://github.com/aaronmck/Maul), my previous barcode splitter. SCIMaul isn't rocket-science, but I really wanted one common tool I could use to pre-process all single-cell data, and one input format that I could use to describe the layout of any sequencing run. 

# Building
The SCIMaul releases contain a jar file, so you shouldn't have to build SCIMaul unless you want to. To build the tool you'll need to have at least Java 1.8 and SBT installed. Once you do, just type:

sbt compile

to build the tool, and:

sbt assembly

to make a single jar version with all of the dependencies built in.

# Running

# Recipe format

SCIMaul splits reads based on a recipe file; check out the _recipe_files_ directory for an example recipe file. This recipe file is a simple JSON file that describes the layout of sequencing run.  The easiest way is to walk through a somewhat complex example file:

```json
{
    "name": "GESTALT-RNA-Seq",
  "barcodes": [
    {
      "name": "row",
      "read": "INDEX1",
      "start": 0,
      "length": 10,
      "use": "INDEX",
      "sequences": "sci_2_level/SCI_P7.txt"
    },
    {
      "name": "column",
      "read": "INDEX2",
      "start": 0,
      "length": 10,
      "use": "INDEX",
      "sequences": "sci_2_level/SCI_P5.txt"
    },
    {
      "name": "cell",
      "read": "READ1",
      "start": 8,
      "length": 10,
      "use": "INDEX",
      "sequences": "sci_2_level/SCI_RT.txt",
      "maxerror": 1
    },
    {
      "name": "umi",
      "read": "READ1",
      "start": 0,
      "length": 8,
      "use": "UMI"
    },
    {
      "name": "static",
      "read": "READ1",
      "start": 58,
      "length": 10,
      "use": "STATIC"
    }
  ]
}
```


# Name

