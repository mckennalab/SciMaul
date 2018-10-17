## SciMaul, a high-dimensional barcode splitter for single-cell sequencing experiments

<p align="center">
    <img src ="https://github.com/aaronmck/SciMaul/raw/master/images/sci_maul.png" />
</p>

SciMaul is a index/barcode splitter for sequencing reads. SciMaul handles 'high-dimensional' configurations that include indices, UMI sequences, and static barcodes, producing a heirarchical directory structure based on these 'dimensions'. The goal is to have one tool that can split SCI experiments, 10X single-cell RNA-Seq, and droplet experiments within a single framework, all the while providing some simple statistics and data cleanup. SCIMaul's name comes from adding single-cell capabilities (SCI) to it's predecessor [Maul](https://github.com/aaronmck/Maul). SCIMaul isn't rocket-science, but I really wanted one common tool I could use to pre-process all single-cell data, and one input format that I could use to describe the layout of any sequencing run. 

## Things SCIMaul does:
- Splits on arbritrary numbers of indicies, located anywhere within a set of reads
- Handles common configuration of read files (read 1 Fastq only, read 1 and 2, or with index files (up to 2))
- Is somewhat optimized for speed in these applications, given the large numbers of dimensions
- Can pull sequences from reads that you don't want to split on (static barcodes) or sequences you want to completely move to the read header (UMI sequences)
- When it can't find the specified barcode, it can align to the known barcode list to recover more reads (currently at a significant speed cost)
- Output a metadata file about the number of reads per cell, and other statistics
- You encode everything about a your configuration in one(ish) json file (see below)

## Things it doesn't do well and TODO:
- On the fly compressed output. Right now it can only output fastq files
- Handle splitting directory from BCL/Illumina sequencer output. You'll need an unfiltered set of fastq file(s) to start things off


## Building
The SCIMaul releases contain a jar file, so you shouldn't have to build SCIMaul unless you want to. To build the tool you'll need to have at least Java 1.8 and SBT installed. Once you do, just type:

sbt compile

to build the tool, and:

sbt assembly

to make a single jar version with all of the dependencies built in.

## Running

## Recipe format

SCIMaul splits reads based on a recipe file; check out the _recipe_files_ directory for an example recipe file. This recipe file is a simple json file that describes the layout of sequencing run.  The easiest way is to walk through a somewhat complex example file:

```json
{
  "name": "RNA-Seq",
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

Each json recipe file must have a name, and a set of barcodes (the top few lines). This describes the name of the experiment and the layout of important sequences over the reads. Each barcode within the json barcode array must have the foloowing fields:
- name: this can be anything, but it's useful to describe the barcode / index in a way that'll you'll remember
- read: which sequencing read to use when looking for this index. options are READ1, READ2, INDEX1, or INDEX2
- start: where this sequence starts within the read of interest
- length: how long this barcode is
- use: what kind of index/barcode this is. _INDEX_ indicates this barcode has a known set of set of sequences that we should compare against, and sort based on that loookup. When use is set to _INDEX_ there should be another json field _sequences_ that points to the corresponding tab-seperated list of sequence names and bases to use in the lookup. The field _maxerror_ can be used to set the maximum number of mismatches that are tollerated against this list. Two other options are available, _UMI_ and _STATIC_. Both don't require a known list, and instead will be added to the read header on output. The difference between the two are that _UMI_ sequences will be extracted and removed from the read sequence, whereas _STATIC_ will be left within the read.

