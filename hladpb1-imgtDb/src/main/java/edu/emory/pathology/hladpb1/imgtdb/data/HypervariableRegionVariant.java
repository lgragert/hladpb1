package edu.emory.pathology.hladpb1.imgtdb.data;

import java.util.List;

/**
 * Data class for a hypervariable region variant.
 * 
 * @author ghsmith
 */
public class HypervariableRegionVariant {

    // Inner class to make it convenient to reference single antigen beads and
    // their positivitiy.
    public static class BeadAlleleRef {
        
        private String alleleName;
        private Boolean compatBeadPositive;

        public String getAlleleName() {
            return alleleName;
        }

        public void setAlleleName(String alleleName) {
            this.alleleName = alleleName;
        }

        public Boolean getCompatBeadPositive() {
            return compatBeadPositive;
        }

        public void setCompatBeadPositive(Boolean compatBeadPositive) {
            this.compatBeadPositive = compatBeadPositive;
        }

    }
    
    private String variantId;
    private List<String> proteinSequenceList;
    private List<BeadAlleleRef> beadAlleleRefList;
    private Boolean knownReactiveEpitopeForCompat = false;
    private Boolean compatIsRecipientEpitope;
    private Integer compatPositiveSabCount;
    private Integer compatPositiveSabPct;
    private Boolean compatAntibodyConsideredPresent;

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public List<String> getProteinSequenceList() {
        return proteinSequenceList;
    }

    public void setProteinSequenceList(List<String> proteinSequenceList) {
        this.proteinSequenceList = proteinSequenceList;
    }

    public List<BeadAlleleRef> getBeadAlleleRefList() {
        return beadAlleleRefList;
    }

    public void setBeadAlleleRefList(List<BeadAlleleRef> beadAlleleRefList) {
        this.beadAlleleRefList = beadAlleleRefList;
    }

    public Boolean getKnownReactiveEpitopeForCompat() {
        return knownReactiveEpitopeForCompat;
    }

    public void setKnownReactiveEpitopeForCompat(Boolean knownReactiveEpitopeForCompat) {
        this.knownReactiveEpitopeForCompat = knownReactiveEpitopeForCompat;
    }

    public Boolean getCompatIsRecipientEpitope() {
        return compatIsRecipientEpitope;
    }

    public void setCompatIsRecipientEpitope(Boolean compatIsRecipientEpitope) {
        this.compatIsRecipientEpitope = compatIsRecipientEpitope;
    }

    public Integer getCompatPositiveSabCount() {
        return compatPositiveSabCount;
    }

    public void setCompatPositiveSabCount(Integer compatPositiveSabCount) {
        this.compatPositiveSabCount = compatPositiveSabCount;
    }

    public Integer getCompatPositiveSabPct() {
        return compatPositiveSabPct;
    }

    public void setCompatPositiveSabPct(Integer compatPositiveSabPct) {
        this.compatPositiveSabPct = compatPositiveSabPct;
    }

    public Boolean getCompatAntibodyConsideredPresent() {
        return compatAntibodyConsideredPresent;
    }

    public void setCompatAntibodyConsideredPresent(Boolean compatAntibodyConsideredPresent) {
        this.compatAntibodyConsideredPresent = compatAntibodyConsideredPresent;
    }
   
}
