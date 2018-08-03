package edu.emory.pathology.hladpb1.imgtdb;

import edu.emory.pathology.hladpb1.imgtdb.data.Allele;
import edu.emory.pathology.hladpb1.imgtdb.data.Codon;
import edu.emory.pathology.hladpb1.imgtdb.data.HypervariableRegion;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;

/**
 * This finder class loads our local data classes from the IMGT data classes.
 * Our local data classes are optimized for the HLA-DPB1 classifier.
 * 
 * @author ghsmith
 */
public class AlleleFinder {

    private static final Logger LOG = Logger.getLogger(AlleleFinder.class.getName());

    private String xmlFileName;
    private List<Allele> alleleList;

    public AlleleFinder() {
    }

    public AlleleFinder(String xmlFileName) {
        this.xmlFileName = xmlFileName;
    }
    
    public Allele getAllele(String alleleName) throws JAXBException {
        return getAlleleList().stream().filter((allele) -> (alleleName.equals(allele.getAlleleName()))).findFirst().get();
    }
    
    public List<Allele> getAlleleList() throws JAXBException {
        if(alleleList == null) {
            alleleList = new ArrayList();
            JaxbImgtFinder imgtFinder = new JaxbImgtFinder(xmlFileName);
            edu.emory.pathology.hladpb1.imgtdb.jaxb.imgt.Alleles imgtAlleles = imgtFinder.getAlleles();
            // Process each IMGT HLA-DPB1 allele that has a sequence element.
            imgtAlleles.getAllele().stream().filter((imgtAllele) -> (imgtAllele.getName().startsWith("HLA-DPB1")) && imgtAllele.getSequence() != null).forEach((imgtAllele) -> {
                Allele allele = new Allele();
                alleleList.add(allele);
                allele.setVersion(imgtAllele.getReleaseversions().getCurrentrelease());
                allele.setAlleleName(imgtAllele.getName());
                allele.setCodonMap(new TreeMap());
                final Iterator translationIterator = imgtAllele.getSequence().getFeature().stream().filter((feature) -> (feature.getFeaturetype().equals("Protein"))).findFirst().get().getTranslation().chars().iterator();
                // Process each each IMGT exon. The codon position is deduced
                // from the CDNA coordinate and the reading frame. Note that
                // NULL alleles will not have protein sequence for all codons.
                imgtAllele.getSequence().getFeature().stream().filter((feature) -> (feature.getFeaturetype().equals("Exon"))).forEach((feature) ->  {
                    for(int cdnaCoordinate = feature.getCDNACoordinates().getStart().intValueExact(); cdnaCoordinate <= feature.getCDNACoordinates().getEnd().intValueExact(); cdnaCoordinate++) {
                        int codonNumber = ((cdnaCoordinate - 1) / 3) + 1; // codon number starts at one (not zero)
                        int positionInCodon = (cdnaCoordinate - 1) - (((cdnaCoordinate - 1) / 3) * 3) + 1; // position in codon starts at one (not zero)
                        // -HARD-CODED SIGNAL PEPTIDE LENGTH--------------------
                        codonNumber = codonNumber - 29; // signal peptide is 29 codons
                        // -----------------------------------------------------
                        if(allele.getCodonMap().get(codonNumber) == null) {
                            if(translationIterator.hasNext()) {
                                if(allele.getCodonMap().size() > 0 || positionInCodon == 1) { // IMGT translation starts with first whole codon
                                    Codon codon = new Codon();
                                    codon.setCodonNumber(codonNumber);
                                    allele.getCodonMap().put(codon.getCodonNumber(), codon);
                                    codon.setAminoAcid(String.format("%c", translationIterator.next()));
                                }
                            }
                            else {
                                assert
                                    // It is a NULL allele...
                                    allele.getAlleleName().endsWith("N")
                                    // ...the exon doesn't end on a codon boundary (some alleles don't have all exons represented)
                                    || ((feature.getCDNACoordinates().getEnd().intValueExact() % 3) != 0)
                                    // ...or it is a stop codon.
                                    || (
                                        "TAG".equals(imgtAllele.getSequence().getNucsequence().substring(feature.getSequenceCoordinates().getStart().intValueExact() + (cdnaCoordinate - feature.getCDNACoordinates().getStart().intValueExact() - positionInCodon)).substring(0, 3))
                                        || "TAA".equals(imgtAllele.getSequence().getNucsequence().substring(feature.getSequenceCoordinates().getStart().intValueExact() + (cdnaCoordinate - feature.getCDNACoordinates().getStart().intValueExact() - positionInCodon)).substring(0, 3))
                                        || "TGA".equals(imgtAllele.getSequence().getNucsequence().substring(feature.getSequenceCoordinates().getStart().intValueExact() + (cdnaCoordinate - feature.getCDNACoordinates().getStart().intValueExact() - positionInCodon)).substring(0, 3))
                                    )    
                                    : allele.getAlleleName() + " translation too short";
                            }
                        }
                    }
                });
                assert !translationIterator.hasNext() : allele.getAlleleName() + " translation too long"; // should not have any leftover translation
                allele.setNullAllele(allele.getAlleleName().endsWith("N"));
                // Synonymous logic requires alleles to be processed in order
                // (i.e., the "master" allele first).
                {
                    Pattern pattern = Pattern.compile("(HLA-DPB1\\*[0-9]*:[0-9]*)[:0-9N]*");
                    Matcher matcherThis = pattern.matcher(allele.getAlleleName());
                    matcherThis.find();
                    Allele synonymousAllele = null;
                    for(Allele candidateSynonymousAllele : alleleList) {
                        Matcher matcherSyn = pattern.matcher(candidateSynonymousAllele.getAlleleName());
                        matcherSyn.find();
                        if(allele != candidateSynonymousAllele && matcherThis.group(1).equals(matcherSyn.group(1))) {
                            synonymousAllele = candidateSynonymousAllele;
                            break;
                        }
                    }
                    if(synonymousAllele != null) {
                        String proteinSequenceThis = allele.getCodonMap().values().stream().map(Codon::getAminoAcid).collect(Collectors.joining());
                        String proteinSequenceSyn = synonymousAllele.getCodonMap().values().stream().map(Codon::getAminoAcid).collect(Collectors.joining());
                        if(allele.getNullAllele()) {
                            if(proteinSequenceThis.endsWith("X")) {
                                proteinSequenceThis = proteinSequenceThis.substring(0, proteinSequenceThis.length() - 1);
                            }
                        }
                        assert
                            proteinSequenceSyn.contains(proteinSequenceThis)
                            || proteinSequenceThis.contains(proteinSequenceSyn)
                            : allele.getAlleleName() + " is not really synonymous with " + synonymousAllele.getAlleleName();
                        allele.setSynonymousAlleleName(synonymousAllele.getAlleleName());
                        allele.setSynonymousAlleleProteinShorter(!proteinSequenceSyn.contains(proteinSequenceThis));
                    }
                }
            });
            // Add sequence number for sorting.
            int sequenceNumber = 1;
            for(Allele allele : alleleList) {
                allele.setSequenceNumber(sequenceNumber++);
            }
            LOG.info(String.format("%d HLA-DPB1 alleles loaded", alleleList.size()));
            LOG.info(String.format("version is %s", alleleList.get(0).getVersion()));
        }
        return alleleList;
    }

    public void assignHypervariableRegionVariantIds(List<HypervariableRegion> hypervariableRegionList) throws JAXBException {
        getAlleleList().stream().forEach((allele) -> {
            allele.setHvrVariantMap(new TreeMap<>());
            hypervariableRegionList.stream().forEach((hypervariableRegion) -> {
                StringBuilder hvrProteinSequence = new StringBuilder();
                hypervariableRegion.getCodonNumberList().stream().forEach((codonNumber) -> {
                    Codon codon = allele.getCodonMap().get(codonNumber);
                    if(codon != null) {
                        codon.setHypervariableRegionName(hypervariableRegion.getHypervariableRegionName());
                    }
                    hvrProteinSequence.append(codon == null ? "*" : codon.getAminoAcid());
                });
                Allele.HypervariableRegionVariantRef hvrvRef = new Allele.HypervariableRegionVariantRef();
                allele.getHvrVariantMap().put(hypervariableRegion.getHypervariableRegionName(), hvrvRef);
                hvrvRef.variantId = hvrProteinSequence.toString(); // default if the protein sequence does not match an established hypervariable region variant
                allele.setSingleAntigenBead(false);
                hypervariableRegion.getVariantMap().values().stream().forEach((hvrVariant) -> {
                    hvrVariant.getProteinSequenceList().stream().filter((proteinSequence) -> (hvrProteinSequence.toString().equals(proteinSequence))).findFirst().ifPresent((proteinSequence) -> {
                        hvrvRef.variantId = hvrVariant.getVariantId();
                        hvrVariant.getBeadAlleleNameList().stream().filter((beadAlleleName) -> (allele.getAlleleName().startsWith(beadAlleleName))).findFirst().ifPresent((beadAlleleName) -> {
                            allele.setSingleAntigenBead(true);
                        });
                    });
                });
            });
        });
    }

    public void assignHypervariableRegionVariantMatches(String referenceAlleleName) throws JAXBException {
        Allele referenceAllele = getAllele(referenceAlleleName);
        getAlleleList().stream().forEach((allele) -> {
            allele.setReferenceForMatches(referenceAllele.equals(allele));
            allele.setMatchesHvrCount((int)referenceAllele.getHvrVariantMap().keySet().stream().filter((hvrName) -> (
                referenceAllele.getHvrVariantMap().get(hvrName).variantId.equals(allele.getHvrVariantMap().get(hvrName).variantId))).count()
            );
            allele.getHvrVariantMap().keySet().stream().forEach((hvrName) -> {
                allele.getHvrVariantMap().get(hvrName).matchesReference
                    = referenceAllele.getHvrVariantMap().get(hvrName).variantId
                    .equals(allele.getHvrVariantMap().get(hvrName).variantId);
            });
            allele.getCodonMap().keySet().stream().forEach((codonNumber) -> {
                allele.getCodonMap().get(codonNumber).setMatchesReference(null);
                if(allele.getCodonMap().get(codonNumber) != null) {
                    if(referenceAllele.getCodonMap().get(codonNumber) != null) {
                        allele.getCodonMap().get(codonNumber).setMatchesReference(
                            referenceAllele.getCodonMap().get(codonNumber).getAminoAcid()
                            .equals(allele.getCodonMap().get(codonNumber).getAminoAcid())
                        );
                    }
                }
            });
        });
    }
    
}
